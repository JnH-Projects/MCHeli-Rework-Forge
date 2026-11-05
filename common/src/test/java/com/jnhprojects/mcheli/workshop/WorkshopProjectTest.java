package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.content.pack.PackFolder;
import com.jnhprojects.mcheli.content.pack.PackManifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the workshop writes has to be a pack the game loads like any other. */
class WorkshopProjectTest {
    private static final Path EXAMPLE = Path.of(System.getProperty("mcheli.examplePack", "../examples/rework-example-pack"));

    @TempDir
    Path dir;
    private Path packs;
    private Path obj;

    @BeforeEach
    void setUp() throws Exception {
        packs = Files.createDirectory(dir.resolve("packs"));
        obj = dir.resolve("My Model.obj");
        Files.writeString(obj, """
                o $body
                v 0 0 0
                v 1 0 0
                v 1 1 0
                vt 0 0
                vt 1 0
                vt 1 1
                f 1/1 2/2 3/3
                """);
    }

    private WorkshopProject create() throws WorkshopException {
        return WorkshopProject.create(packs, "my-pack", WorkshopKind.HELICOPTER, "my-heli", "Me", obj, null);
    }

    private ContentRegistry load(Path pack) throws Exception {
        ContentPack open = ContentPack.open(pack);
        return ContentRegistry.load(List.of(open.contentSource()));
    }

    @Test
    void aNewProjectIsAPackThatLoads() throws Exception {
        WorkshopProject project = create();
        assertEquals(List.of(), project.problems());

        ContentRegistry registry = load(project.root());
        assertEquals(List.of(), registry.problems());
        HelicopterDefinition heli = registry.helicopter("my-heli");
        assertNotNull(heli);
        assertEquals(1, heli.seatCount());
        assertEquals("my-pack", heli.pack());
        assertTrue(Files.isRegularFile(project.assets().resolve("textures/helicopters/my-heli.png")));
    }

    @Test
    void thePlainTextureIsARealPng() throws Exception {
        byte[] png = WorkshopProject.plainTexture();
        // Checked with the JDK's own reader, the game's is not available here.
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(1, image.getWidth());
        assertEquals(0xFFB0B0B0, image.getRGB(0, 0));
    }

    @Test
    void aTextureIsTakenInUnderTheVehiclesName() throws Exception {
        Path png = dir.resolve("Paint.png");
        Files.write(png, WorkshopProject.plainTexture());
        WorkshopProject project = WorkshopProject.create(packs, "painted", WorkshopKind.TANK, "my-tank", "", obj, png);
        assertTrue(Files.isRegularFile(project.assets().resolve("textures/tanks/my-tank.png")));
    }

    @Test
    void badNamesAndTakenIdsAreRefused() throws Exception {
        assertThrows(WorkshopException.class,
                () -> WorkshopProject.create(packs, "Upper Case", WorkshopKind.PLANE, "name", "", obj, null));
        assertThrows(WorkshopException.class,
                () -> WorkshopProject.create(packs, "original", WorkshopKind.PLANE, "name", "", obj, null));
        assertThrows(WorkshopException.class,
                () -> WorkshopProject.create(packs, "fine", WorkshopKind.PLANE, "Bad Name", "", obj, null));
        create();
        assertThrows(WorkshopException.class, this::create);
    }

    @Test
    void aBadModelLeavesNoPackBehind() {
        assertThrows(WorkshopException.class,
                () -> WorkshopProject.create(packs, "broken", WorkshopKind.PLANE, "name", "", dir.resolve("gone.obj"), null));
        assertFalse(Files.exists(packs.resolve("broken")));
    }

    @Test
    void savingKeepsEditsAndLinesTheEditorHasNoPanelFor() throws Exception {
        WorkshopProject project = create();
        project.document().insert(0, "SomeFutureKey", "1");
        project.document().set("Speed", "0.4");
        assertTrue(project.isDirty());
        project.save();
        assertFalse(project.isDirty());

        WorkshopProject again = WorkshopProject.open(packs, WorkshopProject.list(packs).getFirst());
        assertEquals("1", again.document().value("SomeFutureKey"));
        assertEquals("0.4", again.document().value("Speed"));
        assertEquals(0.4F, again.definition().speed);
    }

    @Test
    void addingPartsPutsThemWhereTheParserExpectsThem() throws Exception {
        WorkshopProject project = create();
        int second = project.addPart(PartType.SEAT, -1, PartType.SEAT.defaultValue());
        WorkshopPart seat = project.partAt(second);
        assertNotNull(seat);
        assertEquals(PartType.SEAT, seat.type());
        project.refresh();
        assertEquals(2, project.definition().seatCount());
    }

    @Test
    void aSingleLinePartIsNotAddedTwice() throws Exception {
        WorkshopProject project = create();
        int first = project.addPart(PartType.ENGINE, -1, "0, 1, -2");
        assertEquals(first, project.addPart(PartType.ENGINE, -1, "9, 9, 9"));
        project.refresh();
        assertEquals(new Vector3(0, 1, -2), project.definition().enginePosition);
    }

