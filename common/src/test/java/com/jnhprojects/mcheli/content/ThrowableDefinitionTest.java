package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.content.throwable.ThrowableDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowableDefinitionTest {

    /** The shape of the original M18 files, written here so the test runs without the import. */
    private static final String SMOKE = """
            DisplayName = Smoke (Red)
            AddDisplayName = ja_JP, Smoke
            ItemID = 28902
            StackSize = 1
            AliveTime = 30
            Explosion = 0
            TimeFuse = 2
            Bound = 0.3
            Gravity = -0.04
            GravityInWater = 0.01
            SmokeSize = 10
            SmokeNum = 6
            SmokeColor = 180, 30, 30
            SmokeVelocityVertical   = 0.7
            SmokeVelocityHorizontal = 0.7
            AddRecipe = "DY",  Y, iron_ingot, D,dye,1
            DispenseAcceleration = 1.1
            """;

    @Test
    void readsSmokeGrenade(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("m18_red.txt");
        Files.writeString(file, SMOKE);

        DefinitionReader.Result<ThrowableDefinition> result = DefinitionReader.read(file, new ThrowableDefinition("m18_red"));
        assertTrue(result.isValid());
        assertTrue(result.unhandledKeys().isEmpty(), () -> "unhandled " + result.unhandledKeys());
        ThrowableDefinition smoke = result.definition();

        assertEquals("Smoke (Red)", smoke.displayName);
        assertTrue(smoke.smokes());
        // Seconds in the file, ticks in the definition.
        assertEquals(40, smoke.timeFuse);
        assertEquals(600, smoke.aliveTime);
        assertEquals(0.3F, smoke.bound, 1.0E-6);
        assertEquals(-0.04F, smoke.gravity, 1.0E-6);
        assertEquals(6, smoke.smokeNum);
        assertEquals(0xB41E1E, smoke.smokeColor);
        assertEquals(1, smoke.recipes.size());
    }

    @Test
    void registryLoadsThrowableFolder(@TempDir Path assets) throws IOException {
        Files.createDirectories(assets.resolve(ThrowableDefinition.FOLDER));
        Files.writeString(assets.resolve(ThrowableDefinition.FOLDER).resolve("m18_red.txt"), SMOKE);

        ContentRegistry registry = ContentRegistry.load(assets);
        assertNotNull(registry.throwable("m18_red"));
        assertEquals(1, registry.throwables().size());
    }
}
