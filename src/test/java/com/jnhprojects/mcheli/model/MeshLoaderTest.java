package com.jnhprojects.mcheli.model;

import com.jnhprojects.mcheli.TestResources;
import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.RotorDefinition;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeshLoaderTest {

    private static final String QUAD_MQO = """
            Metasequoia Document
            Format Text Ver 1.1
            Object "$body" {
                shading 0
                vertex 4 {
                    0 0 0
                    100 0 0
                    100 0 100
                    0 0 100
                }
                face 1 {
                    4 V(0 1 2 3) M(0) UV(0 0 1 0 1 1 0 1)
                }
            }
            Object "x" {
                vertex 1 {
                    0 0 0
                }
            }
            Object "$blade0" {
                vertex 3 {
                    0 0 0
                    0 100 0
                    100 0 0
                }
                face 1 {
                    3 V(0 1 2) UV(0 0 0 1 1 0)
                }
            }
            """;

    @Test
    void splitsQuadsScalesAndReversesWinding() throws IOException {
        Mesh mesh = MqoLoader.load("quad.mqo", stream(QUAD_MQO));

        // The single character object name is skipped like in the original loader.
        assertEquals(2, mesh.groups().size());
        MeshGroup body = mesh.groups().getFirst();
        assertEquals(6, body.vertexCount());

        // First triangle is (v2, v1, v0): reversed winding, scaled from centimetres.
        assertEquals(1.0F, body.positions()[0], 1.0E-6);
        assertEquals(1.0F, body.positions()[2], 1.0E-6);
        assertEquals(1.0F, body.uvs()[0], 1.0E-6);
        assertEquals(1.0F, body.uvs()[1], 1.0E-6);
        // With the reversed winding this face points up.
        assertEquals(1.0F, body.normals()[1], 1.0E-6);
    }

    @Test
    void resolvesDollarParts() throws IOException {
        Mesh mesh = MqoLoader.load("quad.mqo", stream(QUAD_MQO));
        assertEquals(1, mesh.part("$body").size());
        assertEquals(1, mesh.part("$BLADE0").size());
        assertTrue(mesh.part("$missing").isEmpty());
        assertEquals(mesh.part("$body"), mesh.body());
    }

    @Test
    void readsObjGroupsAndFlipsV() throws IOException {
        Mesh mesh = ObjLoader.load("tri.obj", stream("""
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vt 0 0.25
                o ignored
                g $body
                f 1/1 2/1 3/1
                """));
        assertEquals(1, mesh.groups().size());
        assertEquals("$body", mesh.groups().getFirst().name());
        assertEquals(0.75F, mesh.groups().getFirst().uvs()[1], 1.0E-6);
    }

    @Test
    void everyHelicopterModelHasItsRotorParts() throws IOException {
        Path assets = TestResources.assets();
        ContentRegistry registry = ContentRegistry.load(assets);
        for (HelicopterDefinition heli : registry.helicopters()) {
            Path model = modelFile(assets, heli.name());
            assertTrue(Files.exists(model), () -> "No model for " + heli.name());
            Mesh mesh;
            try (InputStream in = Files.newInputStream(model)) {
                mesh = MeshLoader.load(model.getFileName().toString(), in);
            }
            assertFalse(mesh.body().isEmpty(), heli.name());
            assertTrue(mesh.triangleCount() > 0, heli.name());
            for (RotorDefinition rotor : heli.rotors) {
                assertFalse(mesh.part("$" + rotor.part()).isEmpty(), () -> heli.name() + " is missing $" + rotor.part());
            }
        }
    }

    private static Path modelFile(Path assets, String name) {
        Path mqo = assets.resolve("models/helicopters/" + name + ".mqo");
        return Files.exists(mqo) ? mqo : assets.resolve("models/helicopters/" + name + ".obj");
    }

    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
