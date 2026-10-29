package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.ClientConfig;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.content.pack.ContentSource;
import com.jnhprojects.mcheli.content.pack.PackException;
import com.jnhprojects.mcheli.entity.ClientAccess;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.network.PackDataPacket;
import com.jnhprojects.mcheli.network.PackListPacket;
import com.jnhprojects.mcheli.network.PackRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The client half of the content pack sync (the server half is {@code PackSync}).
 *
 * <p>When the server sends its pack list, every pack this client has neither in its own
 * {@code mcheli/packs} nor in {@code mcheli/cache} is requested. Each arrives in chunks, is checked
 * against its hash and kept as {@code mcheli/cache/<hash>.zip}. Once all are there, the content is
 * rebuilt from the client's own import plus exactly the server's packs, in the server's order, so
 * the vehicle a player flies (which their client simulates) has the server's definition.
 *
 * <p>In single player, and for the host of a LAN game, client and server share one registry, so
 * only the client's view of the server's packs is refreshed.
 *
 * <p>The cache is capped by {@code packs.cacheSize} in the client config: after a download, the
 * packs used longest ago go first. The current server's packs are always kept.
 */
final class ClientPacks implements ClientAccess.PackReceiver {
    static final ClientPacks INSTANCE = new ClientPacks();

    private static final Pattern CACHED = Pattern.compile("[0-9a-f]{64}\\.zip");

    private final Path cache = FMLPaths.GAMEDIR.get().resolve(MCHeli.MOD_ID).resolve("cache");
    private List<PackListPacket.Entry> wanted = List.of();
    private final Map<String, Download> downloads = new LinkedHashMap<>();
    /** Packs this class opened for the current server; closed when the next list replaces them. */
    private List<ContentPack> mounted = List.of();
    private int downloaded;

    private ClientPacks() {
    }

    @Override
    public void onPackList(PackListPacket packet) {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            useRoots(MCHeli.packs().active());
            ClientContentReload.refresh();
            return;
        }

        wanted = packet.packs();
        downloads.clear();
        downloaded = 0;
        long cap = ClientConfig.packCacheBytes();
        List<String> request = new ArrayList<>();
        long bytes = 0;
        for (PackListPacket.Entry entry : wanted) {
            if (find(entry) != null) {
                continue;
            }
            if (entry.size() > cap || entry.size() <= 0) {
                say(Component.translatable("message.mcheli.pack.too_large", entry.id(), mb(entry.size()), cap >> 20));
                continue;
            }
            downloads.put(entry.hash(), new Download(entry));
            request.add(entry.hash());
            bytes += entry.size();
        }

