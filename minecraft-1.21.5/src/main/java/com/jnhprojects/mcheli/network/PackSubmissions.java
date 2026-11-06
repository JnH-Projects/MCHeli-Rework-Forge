package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.ContentReload;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ServerConfig;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.content.pack.PackException;
import com.jnhprojects.mcheli.content.pack.PendingPacks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.Util;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Players offering a server the vehicles they built, for its operators to approve.
 * <pre>
 *  Submit in the workshop   ── PackSubmitPacket(chunk) ──▶  until the whole zip is here
 *  checked off the main thread, kept in mcheli/pending, operators told
 *  /mcheli approve &lt;pack&gt;    moves it to mcheli/packs and reloads, so every player gets it
 * </pre>
 * Nothing a client sends is trusted. Submissions are off until the server's owner turns them on, one
 * zip can be no larger than the server allows for a pack, a player sends one at a time with a pause
 * in between, chunks out of order end the transfer, and a few transfers run at once at most. The
 * bytes stay in memory only while they arrive; {@link PendingPacks} checks them as it would any pack.
 *
 * <p>Everything here runs on the server thread except the check, which reads the whole pack.
 */
public final class PackSubmissions {
    private static final int MAX_UPLOADS = 4;
    private static final long COOLDOWN_MILLIS = 30_000L;

    private static final Map<UUID, ByteArrayOutputStream> UPLOADS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SUBMISSION = new HashMap<>();

    private PackSubmissions() {
    }

    static void receive(ServerPlayer player, PackSubmitPacket packet) {
        UUID id = player.getUUID();
        if (packet.offset() == 0) {
            UPLOADS.remove(id);
            Component refusal = refusal(player, packet.total());
            if (refusal != null) {
                player.sendSystemMessage(refusal);
                return;
            }
            UPLOADS.put(id, new ByteArrayOutputStream());
        }
        ByteArrayOutputStream upload = UPLOADS.get(id);
        // No upload means it was refused above and the rest of its chunks are still on their way.
        if (upload == null) {
            return;
        }
        long end = (long) upload.size() + packet.data().length;
        if (packet.offset() != upload.size() || packet.total() > ServerConfig.maxPackBytes() || end > packet.total()) {
            UPLOADS.remove(id);
            player.sendSystemMessage(Component.translatable("message.mcheli.submit.failed", "chunks arrived out of order"));
            return;
        }
        upload.write(packet.data(), 0, packet.data().length);
        if (end == packet.total()) {
            UPLOADS.remove(id);
            LAST_SUBMISSION.put(id, System.currentTimeMillis());
            check(player, upload.toByteArray());
        }
    }

    private static @Nullable Component refusal(ServerPlayer player, int total) {
        if (!ServerConfig.allowSubmissions()) {
            return Component.translatable("message.mcheli.submit.off");
        }
        if (total <= 0 || total > ServerConfig.maxPackBytes()) {
            return Component.translatable("message.mcheli.submit.too_large", ServerConfig.maxPackBytes() >> 20);
        }
        if (UPLOADS.size() >= MAX_UPLOADS) {
            return Component.translatable("message.mcheli.submit.busy");
        }
        long now = System.currentTimeMillis();
        LAST_SUBMISSION.values().removeIf(time -> now - time >= COOLDOWN_MILLIS);
        return LAST_SUBMISSION.containsKey(player.getUUID()) ? Component.translatable("message.mcheli.submit.busy") : null;
    }

    private static void check(ServerPlayer player, byte[] zip) {
        MinecraftServer server = player.level().getServer();
        UUID id = player.getUUID();
        String name = player.getScoreboardName();
        CompletableFuture.supplyAsync(() -> {
            try {
                return MCHeli.packs().pending().store(zip, name, id);
            } catch (IOException | PackException | RuntimeException e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, Util.nonCriticalIoPool()).whenComplete((submission, failure) -> server.execute(() -> {
            if (failure != null) {
                Throwable cause = failure.getCause() != null ? failure.getCause() : failure;
                MCHeli.LOGGER.info("{} sent a pack that was refused: {}", name, cause.toString());
                tell(server, id, Component.translatable("message.mcheli.submit.failed", cause.getMessage() != null ? cause.getMessage() : cause.toString()));
            } else {
                arrived(server, submission);
            }
        }));
    }

    private static void arrived(MinecraftServer server, PendingPacks.Submission submission) {
        MCHeli.LOGGER.info("{} submitted content pack {} {} ({})", submission.submitter(), submission.id(),
                submission.manifest().version(), ContentPack.shortHash(submission.hash()));
        // Even with approval off, a pack taking over one that is in use waits for a person.
        boolean replaces = MCHeli.packs().active().stream().anyMatch(pack -> pack.id().equals(submission.id()));
        if (!ServerConfig.requireApproval() && !replaces) {
            try {
                install(server, submission);
            } catch (IOException | PackException e) {
                MCHeli.LOGGER.error("Could not install submitted content pack {}", submission.id(), e);
                tell(server, submission.submitterId(), Component.translatable("message.mcheli.submit.failed", e.getMessage()));
            }
            return;
        }
        tell(server, submission.submitterId(), Component.translatable("message.mcheli.submit.waiting", submission.id()));
        Component note = Component.translatable("message.mcheli.submit.to_operators", submission.submitter(), submission.id(), submission.manifest().version());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (PackSync.seesProblems(player)) {
                player.sendSystemMessage(note);
            }
        }
    }

    /**
     * Puts a waiting pack in use: into {@code mcheli/packs}, then a reload that sends every player
     * the new list.
     *
     * @return whether the pack is in use now; the server's allow list or size limits may leave it out
     */
    public static boolean install(MinecraftServer server, PendingPacks.Submission submission) throws IOException, PackException {
        MCHeli.packs().pending().install(submission, MCHeli.packs().folder());
        ContentReload.reload(server);
        MCHeli.LOGGER.info("Content pack {} from {} approved", submission.id(), submission.submitter());
        boolean live = MCHeli.packs().active().stream().anyMatch(pack -> pack.id().equals(submission.id()));
        if (live) {
            tell(server, submission.submitterId(), Component.translatable("message.mcheli.submit.approved", submission.id()));
        }
        return live;
    }

    /** Throws a waiting pack away and tells the player who sent it, if they are here. */
    public static void reject(MinecraftServer server, PendingPacks.Submission submission) throws IOException {
        MCHeli.packs().pending().remove(submission);
        MCHeli.LOGGER.info("Content pack {} from {} rejected", submission.id(), submission.submitter());
        tell(server, submission.submitterId(), Component.translatable("message.mcheli.submit.rejected", submission.id()));
    }

    private static void tell(MinecraftServer server, UUID player, Component message) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            online.sendSystemMessage(message);
        }
    }

    /** An operator joining hears what is waiting for them. */
    static void onLogin(ServerPlayer player) {
        if (!PackSync.seesProblems(player)) {
            return;
        }
        try {
            List<PendingPacks.Submission> waiting = MCHeli.packs().pending().list();
            if (!waiting.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.mcheli.submit.waiting_for_you", waiting.size()));
            }
        } catch (IOException e) {
            MCHeli.LOGGER.warn("Could not list the submitted content packs", e);
        }
    }

    static void onLogout(ServerPlayer player) {
        UPLOADS.remove(player.getUUID());
    }
}
