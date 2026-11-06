package com.jnhprojects.mcheli.content.pack;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Packs players have sent a server, waiting for an operator in {@code <game dir>/mcheli/pending}.
 *
 * <p>This folder takes files from any player, so nothing about a submission is trusted. Files are
 * named by content hash alone, never by anything the player chose. A pack is opened through the same
 * checks as any other before it is kept, which bounds its file count and unpacked size and refuses
 * anything but definitions, models, textures and sounds. A player has one submission at a time, a
 * new one replacing the last, and {@value #MAX_PENDING} wait at most, so the server's size limit for
 * a pack also limits what this folder can hold.
 *
 * <p>Installing moves the zip as it is, so what goes live is the same bytes the operator reviewed.
 */
public final class PendingPacks {
    public static final int MAX_PENDING = 16;

    private static final Pattern FILE = Pattern.compile("[0-9a-f]{64}\\.zip");
    private static final String REPLACED = ".replaced";

    private final Path folder;
    private final Map<Path, Submission> read = new HashMap<>();

    public PendingPacks(Path folder) {
        this.folder = folder;
    }

    /**
     * A pack waiting for an operator.
     *
     * @param submitter   the name of the player who sent it, which unlike the manifest's author the
     *                    server knows to be true
     * @param submitterId that player's id
     * @param definitions the definition files it holds, for the operator to look over
     */
    public record Submission(PackManifest manifest, String hash, long size, String submitter, UUID submitterId,
                             List<String> definitions, Path file) {
        public String id() {
            return manifest.id();
        }
    }

    /** What is waiting, oldest first. A file that cannot be read is left out, and stays on disk. */
    public synchronized List<Submission> list() throws IOException {
        List<Path> files;
        try (Stream<Path> list = Files.list(folder)) {
            files = list.filter(p -> FILE.matcher(p.getFileName().toString()).matches())
                    .sorted(Comparator.comparingLong(PendingPacks::modified).thenComparing(Comparator.naturalOrder()))
                    .toList();
        } catch (NoSuchFileException e) {
            return List.of();
        }
        List<Submission> found = new ArrayList<>();
        for (Path file : files) {
            try {
                found.add(read(file));
            } catch (IOException | PackException | IllegalArgumentException e) {
                // Damaged by hand or by a full disk. The operator can look at it in the folder.
            }
        }
        return List.copyOf(found);
    }

    private static long modified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /** A file named by its hash never changes, and opening one hashes it, so each is only read once. */
    private Submission read(Path file) throws IOException, PackException {
        Submission known = read.get(file);
        if (known != null) {
            return known;
        }
        Properties about = new Properties();
        try (InputStream in = Files.newInputStream(about(file))) {
            about.load(in);
        }
        try (ContentPack pack = ContentPack.open(file)) {
            Submission submission = new Submission(pack.manifest(), pack.hash(), Files.size(file), about.getProperty("submitter", "?"),
                    UUID.fromString(about.getProperty("submitterId", "")), definitions(pack), file);
            read.put(file, submission);
            return submission;
        }
    }

    private static List<String> definitions(ContentPack pack) {
        String assets = "assets/mcheli/";
        List<String> definitions = new ArrayList<>();
        for (String path : pack.files()) {
            if (path.startsWith(assets) && path.endsWith(".txt")) {
                definitions.add(path.substring(assets.length()));
            }
        }
        return List.copyOf(definitions);
    }

    private static Path about(Path file) {
        String name = file.getFileName().toString();
        return file.resolveSibling(name.substring(0, name.length() - ".zip".length()) + ".properties");
    }

    /** The submission with this pack id, or {@code null}. Ids are unique while waiting. */
    public @Nullable Submission find(String id) throws IOException {
        for (Submission submission : list()) {
            if (submission.id().equals(id)) {
                return submission;
            }
        }
        return null;
    }

    /**
     * Checks and keeps what a player sent, replacing that player's last submission.
     *
     * @throws PackException when it is not a usable pack or there is no room; the message is meant
     *                       for the player
     */
    public synchronized Submission store(byte[] zip, String player, UUID playerId) throws IOException, PackException {
        Files.createDirectories(folder);
        Path part = folder.resolve(playerId + ".part");
        Files.write(part, zip);
        try {
            PackManifest manifest;
            String hash;
            try (ContentPack pack = ContentPack.open(part)) {
                manifest = pack.manifest();
                hash = pack.hash();
            }
            List<Submission> waiting = list();
            Submission previous = null;
            for (Submission other : waiting) {
                if (other.submitterId().equals(playerId)) {
                    previous = other;
                } else if (other.id().equals(manifest.id())) {
                    throw new PackException("%s already has a pack called %s waiting for an operator. Pick another pack id."
                            .formatted(other.submitter(), manifest.id()));
                }
            }
            if (previous == null && waiting.size() >= MAX_PENDING) {
                throw new PackException("%d packs are already waiting for an operator. Try again later.".formatted(MAX_PENDING));
            }
            if (previous != null) {
                remove(previous);
            }
            Path file = folder.resolve(hash + ".zip");
            Properties about = new Properties();
            about.setProperty("submitter", player);
            about.setProperty("submitterId", playerId.toString());
            try (OutputStream out = Files.newOutputStream(about(file))) {
                about.store(out, null);
            }
            Files.move(part, file, StandardCopyOption.REPLACE_EXISTING);
            return read(file);
        } finally {
            Files.deleteIfExists(part);
        }
    }

    /** Drops a submission. */
    public synchronized void remove(Submission submission) throws IOException {
        read.remove(submission.file());
        Files.deleteIfExists(submission.file());
        Files.deleteIfExists(about(submission.file()));
    }

    /**
     * Moves a submission into {@code mcheli/packs}, taking the place of the zip of a pack with the
     * same id. A zip in use stays open and on Windows cannot be overwritten, but it can be renamed:
     * the old file moves aside and goes away in {@link #cleanReplaced} once nothing has it open.
     *
     * @return the installed zip
     * @throws PackException when the pack with that id is a folder, which is someone's working copy
     *                       and has to be removed by hand
     */
    public synchronized Path install(Submission submission, Path packs) throws IOException, PackException {
        Files.createDirectories(packs);
        Path existing = null;
        PackFolder.Scan scan = PackFolder.scan(packs);
        for (ContentPack pack : scan.packs()) {
            if (pack.id().equals(submission.id())) {
                existing = pack.source();
            }
            pack.close();
        }
        if (existing != null && Files.isDirectory(existing)) {
            throw new PackException("%s is a folder in mcheli/packs, so it may hold someone's work. Remove it by hand first."
                    .formatted(existing.getFileName()));
        }
        String shortHash = ContentPack.shortHash(submission.hash());
        if (existing != null) {
            Files.move(existing, existing.resolveSibling(existing.getFileName() + REPLACED + "-" + shortHash),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Path target = packs.resolve(submission.id() + ".zip");
        if (Files.exists(target)) {
            // Some other pack's file happens to have this name; the id inside is what counts.
            target = packs.resolve(submission.id() + "-" + shortHash + ".zip");
        }
        Files.move(submission.file(), target);
        read.remove(submission.file());
        Files.deleteIfExists(about(submission.file()));
        return target;
    }

    /** Deletes the zips {@link #install} moved aside. One still open stays until a later try. */
    public static void cleanReplaced(Path packs) {
        try (Stream<Path> list = Files.list(packs)) {
            for (Path file : (Iterable<Path>) list::iterator) {
                if (file.getFileName().toString().toLowerCase(Locale.ROOT).contains(".zip" + REPLACED)) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        // Still in use. The pack scan ignores it, so it can wait.
                    }
                }
            }
        } catch (IOException e) {
            // No folder, so nothing to clean.
        }
    }
}