        if (request.isEmpty()) {
            apply();
        } else {
            say(Component.translatable("message.mcheli.pack.downloading", request.size(), mb(bytes)));
            ModNetwork.sendToServer(new PackRequestPacket(request));
        }
    }

    @Override
    public void onPackData(PackDataPacket packet) {
        Download download = downloads.get(packet.hash());
        if (download == null) {
            return;
        }
        if (!download.accept(packet)) {
            fail(download, "the server sent a chunk out of order");
            return;
        }
        if (!download.complete()) {
            return;
        }
        downloads.remove(packet.hash());
        try {
            store(download);
            downloaded++;
        } catch (IOException | PackException e) {
            fail(download, e.getMessage());
            return;
        }
        if (downloads.isEmpty()) {
            apply();
        }
    }

    /** Leaving a server: its packs are no longer needed. */
    void loggedOut() {
        downloads.clear();
        wanted = List.of();
        RuntimeAssets.setRoots(List.of());
        close(mounted);
        mounted = List.of();
    }

    private void fail(Download download, String reason) {
        downloads.remove(download.entry.hash());
        MCHeli.LOGGER.error("Content pack {} could not be downloaded: {}", download.entry.id(), reason);
        say(Component.translatable("message.mcheli.pack.failed", download.entry.id(), reason));
        if (downloads.isEmpty()) {
            apply();
        }
    }

    /** Writes a finished download to the cache once its content matches the hash it was listed with. */
    private void store(Download download) throws IOException, PackException {
        String hash = download.entry.hash();
        Files.createDirectories(cache);
        Path part = cache.resolve(hash + ".download.zip");
        Files.write(part, download.data);
        try (ContentPack pack = ContentPack.open(part)) {
            if (!pack.hash().equals(hash)) {
                throw new PackException("its files do not match the hash the server listed");
            }
        } catch (IOException | PackException e) {
            Files.deleteIfExists(part);
            throw e;
        }
        Files.move(part, cache.resolve(hash + ".zip"), StandardCopyOption.REPLACE_EXISTING);
    }

    /** Rebuilds the content from the import and the server's packs that are now on disk. */
    private void apply() {
        List<ContentPack> opened = new ArrayList<>();
        List<ContentSource> sources = new ArrayList<>();
        sources.add(new ContentSource(ContentSource.ORIGINAL, MCHeli.legacyContent().assets()));
        for (PackListPacket.Entry entry : wanted) {
            Path file = find(entry);
            if (file == null) {
                continue;
            }
            try {
                ContentPack pack = ContentPack.open(file);
                if (!pack.hash().equals(entry.hash())) {
                    pack.close();
                    MCHeli.LOGGER.warn("Content pack {} changed on disk and no longer matches the server's", file);
                    continue;
                }
                opened.add(pack);
                sources.add(pack.contentSource());
                if (file.startsWith(cache)) {
                    Files.setLastModifiedTime(file, FileTime.from(Instant.now()));
                }
            } catch (IOException | PackException e) {
                MCHeli.LOGGER.error("Could not open content pack {}", file, e);
            }
        }

        ContentRegistry registry = ContentRegistry.load(sources);
        MCHeli.content().replaceWith(registry);
        List<ContentPack> old = mounted;
        mounted = List.copyOf(opened);
        useRoots(mounted);
        close(old);
        ClientContentReload.refresh();
        trimCache();
        if (downloaded > 0) {
            say(Component.translatable("message.mcheli.pack.ready", downloaded));
            downloaded = 0;
        }
    }

    /** The pack on disk with the entry's content: one of the player's own packs, or a cached download. */
    private @Nullable Path find(PackListPacket.Entry entry) {
        for (ContentPack own : MCHeli.packs().active()) {
            if (own.hash().equals(entry.hash())) {
                return own.source();
            }
        }
        Path cached = cache.resolve(entry.hash() + ".zip");
        return Files.isRegularFile(cached) ? cached : null;
    }

    /** Deletes the cached packs used longest ago until the cache fits its cap. */
    private void trimCache() {
        Set<String> keep = new HashSet<>();
        for (PackListPacket.Entry entry : wanted) {
            keep.add(entry.hash() + ".zip");
        }
        List<Path> files;
        try (Stream<Path> list = Files.list(cache)) {
            files = list.filter(p -> CACHED.matcher(p.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(ClientPacks::lastUsed))
                    .toList();
        } catch (IOException e) {
            return;
        }
        long total = 0;
        for (Path file : files) {
            total += size(file);
        }
        long cap = ClientConfig.packCacheBytes();
        for (Path file : files) {
            if (total <= cap) {
                break;
            }
            if (keep.contains(file.getFileName().toString())) {
                continue;
            }
            try {
                long size = size(file);
                Files.delete(file);
                total -= size;
                MCHeli.LOGGER.info("Removed {} from the content pack cache", file.getFileName());
            } catch (IOException e) {
                MCHeli.LOGGER.warn("Could not remove {} from the content pack cache", file, e);
            }
        }
    }

    private static FileTime lastUsed(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    private static long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    /** The pack loaded last overrides the others, so it is searched first. */
    private static void useRoots(List<ContentPack> packs) {
        List<Path> roots = new ArrayList<>();
        for (ContentPack pack : packs) {
            roots.addFirst(pack.assets());
        }
        RuntimeAssets.setRoots(roots);
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

    private static void say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(message);
        } else {
            MCHeli.LOGGER.info(message.getString());
        }
    }

    private static String mb(long bytes) {
        return "%.1f".formatted(bytes / 1048576.0);
    }

    /** One pack arriving from the server. */
    private static final class Download {
        private final PackListPacket.Entry entry;
        private byte @Nullable [] data;
        private int received;

        Download(PackListPacket.Entry entry) {
            this.entry = entry;
        }

        boolean accept(PackDataPacket packet) {
            if (packet.total() != entry.size() || packet.offset() != received
                    || received + packet.data().length > entry.size()) {
                return false;
            }
            if (data == null) {
                data = new byte[entry.size()];
            }
            System.arraycopy(packet.data(), 0, data, received, packet.data().length);
            received += packet.data().length;
            return true;
        }

        boolean complete() {
            return data != null && received == data.length;
        }
    }
}
