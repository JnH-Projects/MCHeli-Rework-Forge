package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.TankDefinition;
import com.jnhprojects.mcheli.content.pack.ContentSource;
import com.jnhprojects.mcheli.content.pack.PackProblem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adding, replacing and reloading definitions while the game runs. Needs none of the original content. */
class ContentRegistryTest {

    @Test
    void addsAndReplacesByName() {
        ContentRegistry registry = ContentRegistry.empty();
        HelicopterDefinition first = new HelicopterDefinition("scout");
        registry.add(first);
        assertSame(first, registry.helicopter("scout"));

        HelicopterDefinition second = new HelicopterDefinition("scout");
        registry.add(second);
        assertSame(second, registry.any("scout"));
        assertEquals(1, registry.helicopters().size());
    }

    @Test
    void aDefinitionThatChangesKindLeavesItsOldKind() {
        ContentRegistry registry = ContentRegistry.empty();
        registry.add(new HelicopterDefinition("mule"));
        registry.add(new TankDefinition("mule"));

        assertNull(registry.helicopter("mule"));
        assertTrue(registry.any("mule") instanceof TankDefinition);
        assertEquals(1, registry.all().size());
    }

    @Test
    void everyChangeBumpsTheGeneration() {
        ContentRegistry registry = ContentRegistry.empty();
        int start = registry.generation();
        registry.add(new HelicopterDefinition("scout"));
        int afterAdd = registry.generation();
        assertNotEquals(start, afterAdd);

        assertTrue(registry.remove("scout"));
        assertNotEquals(afterAdd, registry.generation());
        assertNull(registry.any("scout"));

        int beforeMiss = registry.generation();
        assertFalse(registry.remove("scout"));
        assertEquals(beforeMiss, registry.generation());
    }

    @Test
    void replaceWithSwapsEverythingAtOnce(@TempDir Path dir) throws IOException {
        Path helicopters = Files.createDirectories(dir.resolve(HelicopterDefinition.FOLDER));
        Files.writeString(helicopters.resolve("Trainer.txt"), "DisplayName = Trainer\nAddSeat = 0, 1, 0\n");

        ContentRegistry registry = ContentRegistry.empty();
        registry.add(new HelicopterDefinition("scout"));
        int before = registry.generation();

        registry.replaceWith(ContentRegistry.load(dir));

        assertNotEquals(before, registry.generation());
        assertNull(registry.any("scout"));
        assertEquals(List.of("trainer"), registry.all().stream().map(d -> d.name()).toList());
    }

    @Test
    void readersKeepTheirViewDuringAChange() {
        ContentRegistry registry = ContentRegistry.empty();
        registry.add(new HelicopterDefinition("a"));
        var view = registry.helicopters();
        registry.add(new HelicopterDefinition("b"));

        assertEquals(1, view.size());
        assertEquals(2, registry.helicopters().size());
    }

    @Test
    void aLaterSourceReplacesADefinitionOfAnyKind(@TempDir Path dir) throws IOException {
        Path first = dir.resolve("first");
        Path second = dir.resolve("second");
        write(first.resolve(HelicopterDefinition.FOLDER).resolve("mule.txt"), "DisplayName = Mule\nAddSeat = 0, 1, 0\n");
        write(first.resolve(HelicopterDefinition.FOLDER).resolve("scout.txt"), "DisplayName = Scout\nAddSeat = 0, 1, 0\n");
        write(second.resolve(TankDefinition.FOLDER).resolve("mule.txt"), "DisplayName = Mule tank\nAddSeat = 0, 1, 0\n");

        ContentRegistry registry = ContentRegistry.load(List.of(
                new ContentSource(ContentSource.ORIGINAL, first), new ContentSource("tanks", second)));

        assertNull(registry.helicopter("mule"));
        assertEquals("tanks", registry.tank("mule").pack());
        assertEquals(ContentSource.ORIGINAL, registry.helicopter("scout").pack());
        assertTrue(registry.problems().isEmpty());
    }

    @Test
    void problemsInAPackAreReportedButTheRestLoads(@TempDir Path dir) throws IOException {
        Path pack = dir.resolve("pack");
        write(pack.resolve(HelicopterDefinition.FOLDER).resolve("good.txt"),
                "DisplayName = Good\nAddSeat = 0, 1, 0\nAddWeapon = no_such_gun, 0, 0, 0\n");
        write(pack.resolve(HelicopterDefinition.FOLDER).resolve("bad.txt"), "DisplayName = Bad\nMaxHp = lots\n");

        ContentRegistry registry = ContentRegistry.load(List.of(new ContentSource("mine", pack)));

        assertEquals("mine", registry.helicopter("good").pack());
        List<String> messages = registry.problems().stream().map(PackProblem::toString).toList();
        assertTrue(messages.stream().anyMatch(m -> m.startsWith("mine: helicopters/bad.txt: line 2 (maxhp)")), messages::toString);
        assertTrue(messages.stream().anyMatch(m -> m.contains("helicopters/good: unknown weapons [no_such_gun]")), messages::toString);
    }

    @Test
    void aMissingFolderLoadsNothing(@TempDir Path dir) {
        ContentRegistry registry = ContentRegistry.load(List.of(new ContentSource("gone", dir.resolve("gone"))));
        assertTrue(registry.all().isEmpty());
        assertTrue(registry.problems().isEmpty());
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}
