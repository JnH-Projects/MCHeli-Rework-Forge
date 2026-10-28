package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.TestResources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the layout {@link LegacyImporter} produces from the original zip. The tests that need
 * the zip are skipped where it is not available, such as on CI.
 */
class LegacyImporterTest {
    private static final Pattern LANG_KEY = Pattern.compile("^ {2}\"([^\"]+)\":", Pattern.MULTILINE);

    @Test
    void producesThe26xLayout() throws IOException {
        Path assets = TestResources.assets();
        assertEquals(130, countFiles(assets.resolve("items")));
        assertTrue(Files.isRegularFile(assets.resolve("models/helicopters/ah-64.mqo")), "paths are lower-cased");
        assertTrue(Files.isRegularFile(assets.resolve("textures/item/ah-64.png")), "textures/items moves to textures/item");
        assertTrue(Files.isRegularFile(assets.resolve("items/ah-64.json")), "item model definition");
        assertTrue(Files.isRegularFile(assets.resolve("models/item/ah-64.json")), "item model");
        assertTrue(Files.readString(assets.resolve("sounds.json")).contains("\"wrench\": {\n    \"sounds\": [\"mcheli:wrench1\""),
                "numbered sound files share one event");
        assertEquals(108, countFiles(TestResources.pack().resolve("data/mcheli/recipe")));
    }

    /** Minecraft merges lang files across packs, so the import and the jar must not fight over a key. */
    @Test
    void translationsDoNotOverlapTheBundledOnes() throws IOException {
        Path importedLang = TestResources.assets().resolve("lang");
        assertTrue(langKeys(importedLang.resolve("en_us.json")).contains("item.mcheli.ah-64"));

        try (Stream<Path> files = Files.list(Path.of(System.getProperty("mcheli.resources", "src/main/resources"), "assets/mcheli/lang"))) {
            for (Path bundledFile : files.toList()) {
                Path importedFile = importedLang.resolve(bundledFile.getFileName());
                if (Files.exists(importedFile)) {
                    Set<String> overlap = langKeys(bundledFile);
                    overlap.retainAll(langKeys(importedFile));
                    assertEquals(Set.of(), overlap, bundledFile.getFileName().toString());
                }
            }
        }
    }

    @Test
    void rejectsAZipWithoutMcheliAssets(@TempDir Path dir) throws IOException {
        Path zip = dir.resolve("other.zip");
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream z = new ZipOutputStream(out)) {
            z.putNextEntry(new ZipEntry("readme.txt"));
            z.write("not mcheli".getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        assertThrows(IOException.class, () -> LegacyImporter.run(zip, dir.resolve("out")));
    }

    @Test
    void sanitizesPathsTheWay26xRequires() {
        assertEquals("textures/items/m1_a2.png", LegacyImporter.sanitize("textures/items/M1#A2.png"));
    }

    private static long countFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

    private static Set<String> langKeys(Path file) throws IOException {
        Set<String> keys = new TreeSet<>();
        Matcher m = LANG_KEY.matcher(Files.readString(file, StandardCharsets.UTF_8));
        while (m.find()) {
            keys.add(m.group(1));
        }
        return keys;
    }
}