    @Test
    void aChildNeedsItsParentAndGoesBehindIt() throws Exception {
        WorkshopProject project = WorkshopProject.create(packs, "plane-pack", WorkshopKind.PLANE, "my-plane", "", obj, null);
        assertEquals(-1, project.addPart(PartType.PYLON, -1, PartType.PYLON.defaultValue()));

        int wing0 = project.addPart(PartType.WING, -1, PartType.WING.defaultValue());
        int wing1 = project.addPart(PartType.WING, -1, PartType.WING.defaultValue());
        int pylon = project.addPart(PartType.PYLON, wing0, PartType.PYLON.defaultValue());
        assertEquals(wing0 + 1, pylon);
        assertEquals(wing1 + 1, project.parts().stream().filter(p -> p.type() == PartType.WING).toList().get(1).line());

        project.refresh();
        com.jnhprojects.mcheli.content.aircraft.PlaneDefinition def = (com.jnhprojects.mcheli.content.aircraft.PlaneDefinition) project.definition();
        assertEquals(1, def.wings.get(0).pylons().size());
        assertEquals(0, def.wings.get(1).pylons().size());
    }

    @Test
    void partsFromAnotherKindAreLeftAlone() throws Exception {
        WorkshopProject project = create();
        project.document().insert(0, "AddPartWing", "0, 0, 0, 0, 1, 0, 90");
        assertTrue(project.parts().stream().noneMatch(p -> p.type() == PartType.WING));
    }

    @Test
    void theModelsMissingPartsShowAsProblems() throws Exception {
        WorkshopProject project = create();
        project.addPart(PartType.ROTOR, -1, PartType.ROTOR.defaultValue());
        project.refresh();
        assertTrue(project.problems().stream().anyMatch(p -> p.contains("$blade0")));
    }

    @Test
    void aDefinitionTheGameWouldRejectKeepsTheLastGoodOne() throws Exception {
        WorkshopProject project = create();
        int seat = project.parts().getFirst().line();
        project.document().removeLine(seat);
        project.refresh();
        assertTrue(project.problems().stream().anyMatch(p -> p.contains("no seats")));
        assertEquals(1, project.definition().seatCount());
    }

    @Test
    void theManifestSurvivesARoundTrip() throws Exception {
        WorkshopProject project = create();
        project.setManifest(new PackManifest("my-pack", "My \\ pack", "Me = you", "2.0", "Line one\nline two"));
        project.save();
        try (ContentPack pack = ContentPack.open(project.root())) {
            assertEquals("My \\ pack", pack.manifest().name());
            assertEquals("Me = you", pack.manifest().author());
            assertEquals("2.0", pack.manifest().version());
            assertEquals("Line one line two", pack.manifest().description());
        }
    }

    @Test
    void exportingMakesAZipThatLoads() throws Exception {
        WorkshopProject project = create();
        Path zip = project.export(dir.resolve("export"));
        assertEquals("my-pack-1.0.0.zip", zip.getFileName().toString());
        try (ContentPack pack = ContentPack.open(zip)) {
            assertEquals("my-pack", pack.id());
            ContentRegistry registry = ContentRegistry.load(List.of(pack.contentSource()));
            assertNotNull(registry.helicopter("my-heli"));
        }
    }

    @Test
    void aZipPackOpensFromACopyAndIsLeftAlone() throws Exception {
        WorkshopProject first = create();
        Path zip = first.export(packs.resolveSibling("zips"));
        Path installed = packs.resolve("my-pack.zip");
        deleteFolder(first.root());
        Files.move(zip, installed);
        byte[] before = Files.readAllBytes(installed);

        List<WorkshopProject.Entry> entries = WorkshopProject.list(packs);
        assertEquals(1, entries.size());
        assertTrue(entries.getFirst().zipped());

        WorkshopProject copy = WorkshopProject.open(packs, entries.getFirst());
        assertEquals("my-pack-edit", copy.manifest().id());
        assertEquals(packs.resolve("my-pack-edit"), copy.root());
        copy.document().set("Speed", "0.2");
        copy.save();
        assertEquals(List.of(), PackFolder.scan(packs).problems());
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(installed)));

        // Opening it again goes back to the copy instead of starting over.
        WorkshopProject again = WorkshopProject.open(packs, entries.getFirst());
        assertEquals("0.2", again.document().value("Speed"));
    }

    @Test
    void theExamplePackCanBeOpened() throws Exception {
        Path copy = packs.resolve("rework-example");
        try (var walk = Files.walk(EXAMPLE)) {
            for (Path from : walk.toList()) {
                Path to = copy.resolve(EXAMPLE.relativize(from).toString());
                if (Files.isDirectory(from)) {
                    Files.createDirectories(to);
                } else {
                    Files.copy(from, to);
                }
            }
        }
        WorkshopProject.Entry entry = WorkshopProject.list(packs).getFirst();
        assertEquals("rework-scout", entry.vehicle());
        WorkshopProject project = WorkshopProject.open(packs, entry);
        assertEquals(List.of(), project.problems());
        assertEquals(5, project.parts().size());
        assertFalse(project.isDirty());
    }

    private static void deleteFolder(Path folder) throws Exception {
        try (var walk = Files.walk(folder)) {
            for (Path path : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
