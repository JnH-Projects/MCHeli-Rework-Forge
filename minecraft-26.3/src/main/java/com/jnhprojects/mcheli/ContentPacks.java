package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.content.pack.ContentSource;
import com.jnhprojects.mcheli.content.pack.PackFolder;
import com.jnhprojects.mcheli.content.pack.PackProblem;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The content packs in {@code <game dir>/mcheli/packs}, loaded together with the imported 1.0.3
 * content, which always comes first.
 *
 * <p>At startup every pack in the folder is loaded, so the vehicles exist before any world does.
 * When a server starts (single player included) and on {@code /mcheli reload}, the folder is
 * scanned again and the server config applied: the allow list, the size limit per pack and the
 * limit for all packs together. The packs that pass are what players get sent.
 *
 * <p>A zip pack is read in place, so packs stay open while their definitions and assets are in
 * use. The set a reload replaces is closed on the reload after that, so a client in the same game
 * that is still drawing from it never reads from a closed zip.
 */
public final class ContentPacks {
    private static final int PROBLEMS_IN_CHAT = 8;

    private final Path folder;
    private final LegacyContent legacy;
    private volatile List<ContentPack> active = List.of();
    private volatile List<PackProblem> problems = List.of();
    private List<ContentPack> retired = List.of();

    ContentPacks(Path gameDir, LegacyContent legacy) {
        this.folder = gameDir.resolve(MCHeli.MOD_ID).resolve("packs");
        this.legacy = legacy;
    }

    /** {@code <game dir>/mcheli/packs}. */
    public Path folder() {
        return folder;
    }

    /** The packs whose definitions are loaded, in load order. The import is not among them. */
    public List<ContentPack> active() {
        return active;
    }

    /** Everything that went wrong in the last load, packs left out included. */
    public List<PackProblem> problems() {
        return problems;
    }

    /**
     * Scans the folder and loads the import and the packs. Never throws; whatever goes wrong is
     * logged and kept in {@link #problems()}.
     *
     * @param serverRules whether to apply the server config; off at startup, when no world exists
     * @return the new content, for the caller to swap in
     */
    public synchronized ContentRegistry load(boolean serverRules) {
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            MCHeli.LOGGER.warn("Could not create {}", folder, e);
        }

        PackFolder.Scan scan = PackFolder.scan(folder);
        List<PackProblem> found = new ArrayList<>(scan.problems());
        List<ContentPack> accepted = new ArrayList<>();
        long total = 0;
        for (ContentPack pack : scan.packs()) {
            long size;
            String refusal;
            try {
                size = pack.archiveSize();
                refusal = serverRules ? refusal(pack, size, total) : null;
            } catch (IOException e) {
                size = 0;
                refusal = "left out, could not be read: " + e;
            }
            if (refusal == null) {
                accepted.add(pack);
                total += size;
            } else {
                found.add(new PackProblem(pack.id(), refusal));
                close(List.of(pack));
            }
        }
        for (PackProblem problem : found) {
            MCHeli.LOGGER.warn("Content pack {}", problem);
        }

        List<ContentSource> sources = new ArrayList<>();
        sources.add(new ContentSource(ContentSource.ORIGINAL, legacy.assets()));
        for (ContentPack pack : accepted) {
            sources.add(pack.contentSource());
        }
        ContentRegistry registry = ContentRegistry.load(sources);
        found.addAll(registry.problems());

        close(retired);
        retired = active;
        active = List.copyOf(accepted);
        problems = List.copyOf(found);
        MCHeli.LOGGER.info("Content packs in use: {}", active.isEmpty() ? "none" : active);
        return registry;
    }

    private static @Nullable String refusal(ContentPack pack, long size, long total) {
        if (!ServerConfig.isPackAllowed(pack.id())) {
            return "left out, not on the server's allow list (packs.allowList in mcheli-server.toml)";
        }
        if (size > ServerConfig.maxPackBytes()) {
            return "left out, %.1f MB is more than the server allows for one pack (packs.maxPackSize, %d MB)"
                    .formatted(size / 1048576.0, ServerConfig.maxPackBytes() >> 20);
        }
        if (total + size > ServerConfig.maxTotalPackBytes()) {
            return "left out, it would take the packs past the server's total limit (packs.maxTotalSize, %d MB)"
                    .formatted(ServerConfig.maxTotalPackBytes() >> 20);
        }
        return null;
    }

    /** Sends the problems of the last load as chat lines, the first few of them. */
    public void reportProblems(Consumer<Component> out) {
        List<PackProblem> list = problems;
        for (int i = 0; i < Math.min(list.size(), PROBLEMS_IN_CHAT); i++) {
            out.accept(Component.translatable("message.mcheli.pack.problem", list.get(i).pack(), list.get(i).message()));
        }
        if (list.size() > PROBLEMS_IN_CHAT) {
            out.accept(Component.translatable("message.mcheli.pack.more_problems", list.size() - PROBLEMS_IN_CHAT));
        }
    }

    private static void close(List<ContentPack> packs) {
        for (ContentPack pack : packs) {
            try {
                pack.close();
            } catch (IOException e) {
                MCHeli.LOGGER.warn("Could not close content pack {}", pack, e);
            }
        }
    }
}
