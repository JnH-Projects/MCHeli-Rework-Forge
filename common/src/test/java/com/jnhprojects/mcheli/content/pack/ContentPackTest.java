package com.jnhprojects.mcheli.content.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opening, checking and hashing packs, in folders and zips built by the tests themselves. */
class ContentPackTest {
    private static final String MANIFEST = "id = scouts\nname = Scouts\nauthor = Test\nversion = 2.1\n";

    @Test
    void aFolderAndItsZipHaveTheSameHash(@TempDir Path dir) throws Exception {
        Map<String, String> files = pack();
        Path folder = folder(dir.resolve("scouts"), files);
        Path zip = zip(dir.resolve("scouts.zip"), "", files);

        try (ContentPack fromFolder = ContentPack.open(folder); ContentPack fromZip = ContentPack.open(zip)) {
            assertEquals("scouts", fromFolder.id());
            assertEquals("2.1", fromZip.manifest().version());
            assertEquals(fromFolder.hash(), fromZip.hash());
            assertEquals(List.copyOf(files.keySet()), fromZip.files());
            assertTrue(Files.isRegularFile(fromZip.assets().resolve("helicopters/scout.txt")));
        }
    }

    @Test
    void aZipWrappingOneFolderIsReadFromThatFolder(@TempDir Path dir) throws Exception {
        Map<String, String> files = pack();
        try (ContentPack flat = ContentPack.open(zip(dir.resolve("flat.zip"), "", files));
             ContentPack wrapped = ContentPack.open(zip(dir.resolve("wrapped.zip"), "scouts-1.0/", files))) {
            assertEquals(flat.hash(), wrapped.hash());
        }
    }

    @Test
    void theHashFollowsTheContent(@TempDir Path dir) throws Exception {
        Map<String, String> files = pack();
        Map<String, String> edited = new TreeMap<>(files);
        edited.put("assets/mcheli/helicopters/scout.txt", "DisplayName = Scout Mk2\n");
        try (ContentPack a = ContentPack.open(folder(dir.resolve("a"), files));
             ContentPack b = ContentPack.open(folder(dir.resolve("b"), edited))) {
            assertNotEquals(a.hash(), b.hash());
        }
    }

    @Test
    void theArchiveOfAFolderOpensToTheSamePack(@TempDir Path dir) throws Exception {
        try (ContentPack folder = ContentPack.open(folder(dir.resolve("scouts"), pack()))) {
            byte[] archive = folder.archive();
            assertEquals(archive.length, folder.archiveSize());
            Path copy = Files.write(dir.resolve("copy.zip"), archive);
            try (ContentPack zipped = ContentPack.open(copy)) {
                assertEquals(folder.hash(), zipped.hash());
            }
        }
    }

    @Test
    void systemFilesAreIgnored(@TempDir Path dir) throws Exception {
        Map<String, String> files = pack();
        Map<String, String> messy = new TreeMap<>(files);
        messy.put(".DS_Store", "x");
        messy.put("assets/mcheli/textures/Thumbs.db", "x");
        try (ContentPack clean = ContentPack.open(folder(dir.resolve("clean"), files));
             ContentPack withJunk = ContentPack.open(folder(dir.resolve("messy"), messy))) {
            assertEquals(clean.hash(), withJunk.hash());
        }
    }

    @Test
    void anythingButDataIsRefused(@TempDir Path dir) throws Exception {
        assertRefused(dir, "assets/mcheli/Evil.class", "only hold definitions");
        assertRefused(dir, "mod.jar", "may sit at the top");
        assertRefused(dir, "data/mcheli/recipe/scout.json", "only hold definitions");
        assertRefused(dir, "assets/mcheli/textures/helicopters/Scout.png", "lower case");
    }

    @Test
    void aPackNeedsAManifestWithAnId(@TempDir Path dir) throws Exception {
        Map<String, String> files = pack();
        files.remove(PackManifest.FILE);
        PackException missing = assertThrows(PackException.class, () -> ContentPack.open(folder(dir.resolve("a"), files)));
        assertTrue(missing.getMessage().contains("no pack.properties"));

        files.put(PackManifest.FILE, "id = Not Valid\n");
        PackException invalid = assertThrows(PackException.class, () -> ContentPack.open(folder(dir.resolve("b"), files)));
        assertTrue(invalid.getMessage().contains("must be 1 to 64 characters"));

        files.put(PackManifest.FILE, "id = original\n");
        PackException reserved = assertThrows(PackException.class, () -> ContentPack.open(folder(dir.resolve("c"), files)));
        assertTrue(reserved.getMessage().contains("reserved"));
    }

    @Test
    void aFileThatIsNoZipIsRefused(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("broken.zip"), "not a zip");
        PackException e = assertThrows(PackException.class, () -> ContentPack.open(file));
        assertEquals("not a zip file", e.getMessage());
    }

    private static void assertRefused(Path dir, String extraFile, String expected) throws IOException {
        Map<String, String> files = pack();
        files.put(extraFile, "x");
        Path folder = folder(dir.resolve("refused-" + Math.abs(extraFile.hashCode())), files);
        PackException e = assertThrows(PackException.class, () -> ContentPack.open(folder));
        assertTrue(e.getMessage().startsWith(extraFile + ": ") && e.getMessage().contains(expected), e.getMessage());
    }

    static Map<String, String> pack() {
        Map<String, String> files = new TreeMap<>();
        files.put(PackManifest.FILE, MANIFEST);
        files.put("README.md", "Scouts\n");
        files.put("assets/mcheli/helicopters/scout.txt", "DisplayName = Scout\nAddSeat = 0, 1, 0\n");
        files.put("assets/mcheli/models/helicopters/scout.obj", "o $body\nv 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n");
        return files;
    }

    static Path folder(Path folder, Map<String, String> files) throws IOException {
        for (Map.Entry<String, String> file : files.entrySet()) {
            Path path = folder.resolve(file.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, file.getValue());
        }
        return folder;
    }

    static Path zip(Path zip, String prefix, Map<String, String> files) throws IOException {
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zipOut = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                zipOut.putNextEntry(new ZipEntry(prefix + file.getKey()));
                zipOut.write(file.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zipOut.closeEntry();
            }
        }
        return zip;
    }
}
