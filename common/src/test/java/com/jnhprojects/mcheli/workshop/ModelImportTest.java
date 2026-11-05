package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.DefinitionReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelImportTest {
    private static final String CUBE = """
            o $body
            v 0 0 0
            v 2 0 0
            v 2 3 0
            v 0 3 0
            vt 0 0
            vt 1 0
            vt 1 1
            vt 0 1
            f 1/1 2/2 3/3 4/4
            """;

    @TempDir
    Path dir;

    private Path write(String name, String text) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, text);
        return file;
    }

    private String errorOf(Path file) {
        return assertThrows(WorkshopException.class, () -> ModelImport.check(file)).getMessage();
    }

    @Test
    void aGoodModelHasNoWarnings() throws Exception {
        ModelImport model = ModelImport.check(write("cube.obj", CUBE));
        assertEquals(List.of(), model.warnings());
        assertEquals(2, model.mesh().triangleCount());
        assertEquals(2.0F, model.max()[0]);
        assertEquals(3.0F, model.max()[1]);
    }

    @Test
    void largerPolygonsBecomeTriangles() throws Exception {
        String pentagon = """
                o $body
                v 0 0 0
                v 1 0 0
                v 2 1 0
                v 1 2 0
                v 0 1 0
                f 1 2 3 4 5
                """;
        assertEquals(3, ModelImport.check(write("five.obj", pentagon)).mesh().triangleCount());
    }

    @Test
    void otherFormatsAreNamedAsSuch() throws Exception {
        assertTrue(errorOf(write("heli.fbx", "x")).contains("not an OBJ"));
    }

    @Test
    void aMissingFileIsReported() {
        assertTrue(errorOf(dir.resolve("gone.obj")).contains("No file"));
    }

    @Test
    void aBinaryFileIsNotAnObj() throws Exception {
        Path file = dir.resolve("fake.obj");
        Files.write(file, new byte[]{'g', 'l', 'T', 'F', 0, 0, 0, 2});
        assertTrue(errorOf(file).contains("not a text file"));
    }

    @Test
    void aFaceThatUsesAMissingVertexNamesTheLine() throws Exception {
        String message = errorOf(write("bad.obj", "v 0 0 0\nv 1 0 0\nf 1 2 9\n"));
        assertTrue(message.contains("line 3"), message);
        assertTrue(message.contains("vertex 9"), message);
    }

    @Test
    void aBadNumberNamesTheLine() throws Exception {
        String message = errorOf(write("bad.obj", "v 0 0 0\nv one 0 0\n"));
        assertTrue(message.contains("line 2"), message);
    }

    @Test
    void aFileWithNoFacesIsRefused() throws Exception {
        assertTrue(errorOf(write("empty.obj", "v 0 0 0\n")).contains("no faces"));
    }

    @Test
    void aModelWithoutABodyIsWarnedAbout() throws Exception {
        ModelImport model = ModelImport.check(write("nobody.obj", CUBE.replace("o $body\n", "")));
        assertTrue(model.warnings().stream().anyMatch(w -> w.contains("$body")));
    }

    @Test
    void aSixteenTimesTooLargeModelIsWarnedAbout() throws Exception {
        ModelImport model = ModelImport.check(write("big.obj", CUBE.replace("v 2 3 0", "v 2 300 0")));
        assertTrue(model.warnings().stream().anyMatch(w -> w.contains("1/16")));
    }

    @Test
    void anUnmappedModelIsWarnedAbout() throws Exception {
        ModelImport model = ModelImport.check(write("plain.obj", "o $body\nv 0 0 0\nv 1 0 0\nv 1 1 0\nf 1 2 3\n"));
        assertTrue(model.warnings().stream().anyMatch(w -> w.contains("texture coordinates")));
    }

    @Test
    void theTextureIsFoundByTheMaterialFile() throws Exception {
        Path obj = write("heli.obj", CUBE);
        write("heli.mtl", "newmtl paint\nmap_Kd paint_job.png\n");
        Path png = write("paint_job.png", "not checked here");
        assertEquals(png.toAbsolutePath(), ModelImport.findTexture(obj));
    }

    @Test
    void theTextureIsFoundByName() throws Exception {
        Path obj = write("heli.obj", CUBE);
        Path png = write("heli.png", "not checked here");
        assertEquals(png.toAbsolutePath(), ModelImport.findTexture(obj));
    }

    @Test
    void noTextureIsFine() throws Exception {
        assertNull(ModelImport.findTexture(write("heli.obj", CUBE)));
    }

    @Test
    void partsTheModelLacksAreListed() throws Exception {
        ModelImport model = ModelImport.check(write("heli.obj", CUBE + "o $blade0\nf 1 2 3\n"));
        HelicopterDefinition def = DefinitionReader.read("""
                AddSeat = 0, 1, 0
                AddRotor = 2, 180, 0, 2, 0, 0, 1, 0
                AddRotor = 2, 180, 0, 2, -3, 1, 0, 0
                AddPartLG = 0, 0, 0, 1, 0, 0
                """, new HelicopterDefinition("heli")).definition();
        assertEquals(List.of("$blade1", "$lg0"), ModelImport.missingParts(def, model.mesh()));
    }

    @Test
    void eitherCanopyNameCounts() throws Exception {
        ModelImport model = ModelImport.check(write("heli.obj", CUBE + "o $canopy\nf 1 2 3\n"));
        HelicopterDefinition def = DefinitionReader.read("AddSeat = 0, 1, 0\nAddPartCanopy = 0, 0, 0, 1, 0, 0\n",
                new HelicopterDefinition("heli")).definition();
        assertFalse(ModelImport.missingParts(def, model.mesh()).contains("$canopy0"));
    }
}
