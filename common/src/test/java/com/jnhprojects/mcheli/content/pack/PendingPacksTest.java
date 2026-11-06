package com.jnhprojects.mcheli.content.pack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Anyone on a server can send these files, so most of this is about what must not get through. */
class PendingPacksTest {
    private static final UUID ALICE = new UUID(1, 1);
    private static final UUID BOB = new UUID(2, 2);

    @TempDir
    Path dir;
    private Path packs;
    private PendingPacks pending;

    @BeforeEach
    void setUp() {
        packs = dir.resolve("packs");
        pending = new PendingPacks(dir.resolve("pending"));
    }

    private static byte[] zip(String id, String... extraFiles) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            out.putNextEntry(new ZipEntry(PackManifest.FILE));
            out.write(("id = " + id + "\nauthor = Someone\n").getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("assets/mcheli/helicopters/" + id + ".txt"));
            out.write("DisplayName = Test\n".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            for (String file : extraFiles) {
                out.putNextEntry(new ZipEntry(file));
                out.write(1);
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    @Test
    void keepsAPackWithWhoSentIt() throws Exception {
        PendingPacks.Submission stored = pending.store(zip("heli"), "Alice", ALICE);
        assertEquals("heli", stored.id());
        assertEquals("Someone", stored.manifest().author());
        assertEquals("Alice", stored.submitter());
        assertEquals(List.of("helicopters/heli.txt"), stored.definitions());
        assertEquals(1, pending.list().size());
        assertEquals(stored.hash(), pending.find("heli").hash());
        assertNull(pending.find("other"));
    }

    @Test
    void namesFilesByHashNotByAnythingThePlayerChose() throws Exception {
        PendingPacks.Submission stored = pending.store(zip("heli"), "..\\..\\evil", ALICE);
        assertEquals(stored.hash() + ".zip", stored.file().getFileName().toString());
        assertEquals(dir.resolve("pending"), stored.file().getParent());
    }

    @Test
    void refusesWhatIsNotAPackAndKeepsNothing() throws Exception {
        assertThrows(PackException.class, () -> pending.store(new byte[]{1, 2, 3}, "Alice", ALICE));
        assertThrows(PackException.class, () -> pending.store(zip("heli", "mods/evil.jar"), "Alice", ALICE));
        assertThrows(PackException.class, () -> pending.store(zip("heli", "assets/mcheli/run.class"), "Alice", ALICE));
        assertThrows(PackException.class, () -> pending.store(zip("../evil"), "Alice", ALICE));
        assertTrue(pending.list().isEmpty());
        try (var files = Files.list(dir.resolve("pending"))) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void aPlayerHasOneSubmissionAtATime() throws Exception {
        pending.store(zip("first"), "Alice", ALICE);
        pending.store(zip("second"), "Alice", ALICE);
        assertEquals(1, pending.list().size());
        assertNull(pending.find("first"));
        assertNotNull(pending.find("second"));
        try (var files = Files.list(dir.resolve("pending"))) {
            assertEquals(2, files.count());
        }
    }

    @Test
    void twoPlayersCannotWaitWithTheSameId() throws Exception {
        pending.store(zip("heli"), "Alice", ALICE);
        PackException e = assertThrows(PackException.class, () -> pending.store(zip("heli", "README.md"), "Bob", BOB));
        assertTrue(e.getMessage().contains("Alice"));
        assertEquals("Alice", pending.find("heli").submitter());
    }

    @Test
    void theQueueIsBounded() throws Exception {
        for (int i = 0; i < PendingPacks.MAX_PENDING; i++) {
            pending.store(zip("pack" + i), "Player" + i, new UUID(9, i));
        }
        assertThrows(PackException.class, () -> pending.store(zip("one-more"), "Late", new UUID(10, 0)));
        // Someone already waiting can still replace their own.
        pending.store(zip("pack0-fixed"), "Player0", new UUID(9, 0));
        assertEquals(PendingPacks.MAX_PENDING, pending.list().size());
    }

    @Test
    void rejectingRemovesEverything() throws Exception {
        pending.remove(pending.store(zip("heli"), "Alice", ALICE));
        assertTrue(pending.list().isEmpty());
        try (var files = Files.list(dir.resolve("pending"))) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void installingPutsThePackWhereTheGameLoadsIt() throws Exception {
        PendingPacks.Submission stored = pending.store(zip("heli"), "Alice", ALICE);
        Path installed = pending.install(stored, packs);
        assertEquals(packs.resolve("heli.zip"), installed);
        assertTrue(pending.list().isEmpty());
        PackFolder.Scan scan = PackFolder.scan(packs);
        assertEquals(1, scan.packs().size());
        assertEquals(stored.hash(), scan.packs().getFirst().hash());
        scan.packs().getFirst().close();
    }

    /** Windows will not overwrite a zip the game has open, and the game keeps its packs open. */
    @Test
    void anUpdateReplacesAZipThatIsStillOpen() throws Exception {
        Files.createDirectories(packs);
        Files.write(packs.resolve("heli.zip"), zip("heli"));
        try (ContentPack inUse = ContentPack.open(packs.resolve("heli.zip"))) {
            PendingPacks.Submission update = pending.store(zip("heli", "README.md"), "Alice", ALICE);
            pending.install(update, packs);

            // The open pack can still read its old files.
            assertTrue(Files.exists(inUse.assets().resolve("helicopters/heli.txt")));
            PackFolder.Scan scan = PackFolder.scan(packs);
            assertEquals(1, scan.packs().size());
            assertEquals(List.of(), scan.problems());
            assertEquals(update.hash(), scan.packs().getFirst().hash());
            scan.packs().getFirst().close();
        }
        PendingPacks.cleanReplaced(packs);
        try (var files = Files.list(packs)) {
            assertEquals(List.of("heli.zip"), files.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void aFolderPackIsNeverReplaced() throws Exception {
        Path folder = Files.createDirectories(packs.resolve("heli"));
        Files.writeString(folder.resolve(PackManifest.FILE), "id = heli\n");
        PendingPacks.Submission update = pending.store(zip("heli"), "Alice", ALICE);
        assertThrows(PackException.class, () -> pending.install(update, packs));
        assertTrue(Files.exists(folder.resolve(PackManifest.FILE)));
        assertNotNull(pending.find("heli"));
    }

    @Test
    void anUnrelatedFileWithTheSameNameIsLeftAlone() throws Exception {
        Files.createDirectories(packs);
        Files.write(packs.resolve("heli.zip"), zip("other"));
        Path installed = pending.install(pending.store(zip("heli"), "Alice", ALICE), packs);
        assertFalse(installed.equals(packs.resolve("heli.zip")));
        PackFolder.Scan scan = PackFolder.scan(packs);
        assertEquals(2, scan.packs().size());
        for (ContentPack pack : scan.packs()) {
            pack.close();
        }
    }
}
