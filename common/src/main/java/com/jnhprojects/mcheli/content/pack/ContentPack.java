package com.jnhprojects.mcheli.content.pack;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * A content pack: vehicle and weapon definitions with their models, textures and sounds, in a zip
 * file or a folder, described by a {@link PackManifest}.
 * <pre>
 * rework-example.zip
 *   pack.properties
 *   assets/mcheli/helicopters/rework-scout.txt
 *   assets/mcheli/models/helicopters/rework-scout.obj
 *   assets/mcheli/textures/helicopters/rework-scout.png
 *   assets/mcheli/sounds/rework-scout.ogg
 * </pre>
 * A zip whose files all sit in one folder (what zipping a folder usually gives) works too.
 *
 * <p>A pack is data only. Opening one checks that it holds nothing but definitions, models,
 * textures and sounds under {@code assets/mcheli}, plus a readme or licence at the top, so a pack
 * can never bring code. It also stays within {@link #MAX_FILES} files and
 * {@link #MAX_UNPACKED_BYTES} unpacked, counted while reading rather than trusted from the zip,
 * so a crafted zip cannot exhaust memory or disk.
 *
 * <p>The {@linkplain #hash() content hash} covers the path and bytes of every file rather than the
 * zip, so the same files hash the same whether they are zipped, how they are compressed, or kept
 * in a folder. Servers and clients compare it to decide whether a pack has to be sent.
 *
 * <p>A zip is read in place through a zip file system, which stays open until {@link #close}.
 */
public final class ContentPack implements Closeable {
    public static final int MAX_FILES = 10_000;
    public static final long MAX_UNPACKED_BYTES = 512L << 20;

    private static final String ASSETS = "assets/mcheli/";
    private static final Set<String> ASSET_TYPES = Set.of("txt", "mqo", "obj", "mtl", "png", "ogg");
    private static final Set<String> TOP_LEVEL_TYPES = Set.of("", "txt", "md");

    private final Path source;
    private final @Nullable FileSystem zip;
    private final Path root;
    private final PackManifest manifest;
    private final List<String> files;
    private final String hash;
    private byte @Nullable [] folderArchive;

    private ContentPack(Path source, @Nullable FileSystem zip, Path root, PackManifest manifest, List<String> files, String hash) {
        this.source = source;
        this.zip = zip;
        this.root = root;
        this.manifest = manifest;
        this.files = files;
        this.hash = hash;
    }

    /**
     * Opens and checks the pack in a zip file or folder.
     *
     * @throws PackException when it is not a usable pack; the message is meant for its author
     */
    public static ContentPack open(Path source) throws IOException, PackException {
        if (Files.isDirectory(source)) {
            return read(source, null, packRoot(source));
        }
        FileSystem zip;
        try {
            zip = FileSystems.newFileSystem(source);
        } catch (ZipException | ProviderNotFoundException e) {
            throw new PackException("not a zip file");
        }
        try {
            return read(source, zip, packRoot(zip.getRootDirectories().iterator().next()));
        } catch (IOException | PackException | RuntimeException e) {
            zip.close();
            throw e;
        }
    }

    /** The folder holding {@code pack.properties}: the top, or the one folder at the top. */
    private static Path packRoot(Path top) throws IOException {
        if (Files.exists(top.resolve(PackManifest.FILE))) {
            return top;
        }
        try (Stream<Path> children = Files.list(top)) {
            List<Path> list = children.filter(p -> !ignored(p.getFileName().toString())).toList();
            if (list.size() == 1 && Files.isRegularFile(list.getFirst().resolve(PackManifest.FILE))) {
                return list.getFirst();
            }
        }
        return top;
    }

    private static ContentPack read(Path source, @Nullable FileSystem zip, Path root) throws IOException, PackException {
        List<String> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                String path = relative(root, file);
                if (isIgnored(path)) {
                    continue;
                }
                check(path);
                files.add(path);
                if (files.size() > MAX_FILES) {
                    throw new PackException("more than %d files".formatted(MAX_FILES));
                }
            }
        }
        Collections.sort(files);
        if (!files.contains(PackManifest.FILE)) {
            throw new PackException("no " + PackManifest.FILE + " at the top of the pack");
        }
        PackManifest manifest;
        try (InputStream in = Files.newInputStream(root.resolve(PackManifest.FILE))) {
            manifest = PackManifest.read(in);
        }
        return new ContentPack(source, zip, root, manifest, List.copyOf(files), hash(root, files));
    }

    private static void check(String path) throws PackException {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        String type = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (path.equals(name)) {
            if (!path.equals(PackManifest.FILE) && !TOP_LEVEL_TYPES.contains(type)) {
                throw new PackException(path + ": only " + PackManifest.FILE + ", a readme and a licence may sit at the top of a pack");
            }
            return;
        }
        if (!path.startsWith(ASSETS) || !ASSET_TYPES.contains(type)) {
            throw new PackException(path + ": a pack may only hold definitions (.txt), models (.obj, .mqo), textures (.png)"
                    + " and sounds (.ogg) under " + ASSETS);
        }
        if (!path.equals(path.toLowerCase(Locale.ROOT))) {
            throw new PackException(path + ": file names under " + ASSETS + " must be lower case");
        }
    }

    /** Files that operating systems and archivers leave behind, which belong to no pack. */
    private static boolean isIgnored(String path) {
        for (String part : path.split("/")) {
            if (ignored(part)) {
                return true;
            }
        }
        return false;
    }

    private static boolean ignored(String name) {
        return name.startsWith(".") || name.equals("__MACOSX") || name.equalsIgnoreCase("thumbs.db")
                || name.equalsIgnoreCase("desktop.ini");
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
    }

    /**
     * SHA-256 over each file's path and its own SHA-256, in path order. Hashing each file on its
     * own keeps the encoding unambiguous without trusting the sizes a zip claims.
     */
    private static String hash(Path root, List<String> files) throws IOException, PackException {
        MessageDigest pack = sha256();
        MessageDigest file = sha256();
        byte[] buffer = new byte[8192];
        long total = 0;
        for (String path : files) {
            try (InputStream in = Files.newInputStream(root.resolve(path))) {
                int n;
                while ((n = in.read(buffer)) > 0) {
                    total += n;
                    if (total > MAX_UNPACKED_BYTES) {
                        throw new PackException("more than %d MB unpacked".formatted(MAX_UNPACKED_BYTES >> 20));
                    }
                    file.update(buffer, 0, n);
                }
            }
            pack.update(path.getBytes(StandardCharsets.UTF_8));
            pack.update((byte) 0);
            pack.update(file.digest());
        }
        return HexFormat.of().formatHex(pack.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime has SHA-256", e);
        }
    }

    public String id() {
        return manifest.id();
    }

    public PackManifest manifest() {
        return manifest;
    }

    /** Hex SHA-256 of the pack's files; see the class comment. */
    public String hash() {
        return hash;
    }

    /** The zip file or folder the pack was opened from. */
    public Path source() {
        return source;
    }

    /** The pack's {@code assets/mcheli} folder, for the definition loader and the runtime assets. */
    public Path assets() {
        return root.resolve(ASSETS);
    }

    public ContentSource contentSource() {
        return new ContentSource(id(), assets());
    }

    /** Paths of the pack's files relative to its top, in order. */
    public List<String> files() {
        return files;
    }

    /**
     * The pack as zip bytes, ready to send: the zip file as it is on disk, or for a folder a zip
     * built from it once and kept. A zip is read again on every call and may have changed on disk
     * since the pack was opened; check the length against {@link #archiveSize()}.
     */
    public synchronized byte[] archive() throws IOException {
        if (zip != null) {
            return Files.readAllBytes(source);
        }
        if (folderArchive == null) {
            folderArchive = zipFolder();
        }
        return folderArchive;
    }

    /** The length {@link #archive()} returns now, without reading a zip. */
    public long archiveSize() throws IOException {
        return zip != null ? Files.size(source) : archive().length;
    }

    private byte[] zipFolder() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (String path : files) {
                ZipEntry entry = new ZipEntry(path);
                entry.setTime(0L);
                out.putNextEntry(entry);
                Files.copy(root.resolve(path), out);
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    @Override
    public void close() throws IOException {
        if (zip != null) {
            zip.close();
        }
    }

    @Override
    public String toString() {
        return id() + " " + manifest.version() + " (" + source.getFileName() + ")";
    }

    /** The hash as a short string for logs and chat. */
    public static String shortHash(String hash) {
        return hash.length() > 12 ? hash.substring(0, 12) : hash;
    }
}
