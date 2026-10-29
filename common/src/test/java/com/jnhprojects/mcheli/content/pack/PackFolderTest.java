package com.jnhprojects.mcheli.content.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scanning {@code mcheli/packs}: bad packs are reported and left out, never fatal. */
class PackFolderTest {

    @Test
    void loadsGoodPacksAndReportsTheRest(@TempDir Path dir) throws IOException {
        Map<String, String> other = new TreeMap<>(ContentPackTest.pack());
        other.put(PackManifest.FILE, "id = another\n");
        ContentPackTest.folder(dir.resolve("b-scouts"), ContentPackTest.pack());
        ContentPackTest.zip(dir.resolve("a-another.zip"), "", other);
        ContentPackTest.zip(dir.resolve("c-scouts-copy.zip"), "", ContentPackTest.pack());
        Files.writeString(dir.resolve("d-broken.zip"), "not a zip");
        Files.writeString(dir.resolve("notes.txt"), "not a pack");

        PackFolder.Scan scan = PackFolder.scan(dir);
        try {
            assertEquals(List.of("another", "scouts"), scan.packs().stream().map(ContentPack::id).toList());
            assertEquals(List.of("c-scouts-copy.zip", "d-broken.zip"), scan.problems().stream().map(PackProblem::pack).toList());
            assertTrue(scan.problems().getFirst().message().contains("b-scouts"));
        } finally {
            for (ContentPack pack : scan.packs()) {
                pack.close();
            }
        }
    }

    @Test
    void aMissingFolderHasNoPacks(@TempDir Path dir) {
        PackFolder.Scan scan = PackFolder.scan(dir.resolve("packs"));
        assertTrue(scan.packs().isEmpty());
        assertTrue(scan.problems().isEmpty());
    }
}
