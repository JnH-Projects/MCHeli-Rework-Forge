package com.jnhprojects.mcheli.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSpecTest {
    private final ConfigSpec.Builder builder = ConfigSpec.builder();
    private final ConfigSpec.BooleanValue breakBlocks;
    private final ConfigSpec.DoubleValue damageFactor;
    private final ConfigSpec.StringListValue disabled;
    private final ConfigSpec.IntValue maxPackSize;
    private final ConfigSpec spec;

    ConfigSpecTest() {
        builder.push("explosions");
        breakBlocks = builder.comment("Whether weapons break blocks.").define("breakBlocks", true);
        builder.pop();
        builder.push("vehicles");
        damageFactor = builder.defineInRange("damageFactor", 1.0, 0.0, 100.0);
        disabled = builder.defineList("disabled", List.of());
        builder.pop();
        builder.push("packs");
        maxPackSize = builder.defineInRange("maxPackSize", 8, 1, 512);
        builder.pop();
        spec = builder.build();
    }

    @Test
    void readsTheFileForgeWrote(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("mcheli-server.toml");
        Files.writeString(file, """
                [explosions]
                	#Whether weapons and crashing vehicles break blocks.
                	breakBlocks = false

                [vehicles]
                	#Range: 0.0 ~ 100.0
                	damageFactor = 0.5
                	disabled = ["ah-64", "f-35b"]

                [packs]
                	maxPackSize = 16
                """, StandardCharsets.UTF_8);

        spec.load(file);

        assertFalse(breakBlocks.get());
        assertEquals(0.5, damageFactor.get());
        assertEquals(List.of("ah-64", "f-35b"), disabled.get());
        assertEquals(16, maxPackSize.get());
    }

    @Test
    void badValuesFallBackToTheDefault(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("bad.toml");
        Files.writeString(file, """
                [explosions]
                breakBlocks = maybe
                [vehicles]
                damageFactor = 1000.0
                disabled = [ "a",
                  "b # not a comment" ]
                [packs]
                maxPackSize = 0
                """, StandardCharsets.UTF_8);

        spec.load(file);

        assertTrue(breakBlocks.get());
        assertEquals(1.0, damageFactor.get());
        assertEquals(List.of("a", "b # not a comment"), disabled.get());
        assertEquals(8, maxPackSize.get());
    }

    @Test
    void writesEveryKeyBackAndReadsItAgain(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sub").resolve("new.toml");

        spec.load(file);
        String written = Files.readString(file, StandardCharsets.UTF_8);
        spec.load(file);

        assertTrue(written.contains("\t# Whether weapons break blocks.\n\tbreakBlocks = true\n"), written);
        assertTrue(written.contains("\t#Range: 1 ~ 512\n\tmaxPackSize = 8\n"), written);
        assertEquals(written, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void unloadingGoesBackToTheDefaults(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("c.toml");
        Files.writeString(file, "[packs]\nmaxPackSize = 32\n", StandardCharsets.UTF_8);
        spec.load(file);
        assertTrue(spec.isLoaded());

        spec.unload();

        assertFalse(spec.isLoaded());
        assertEquals(8, maxPackSize.get());
    }
}
