package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.content.pack.PackManifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PNGs and OGGs taken in by the workshop, and the pack browser over what comes out. */
class WorkshopAssetsTest {
    private static final WorkshopProject.SoundSlot ENGINE = new WorkshopProject.SoundSlot(WorkshopProject.SoundKind.ENGINE, null);
    private static final WorkshopProject.SoundSlot ROTOR = new WorkshopProject.SoundSlot(WorkshopProject.SoundKind.ROTOR, null);

    @TempDir
    Path dir;
    private Path packs;
    private WorkshopProject project;

    @BeforeEach
    void setUp() throws Exception {
        packs = Files.createDirectory(dir.resolve("packs"));
        Path obj = dir.resolve("heli.obj");
        Files.writeString(obj, """
                o $body
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vt 0 0
                vt 1 0
                vt 0 1
                f 1/1 2/2 3/3
                o $cockpit
                v 0 0 1
                v 1 0 1
                v 0 1 1
                f 4/1 5/2 6/3
                """);
        project = WorkshopProject.create(packs, "test-pack", WorkshopKind.HELICOPTER, "heli", "Someone", obj, null);
    }

    private Path png(String name, int width, int height) throws Exception {
        Path file = dir.resolve(name);
        Files.write(file, pngBytes(width, height));
        return file;
    }

    private static byte[] pngBytes(int width, int height) {
        byte[] bytes = new byte[33];
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        bytes[11] = 13;
        bytes[12] = 'I';
        bytes[13] = 'H';
        bytes[14] = 'D';
        bytes[15] = 'R';
        for (int i = 0; i < 4; i++) {
            bytes[16 + i] = (byte) (width >>> (24 - i * 8));
            bytes[20 + i] = (byte) (height >>> (24 - i * 8));
        }
        return bytes;
    }

    private Path ogg(String name, String codec) throws Exception {
        Path file = dir.resolve(name);
        byte[] page = new byte[64];
        byte[] magic = "OggS".getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(magic, 0, page, 0, magic.length);
        byte[] head = codec.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(head, 0, page, 29, head.length);
        Files.write(file, page);
        return file;
    }

    private AircraftDefinition loadSaved() throws Exception {
        project.save();
        try (ContentPack pack = ContentPack.open(project.root())) {
            ContentRegistry registry = ContentRegistry.load(List.of(pack.contentSource()));
            assertEquals(List.of(), registry.problems());
            return registry.any("heli");
        }
    }

    @Test
    void partsWithTexturesComeBackFromTheModel() {
        assertEquals(List.of("body", "cockpit"), project.texturableParts());
    }

    @Test
    void aPartTextureIsCopiedInAndLoads() throws Exception {
        WorkshopProject.ImportedTexture imported = project.importTexture("cockpit", png("Paint Job.png", 64, 32));
        assertEquals("heli_cockpit", imported.name());
        assertEquals(64, imported.checked().width());
        assertTrue(Files.exists(project.assets().resolve("textures/helicopters/heli_cockpit.png")));
        assertEquals("heli_cockpit", project.partTexture("cockpit"));

        AircraftDefinition def = loadSaved();
        assertEquals("heli_cockpit", def.partTextures.get("cockpit"));
        assertEquals("Someone", def.credit().author());
    }

    @Test
    void theSamePartNeverGetsTwoLines() throws Exception {
        project.importTexture("cockpit", png("a.png", 16, 16));
        project.importTexture("cockpit", png("b.png", 32, 32));
        assertEquals(1, project.document().entries().stream().filter(e -> e.key().equals("PartTexture")).count());
    }

    @Test
    void aWholeVehicleTextureReplacesItsFileAndAddsNoLine() throws Exception {
        List<String> before = project.document().snapshot();
        project.importTexture(null, png("new.png", 64, 64));
        assertEquals(before, project.document().snapshot());
        assertEquals(64, AssetImport.texture(project.assets().resolve("textures/helicopters/heli.png")).width());
    }

    @Test
    void clearingAPartGoesBackToThePaintAndUndoWorks() throws Exception {
        project.importTexture("cockpit", png("a.png", 16, 16));
        List<String> imported = project.document().snapshot();
        project.clearPartTexture("cockpit");
        assertNull(project.partTexture("cockpit"));
        project.document().restore(imported);
        assertEquals("heli_cockpit", project.partTexture("cockpit"));
        assertTrue(Files.exists(project.assets().resolve("textures/helicopters/heli_cockpit.png")));
    }

    @Test
    void importingAFileFromThePackItselfChangesNothing() throws Exception {
        project.importTexture("cockpit", png("a.png", 16, 16));
        Path inPack = project.assets().resolve("textures/helicopters/heli_cockpit.png");
        project.importTexture("cockpit", inPack);
        assertEquals(16, AssetImport.texture(inPack).width());
    }

