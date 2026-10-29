package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.TestResources;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.RotorDefinition;
import com.jnhprojects.mcheli.content.aircraft.WeaponGroup;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelicopterDefinitionTest {

    @Test
    void parsesAh6() throws IOException {
        HelicopterDefinition ah6 = read(TestResources.assets().resolve("helicopters/ah-6.txt"));

        assertEquals("AH-6 Killer Egg", ah6.displayName);
        assertEquals("AH-6 キラ－エッグ", ah6.localizedNames.get("ja_jp"));
        assertEquals(150, ah6.maxHp);
        assertEquals(0.6F, ah6.speed, 1.0E-6);
        assertEquals(1.2F, ah6.throttleUpDown, 1.0E-6);
        assertEquals(1200, ah6.maxFuel);
        assertEquals("M.A", ah6.category);
        assertTrue(ah6.nightVision);
        assertEquals(List.of("ah-6", "ah-6_jp"), ah6.textures());

        assertEquals(4, ah6.seatCount());
        assertEquals(-0.45, ah6.seats.getFirst().position().x(), 1.0E-9);
        assertEquals(List.of("heli", "none", "none", "none"), ah6.huds);

        assertEquals(2, ah6.rotors.size());
        RotorDefinition tail = ah6.rotors.get(1);
        assertEquals("blade1", tail.part());
        assertEquals(-6.55, tail.position().z(), 1.0E-9);
        assertEquals(1.0, tail.axis().x(), 1.0E-9);

        List<String> weapons = ah6.weaponGroups.stream().map(WeaponGroup::weaponType).toList();
        assertEquals(List.of("m134", "hydra70_l", "hydra70_m264rp", "aim92", "targeting_pod_mob", "targeting_pod_pv10s"), weapons);
        assertEquals(2, ah6.weaponGroups.getFirst().mounts().size());
        assertEquals(1, ah6.weaponGroups.get(4).mounts().getFirst().seatIndex());

        assertEquals(1, ah6.boundingBoxes.size());
        assertEquals(2.4F, ah6.boundingBoxes.getFirst().width(), 1.0E-6);
    }

    @Test
    void appliesHelicopterDefaults() throws IOException {
        HelicopterDefinition def = parse("bare", "AddSeat = 0, 1, 0");

        assertEquals(79.99F, def.rotorSpeed, 1.0E-4);
        assertEquals(-20.0F, def.minRotationPitch);
        assertEquals(20.0F, def.maxRotationPitch);
        assertEquals("heli", def.soundOrDefault());
        // A single seat still gets the gunner HUD slot, as in the original.
        assertEquals(List.of("heli", "heli_gnr"), def.huds);
    }

    @Test
    void rejectsDefinitionWithoutSeats() throws IOException {
        DefinitionReader.Result<HelicopterDefinition> result = parseResult("empty", "DisplayName = Nothing");
        assertFalse(result.isValid());
    }

    @Test
    void keepsLoadingAfterBadLine() throws IOException {
        DefinitionReader.Result<HelicopterDefinition> result = parseResult("broken", """
                MaxHp = lots
                AddSeat = 0, 1, 0
                Speed = 2.0
                """);
        assertTrue(result.isValid());
        assertEquals(1, result.problems().size());
        // Speed is clamped to the helicopter maximum.
        assertEquals(0.8F, result.definition().speed, 1.0E-6);
    }

    @Test
    void everyShippedHelicopterLoadsCleanly() throws IOException {
        try (Stream<Path> files = Files.list(TestResources.assets().resolve("helicopters"))) {
            List<Path> list = files.filter(p -> p.toString().endsWith(".txt")).toList();
            assertEquals(33, list.size());
            for (Path file : list) {
                String name = file.getFileName().toString().replace(".txt", "");
                DefinitionReader.Result<HelicopterDefinition> result = DefinitionReader.read(file, new HelicopterDefinition(name));
                assertTrue(result.isValid(), () -> name + " rejected: " + result.problems());
                assertEquals(List.of(), result.problems(), name);
                assertFalse(result.definition().rotors.isEmpty(), () -> name + " has no rotors");
            }
        }
    }

    @Test
    void registryLoadsAllHelicopters() throws IOException {
        ContentRegistry registry = ContentRegistry.load(TestResources.assets());
        assertEquals(33, registry.helicopters().size());
        assertNotNull(registry.helicopter("ah-64"));
    }

    private static HelicopterDefinition read(Path file) throws IOException {
        String name = file.getFileName().toString().replace(".txt", "");
        DefinitionReader.Result<HelicopterDefinition> result = DefinitionReader.read(file, new HelicopterDefinition(name));
        assertTrue(result.isValid(), () -> String.valueOf(result.problems()));
        return result.definition();
    }

    private static HelicopterDefinition parse(String name, String text) throws IOException {
        DefinitionReader.Result<HelicopterDefinition> result = parseResult(name, text);
        assertTrue(result.isValid(), () -> String.valueOf(result.problems()));
        return result.definition();
    }

    private static DefinitionReader.Result<HelicopterDefinition> parseResult(String name, String text) throws IOException {
        Path file = Files.createTempFile("mcheli-" + name, ".txt");
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
            return DefinitionReader.read(file, new HelicopterDefinition(name));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
