package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The server half of the content pack sync.
 * <pre>
 *  player joins          ── PackListPacket(id, version, hash, size per pack) ──▶ client
 *                        ◀── PackRequestPacket(hashes it has not cached) ────────
 *  every tick            ── PackDataPacket(chunk) ──▶  until each pack is sent
 * </pre>
 * Each player gets {@link #CHUNKS_PER_TICK} chunks a tick (about 5 MB a second), one pack after
 * the other, so a download never crowds out the game's own packets. A pack's bytes are read when
 * its turn comes and dropped once sent. Everything here runs on the server thread.
 */
public final class PackSync {
    private static final int CHUNKS_PER_TICK = 2;

    private static final Map<UUID, Deque<Transfer>> TRANSFERS = new HashMap<>();

    private PackSync() {
    }

    /** Sends a joining player the pack list, and operators any pack problems. */
    public static void onLogin(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, list());
        if (seesProblems(player)) {
            MCHeli.packs().reportProblems(player::sendSystemMessage);
        }
    }

    public static void onLogout(ServerPlayer player) {
        TRANSFERS.remove(player.getUUID());
    }

    /** The packs the server uses now, as the client should load them. */
    public static PackListPacket list() {
        List<PackListPacket.Entry> entries = new ArrayList<>();
        for (ContentPack pack : MCHeli.packs().active()) {
            try {
                entries.add(new PackListPacket.Entry(pack.id(), pack.manifest().version(), pack.hash(), (int) pack.archiveSize()));
            } catch (IOException e) {
                MCHeli.LOGGER.error("Content pack {} cannot be sent to players", pack, e);
            }
        }
        return new PackListPacket(List.copyOf(entries));
    }

    /** After a reload: drops transfers in progress and sends every player the new list. */
    public static void sendListToAll(MinecraftServer server) {
        TRANSFERS.clear();
        ModNetwork.sendToAll(server, list());
    }

    /** Operators, and the owner of a single player or LAN world, are told what is wrong with the packs. */
    public static boolean seesProblems(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server.isSingleplayerOwner(player.nameAndId())
                || Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }

    static void request(ServerPlayer player, List<String> hashes) {
        Deque<Transfer> queue = TRANSFERS.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        for (String hash : hashes) {
            ContentPack pack = find(hash);
            if (pack == null) {
                MCHeli.LOGGER.debug("{} asked for content pack {}, which the server does not use", player.getScoreboardName(), hash);
                continue;
            }
            if (queue.stream().noneMatch(t -> t.pack == pack)) {
                queue.add(new Transfer(pack));
            }
        }
    }

    private static @Nullable ContentPack find(String hash) {
        for (ContentPack pack : MCHeli.packs().active()) {
            if (pack.hash().equals(hash)) {
                return pack;
            }
        }
        return null;
    }

    public static void pump(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Deque<Transfer>>> it = TRANSFERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Deque<Transfer>> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Deque<Transfer> queue = entry.getValue();
            if (player == null || queue.isEmpty()) {
                it.remove();
                continue;
            }
            for (int sent = 0; sent < CHUNKS_PER_TICK && !queue.isEmpty(); sent++) {
                Transfer transfer = queue.peek();
                PackDataPacket chunk = transfer.next();
                if (chunk == null) {
                    queue.poll();
                    continue;
                }
                ModNetwork.sendToPlayer(player, chunk);
                if (transfer.done()) {
                    queue.poll();
                }
            }
        }
    }

    /** One pack on its way to one player. */
    private static final class Transfer {
        private final ContentPack pack;
        private byte @Nullable [] data;
        private int offset;

        Transfer(ContentPack pack) {
            this.pack = pack;
        }

        /** The next chunk, or {@code null} when the pack could not be read. */
        @Nullable PackDataPacket next() {
            if (data == null) {
                try {
                    data = pack.archive();
                } catch (IOException e) {
                    MCHeli.LOGGER.error("Could not read content pack {} to send it", pack, e);
                    return null;
                }
            }
            int end = Math.min(offset + PackDataPacket.CHUNK_SIZE, data.length);
            PackDataPacket chunk = new PackDataPacket(pack.hash(), offset, data.length, Arrays.copyOfRange(data, offset, end));
            offset = end;
            return chunk;
        }

        boolean done() {
            return data != null && offset >= data.length;
        }
    }
}