    @Test
    void refusesWhatIsNotAPng() throws Exception {
        Path fake = dir.resolve("fake.png");
        Files.writeString(fake, "not an image at all, not even close to one");
        assertThrows(WorkshopException.class, () -> project.importTexture("cockpit", fake));
        Path jpeg = dir.resolve("photo.jpg");
        Files.writeString(jpeg, "x");
        assertThrows(WorkshopException.class, () -> project.importTexture("cockpit", jpeg));
        assertThrows(WorkshopException.class, () -> project.importTexture("cockpit", dir.resolve("missing.png")));
        assertThrows(WorkshopException.class, () -> AssetImport.texture(png("huge.png", 100000, 16)));
        assertNull(project.partTexture("cockpit"));
    }

    @Test
    void warnsAboutALargeTexture() throws Exception {
        assertFalse(AssetImport.texture(png("large.png", 4096, 4096)).warnings().isEmpty());
        assertTrue(AssetImport.texture(png("fine.png", 1024, 1024)).warnings().isEmpty());
    }

    @Test
    void engineAndRotorSoundsLoad() throws Exception {
        assertEquals("heli_engine", project.importSound(ENGINE, ogg("Engine Loop.ogg", "\u0001vorbis")).name());
        assertEquals("heli_rotor", project.importSound(ROTOR, ogg("rotor.ogg", "\u0001vorbis")).name());
        assertTrue(Files.exists(project.assets().resolve("sounds/heli_engine.ogg")));
        assertEquals("heli_engine", project.sound(ENGINE));

        AircraftDefinition def = loadSaved();
        assertEquals("heli_engine", def.sound);
        assertEquals("heli_rotor", def.rotorSound);
    }

    @Test
    void aWeaponSoundKeepsItsPitchWhenReplaced() throws Exception {
        project.document().insert(project.document().size(), "AddWeapon", "m230, 0, 0, 0");
        project.refresh();
        WorkshopProject.SoundSlot gun = new WorkshopProject.SoundSlot(WorkshopProject.SoundKind.WEAPON, "m230");
        assertTrue(project.soundSlots().contains(gun));

        project.document().setKeyed("WeaponSound", "m230", "m230, old, 1.25");
        project.refresh();
        project.importSound(gun, ogg("gun.ogg", "\u0001vorbis"));
        project.refresh();
        assertEquals("heli_m230", project.sound(gun));
        assertEquals(1.25F, project.definition().weaponSounds.get("m230").pitch());

        project.setSound(gun, null);
        project.refresh();
        assertNull(project.sound(gun));
    }

    @Test
    void tanksHaveNoRotorSlot() throws Exception {
        Path obj = project.assets().resolve("models/helicopters/heli.obj");
        WorkshopProject tank = WorkshopProject.create(packs, "tank-pack", WorkshopKind.TANK, "tank", "", obj, null);
        assertFalse(tank.soundSlots().contains(ROTOR));
        assertTrue(tank.soundSlots().contains(ENGINE));
    }

    @Test
    void refusesWhatTheGameCannotPlay() throws Exception {
        WorkshopException opus = assertThrows(WorkshopException.class, () -> project.importSound(ENGINE, ogg("voice.ogg", "OpusHead")));
        assertTrue(opus.getMessage().contains("Opus"));
        assertThrows(WorkshopException.class, () -> project.importSound(ENGINE, ogg("other.ogg", "FLAC")));
        Path mp3 = dir.resolve("song.mp3");
        Files.writeString(mp3, "x");
        assertThrows(WorkshopException.class, () -> project.importSound(ENGINE, mp3));
        Path renamed = dir.resolve("renamed.ogg");
        Files.writeString(renamed, "RIFF and so on");
        assertThrows(WorkshopException.class, () -> project.importSound(ENGINE, renamed));
        assertNull(project.sound(ENGINE));
    }

    @Test
    void theBrowserListsWhoMadeWhat() throws Exception {
        project.save();
        PackCatalog.Listing listing = PackCatalog.read(packs);
        assertEquals(List.of(), listing.problems());
        PackCatalog.Info info = listing.packs().getFirst();
        assertEquals("Someone", info.manifest().author());
        assertEquals(List.of("heli"), info.vehicles());
        assertFalse(info.zipped());
        assertTrue(info.size() > 0);
    }

    @Test
    void theBrowserReportsABrokenPack() throws Exception {
        Files.writeString(packs.resolve("broken.zip"), "not a zip");
        PackCatalog.Listing listing = PackCatalog.read(packs);
        assertEquals(1, listing.packs().size());
        assertEquals("broken.zip", listing.problems().getFirst().pack());
    }

    @Test
    void installingCopiesAZipAndKeepsWhatIsThere() throws Exception {
        Path exported = project.export(dir.resolve("out"));
        Path other = Files.createDirectory(dir.resolve("other"));
        PackManifest manifest = PackCatalog.install(exported, other);
        assertEquals("test-pack", manifest.id());
        assertNotNull(PackCatalog.read(other).packs().getFirst());
        assertThrows(WorkshopException.class, () -> PackCatalog.install(exported, other));
        assertThrows(WorkshopException.class, () -> PackCatalog.install(exported, packs));

        Path notAPack = dir.resolve("photos.zip");
        Files.writeString(notAPack, "nope");
        assertThrows(WorkshopException.class, () -> PackCatalog.install(notAPack, other));
    }
}
