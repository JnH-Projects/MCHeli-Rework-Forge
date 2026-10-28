package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.TestResources;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.WeaponGroup;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.content.weapon.WeaponType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponDefinitionTest {

    @Test
    void parsesM134() throws IOException {
        WeaponDefinition m134 = read("m134");

        assertEquals("M134 Minigun", m134.displayName);
        assertEquals(WeaponType.MACHINE_GUN_1, m134.type);
        assertEquals(8, m134.power);
        assertEquals(8.0F, m134.acceleration, 1.0E-6);
        assertEquals(WeaponDefinition.Sight.ROCKET, m134.sight);
        assertEquals(100, m134.round);
        assertEquals(1600, m134.maxAmmo);
        assertEquals("gun_l2_snd", m134.sound);
        assertEquals(2, m134.muzzleFlashes.size());
        assertEquals(2, m134.muzzleSmokes.size());
        assertEquals(1, m134.muzzleSmokes.getFirst().count());
        assertNotNull(m134.cartridge);
        assertEquals(2.0F, m134.cartridge.scale(), 1.0E-6);
        assertEquals(2, m134.roundItems.size());
        // ExplosionBlock defaults to Explosion.
        assertEquals(0, m134.explosionBlock);
    }

    @Test
    void parsesMissileAndBomb() throws IOException {
        WeaponDefinition aim9 = read("aim9");
        assertEquals(WeaponType.AA_MISSILE, aim9.type);
        assertEquals(WeaponDefinition.Sight.LOCK, aim9.sight);
        assertEquals(10, aim9.lockTime);
        assertEquals("aim9lm", aim9.bulletModel);
        assertEquals(5, aim9.explosionBlock);

        WeaponDefinition mk82 = read("mk82");
        assertEquals(WeaponType.BOMB, mk82.type);
        // Bombs fall under their own gravity unless the file says otherwise.
        assertEquals(-0.03F, mk82.gravity, 1.0E-6);
        assertEquals(4, mk82.round);
    }

    @Test
    void appliesCheckData() throws IOException {
        WeaponDefinition def = parse("bare", "Type = Rocket", "Round = 50", "MaxAmmo = 20", "Explosion = 0", "FAE = true", "ModeNum = 1", "FixMode = 3");

        assertEquals(20, def.round);
        assertFalse(def.fae);
        assertEquals(0, def.fixMode);
        assertEquals("bare_snd", def.sound);
    }

    @Test
    void readsDamageFactorsAndTargets() throws IOException {
        WeaponDefinition shell = read("46cm");
        assertEquals(8.0F, shell.damageFactor("helicopter"), 1.0E-6);
        assertEquals(1.0F, shell.damageFactor("player"), 1.0E-6);

        WeaponDefinition pod = parse("pod", "Type = TargetingPod", "Target = planes, players");
        assertEquals(WeaponDefinition.TARGET_PLANES | WeaponDefinition.TARGET_PLAYERS, pod.target);
    }

    @Test
    void everyShippedWeaponLoadsCleanly() throws IOException {
        Path folder = TestResources.assets().resolve(WeaponDefinition.FOLDER);
        List<String> failures = new ArrayList<>();
        int count = 0;
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".txt")).toList()) {
                String name = file.getFileName().toString().replace(".txt", "");
                DefinitionReader.Result<WeaponDefinition> result = DefinitionReader.read(file, new WeaponDefinition(name));
                if (!result.isValid() || !result.problems().isEmpty() || !result.unhandledKeys().isEmpty()) {
                    failures.add(name + ": " + result.problems() + " unhandled " + result.unhandledKeys());
                }
                count++;
            }
        }
        assertEquals(229, count);
        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void everyVehicleWeaponExists() throws IOException {
        ContentRegistry registry = ContentRegistry.load(TestResources.assets());
        List<String> missing = new ArrayList<>();
        for (AircraftDefinition def : registry.all()) {
            for (WeaponGroup group : def.weaponGroups) {
                if (registry.weapon(group.weaponType()) == null) {
                    missing.add(def.name() + " -> " + group.weaponType());
                }
            }
        }
        assertTrue(missing.isEmpty(), () -> String.join("\n", missing));
        assertEquals(229, registry.weapons().size());
    }

    private static WeaponDefinition read(String name) throws IOException {
        Path file = TestResources.assets().resolve(WeaponDefinition.FOLDER).resolve(name + ".txt");
        DefinitionReader.Result<WeaponDefinition> result = DefinitionReader.read(file, new WeaponDefinition(name));
        assertTrue(result.isValid(), () -> name + " rejected: " + result.problems());
        return result.definition();
    }

    private static WeaponDefinition parse(String name, String... lines) throws IOException {
        Path file = Files.createTempFile("weapon", ".txt");
        try {
            Files.writeString(file, String.join("\n", lines), StandardCharsets.UTF_8);
            DefinitionReader.Result<WeaponDefinition> result = DefinitionReader.read(file, new WeaponDefinition(name));
            assertTrue(result.isValid(), () -> name + " rejected: " + result.problems());
            return result.definition();
        } finally {
            Files.delete(file);
        }
    }
}
