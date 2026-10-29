package com.jnhprojects.mcheli.content.pack;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.RotorDefinition;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The example pack in {@code examples/} is what pack authors copy, so it has to load cleanly. */
class ExamplePackTest {
    private static final Path PACK = Path.of(System.getProperty("mcheli.examplePack", "../examples/rework-example-pack"));

    @Test
    void loadsWithoutProblems() throws Exception {
        try (ContentPack pack = ContentPack.open(PACK)) {
            assertEquals("rework-example", pack.id());

            ContentRegistry registry = ContentRegistry.load(List.of(pack.contentSource()));
            assertEquals(List.of(), registry.problems());
            HelicopterDefinition scout = registry.helicopter("rework-scout");
            assertNotNull(scout);
            assertEquals("rework-example", scout.pack());

            Path model = pack.assets().resolve("models/helicopters/rework-scout.obj");
            Mesh mesh;
            try (InputStream in = Files.newInputStream(model)) {
                mesh = MeshLoader.load(model.getFileName().toString(), in);
            }
            assertFalse(mesh.body().isEmpty());
            for (RotorDefinition rotor : scout.rotors) {
                assertFalse(mesh.part("$" + rotor.part()).isEmpty(), "model has no $" + rotor.part());
            }

            assertTrue(Files.isRegularFile(pack.assets().resolve("textures/helicopters/rework-scout.png")));
            for (String texture : scout.textures) {
                assertTrue(Files.isRegularFile(pack.assets().resolve("textures/helicopters/" + texture + ".png")), texture);
            }
        }
    }
}
