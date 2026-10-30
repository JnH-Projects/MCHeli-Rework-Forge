package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.content.ImportTarget;
import com.jnhprojects.mcheli.content.LegacyImporter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * The original vehicles, models, textures and sounds, imported from the player's own copy of
 * MCHeli 1.0.3.
 *
 * <p>That content is EMB4's and is not redistributed with this mod. Players download the original
 * zip from the MCHeli CurseForge page and put it in {@code <game dir>/mcheli}. On start the zip is
 * converted once by {@link LegacyImporter} into {@code mcheli/imported}, which then serves as a
 * required resource pack and data pack. The zip is converted again only when it changes or the
 * importer's output format does; once imported it may be deleted.
 *
 * <p>Development runs point the {@code mcheli.legacyZip} system property at the zip instead.
 */
public final class LegacyContent {
    /** The file name the original download has. Any zip with the same layout works. */
    public static final String ZIP_NAME = "MC1.7.10_mcheli_1.0.3.zip";

    private static final String PACK_ID = "mcheli_content";
    private static final String MARKER = "import.properties";
    /** The pack layout this Minecraft version reads. */
    private static final ImportTarget TARGET = ImportTarget.MODERN;

    private final Path folder;
    private final Path packRoot;
    private @Nullable Component problem;

    private LegacyContent(Path folder) {
        this.folder = folder;
        this.packRoot = folder.resolve("imported");
    }

    /** Imports the zip if needed. Never throws; a missing or broken zip leaves the mod without content. */
    static LegacyContent prepare(Path gameDir) {
        LegacyContent content = new LegacyContent(gameDir.resolve(MCHeli.MOD_ID));
        try {
            content.importIfNeeded();
        } catch (IOException | UncheckedIOException e) {
            MCHeli.LOGGER.error("Could not import the original MCHeli content", e);
            content.problem = Component.translatable("message.mcheli.content.import_failed", e.getMessage());
        }
        return content;
    }

    /** The imported {@code assets/mcheli} folder. It does not exist when nothing was imported. */
    public Path assets() {
        return packRoot.resolve("assets").resolve(MCHeli.MOD_ID);
    }

    /** Why the vehicles are missing, for the player; empty when the content is in place. */
    public Optional<Component> problem() {
        return Optional.ofNullable(problem);
    }

    /**
     * The imported folder as a pack, for both the client resource packs and the server data packs.
     * Empty when nothing was imported.
     */
    public Optional<Pack> pack() {
        if (!Files.isDirectory(packRoot)) {
            return Optional.empty();
        }
        PackLocationInfo location = new PackLocationInfo(PACK_ID,
                Component.translatable("pack.mcheli.content"), PackSource.BUILT_IN, Optional.empty());
        Pack.Metadata metadata = new Pack.Metadata(Component.translatable("pack.mcheli.content.description"),
                PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
        // Required, so it is enabled in every world and cannot be switched off. It sits at the
        // bottom so that regular resource packs can still replace its textures and models.
        return Optional.of(new Pack(location, new PathPackResources.PathResourcesSupplier(packRoot), metadata,
                new PackSelectionConfig(true, Pack.Position.BOTTOM, false)));
    }

    private void importIfNeeded() throws IOException {
        Path zip = findZip();
        if (zip == null) {
            if (Files.isDirectory(packRoot)) {
                MCHeli.LOGGER.info("Using the MCHeli content imported earlier into {}", packRoot);
            } else {
                MCHeli.LOGGER.error("MCHeli content not found. Download {} from the MCHeli CurseForge page and put it in {}",
                        ZIP_NAME, folder.toAbsolutePath());
                problem = Component.translatable("message.mcheli.content.missing", ZIP_NAME, folder.toAbsolutePath().toString());
            }
            return;
        }

        Properties source = describe(zip);
        if (source.equals(readMarker())) {
            return;
        }

        MCHeli.LOGGER.info("Importing MCHeli content from {}", zip);
        Path staging = folder.resolve("imported.tmp");
        deleteTree(staging);
        LegacyImporter.Summary summary = LegacyImporter.run(zip, staging, TARGET);
        try (OutputStream out = Files.newOutputStream(staging.resolve(MARKER))) {
            source.store(out, "Written by MCHeli Rework. Delete this folder to import again.");
        }
        swapIn(staging);

        MCHeli.LOGGER.info("Imported {} files, {} sounds, {} item models and {} recipes into {}",
                summary.files(), summary.sounds(), summary.itemModels(), summary.recipes(), packRoot);
        for (String skipped : summary.skippedRecipes()) {
            MCHeli.LOGGER.info("Recipe left out: {}", skipped);
        }
    }

    /**
     * Replaces the previous import with the fresh one in {@code staging}.
     *
     * <p>On Windows a folder that was just deleted keeps its name for a moment while another
     * program (a file watcher, a virus scanner) still has a handle in it, and renaming onto that
     * name fails with "access denied". So the old import is moved out of the way rather than
     * deleted, the renames are retried for a few seconds, and the old copy is deleted last, on a
     * best effort basis; a copy left behind is removed on the next import.
     */
    private void swapIn(Path staging) throws IOException {
        Path old = folder.resolve("imported.old");
        deleteQuietly(old);
        if (Files.exists(packRoot)) {
            try {
                moveWithRetry(packRoot, old);
            } catch (IOException e) {
                deleteTree(packRoot);
            }
        }
        moveWithRetry(staging, packRoot);
        deleteQuietly(old);
    }

    private static void moveWithRetry(Path from, Path to) throws IOException {
        for (int attempt = 1; ; attempt++) {
            try {
                Files.move(from, to);
                return;
            } catch (FileSystemException e) {
                if (attempt >= 15) {
                    throw e;
                }
                try {
                    Thread.sleep(50L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private static void deleteQuietly(Path root) {
        try {
            deleteTree(root);
        } catch (IOException e) {
            MCHeli.LOGGER.warn("Could not delete {}; it is removed on the next import", root, e);
        }
    }

    private @Nullable Path findZip() throws IOException {
        String configured = System.getProperty("mcheli.legacyZip");
        if (configured != null && !configured.isBlank()) {
            Path zip = Path.of(configured);
            if (Files.isRegularFile(zip)) {
                return zip;
            }
            MCHeli.LOGGER.warn("mcheli.legacyZip points to {}, which does not exist", zip);
        }

        Files.createDirectories(folder);
        if (Files.isRegularFile(folder.resolve(ZIP_NAME))) {
            return folder.resolve(ZIP_NAME);
        }
        // Browsers rename repeated downloads, so take any zip, the same way every start.
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .findFirst()
                    .orElse(null);
        }
    }

    /** What the current import was made from. Comparing size and time is enough to notice a new zip. */
    private static Properties describe(Path zip) throws IOException {
        Properties p = new Properties();
        p.setProperty("format", Integer.toString(LegacyImporter.FORMAT));
        p.setProperty("target", TARGET.name());
        p.setProperty("source", zip.toAbsolutePath().normalize().toString());
        p.setProperty("size", Long.toString(Files.size(zip)));
        p.setProperty("modified", Long.toString(Files.getLastModifiedTime(zip).toMillis()));
        return p;
    }

    private Properties readMarker() throws IOException {
        Properties p = new Properties();
        Path marker = packRoot.resolve(MARKER);
        if (Files.isRegularFile(marker)) {
            try (InputStream in = Files.newInputStream(marker)) {
                p.load(in);
            }
        }
        return p;
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
