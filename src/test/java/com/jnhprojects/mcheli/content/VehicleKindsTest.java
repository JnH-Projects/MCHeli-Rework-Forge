package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.TestResources;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.RackDefinition;
import com.jnhprojects.mcheli.content.aircraft.TankDefinition;
import com.jnhprojects.mcheli.content.aircraft.VehicleDefinition;
import com.jnhprojects.mcheli.content.aircraft.part.CrawlerTrack;
import com.jnhprojects.mcheli.content.aircraft.part.LandingGearPart;
import com.jnhprojects.mcheli.content.aircraft.part.MovingPart;
import com.jnhprojects.mcheli.content.aircraft.part.VehiclePart;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponPart;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleKindsTest {

    @Test
    void parsesF35bParts() throws IOException {
        PlaneDefinition f35 = read("planes", "f-35b", PlaneDefinition::new);

        assertTrue(f35.vtol);
        assertTrue(f35.ejectionSeat);
        assertEquals(0.2F, f35.vtolPitch, 1.0E-6);
        assertTrue(f35.hasNozzleState());

        // The first canopy is registered under two names.
        assertEquals(List.of("canopy0", "canopy"), f35.canopies.stream().map(MovingPart::part).toList());
        assertEquals(40.0F / 90.0F, f35.canopies.getFirst().factor(), 1.0E-6);

        assertEquals(1, f35.nozzles.size());
        assertEquals(8, f35.propellers.size());
        assertEquals("blade0", f35.propellers.getFirst().blades().getFirst().part());

        LandingGearPart nose = f35.landingGear.getFirst();
        assertEquals("lg0", nose.part());
        assertFalse(nose.reverse());
        assertTrue(f35.landingGear.get(1).reverse());
        assertNotNull(f35.landingGear.get(3).secondAxis());

        assertEquals(6, f35.weaponBays.size());
        assertEquals(List.of("aim-9x", "gbu-31"), f35.weaponBays.getFirst().weapons());
        assertEquals("wb0", f35.weaponBays.getFirst().door().part());

        WeaponPart missile = f35.weaponParts.getFirst();
        assertTrue(missile.missile());
        assertEquals(List.of("gbu-31"), missile.weapons());
    }

    @Test
    void appliesPlaneDefaults() throws IOException {
        PlaneDefinition def = parse("bare", "AddSeat = 0, 1, 0\nSpeed = 5", PlaneDefinition::new);

        assertEquals(1.8F, def.speed, 1.0E-6);
        assertEquals(47.94F, def.rotorSpeed, 1.0E-4);
        assertEquals(0.1F, def.sweepWingSpeed, 1.0E-6);
        assertEquals("plane", def.soundOrDefault());
        assertEquals(List.of("plane", "plane"), def.huds);
        assertFalse(def.hasNozzleState());
    }

    @Test
    void dropsHatchesAndWingsTogether() throws IOException {
        PlaneDefinition def = parse("both", """
                AddSeat = 0, 1, 0
                AddPartHatch = 0, 1, 0, 1, 0, 0
                AddPartWing = 1, 1, 0, 0, 0, 1, 90
                """, PlaneDefinition::new);
        assertTrue(def.hatches.isEmpty());
        assertTrue(def.wings.isEmpty());
    }

    @Test
    void parsesM1a2RunningGear() throws IOException {
        TankDefinition m1 = read("tanks", "m1a2", TankDefinition::new);

        assertEquals(TankDefinition.WeightType.TANK, m1.weightType);
        assertTrue(m1.defaultFreelook);
        assertTrue(m1.enableBack);
        assertEquals(2.0F, m1.onGroundPitchFactor, 1.0E-6);

        assertEquals(5, m1.groundWheels.size());
        // Sorted front to back.
        assertEquals(2.86, m1.groundWheels.getFirst().z(), 1.0E-6);
        assertEquals(-4.14, m1.groundWheels.getLast().z(), 1.0E-6);
        assertEquals(1.82, m1.groundWheels.getFirst().x(), 1.0E-6);

        assertEquals(18, m1.trackRollers.size());
        assertEquals(0, m1.trackRollers.getFirst().side());

        assertEquals(2, m1.crawlerTracks.size());
        CrawlerTrack left = m1.crawlerTracks.get(1);
        assertEquals("crawler_track1", left.part());
        assertEquals(1, left.side());
        assertTrue(left.links().size() > 40, "a full track loop should have many links");

        WeaponPart gun = m1.weaponParts.getFirst();
        assertEquals(List.of("rehinmetall_apfsds", "rehinmetall_he"), gun.weapons());
        assertEquals(2, gun.children().size());
        assertEquals("weapon0_1", gun.children().get(1).part());
        assertTrue(m1.weaponParts.get(1).turret());

        assertEquals(2, m1.rideRacks.size());
        assertEquals("c5", m1.rideRacks.getFirst().carrier());
    }

    @Test
    void tanksGetDefaultWheelsWithoutSetWheelPos() throws IOException {
        TankDefinition def = parse("bare", "AddSeat = 0, 1, 0", TankDefinition::new);
        assertEquals(2, def.groundWheels.size());
        assertEquals("prop", def.soundOrDefault());
        assertEquals(50.0F, def.soundRange, 1.0E-6);
    }

    @Test
    void parsesMk15PartTree() throws IOException {
        VehicleDefinition mk15 = read("vehicles", "mk15", VehicleDefinition::new);

        assertEquals(-80.0F, mk15.minRotationPitch, 1.0E-6);
        assertEquals(25.0F, mk15.maxRotationPitch, 1.0E-6);
        assertEquals(2, mk15.parts.size());
        VehiclePart hull = mk15.parts.get(1);
        assertEquals("part1", hull.part());
        assertTrue(hull.yaw());
        assertTrue(hull.pitch());
        assertEquals(1, hull.children().size());
        assertEquals("part1_0", hull.children().getFirst().part());
        assertEquals(VehiclePart.TYPE_ROTATING_BARREL, hull.children().getFirst().type());
    }

    @Test
    void vehicleAliasesForwardPitchLimits() throws IOException {
        VehicleDefinition def = parse("alias", """
                AddSeat = 0, 1, 0
                RotationPitchMin = -45
                RotationPitchMax = 120
                """, VehicleDefinition::new);
        assertTrue(def.limitRotation);
        assertEquals(-45.0F, def.minRotationPitch, 1.0E-6);
        assertEquals(90.0F, def.maxRotationPitch, 1.0E-6);
    }

    @Test
    void racksFollowCrewSeatsInTheSeatIndex() throws IOException {
        PlaneDefinition def = parse("carrier", """
                AddSeat = 0, 1, 0
                AddSeat = 0, 1, -1
                AddRack = mh-6/plane, 0, 1, -3, 0, 0, -6, 8, 30
                ExclusionSeat = 2, 3
                """, PlaneDefinition::new);

        assertEquals(2, def.seatCount());
        assertEquals(3, def.seatAndRackCount());
        assertNull(def.rack(1));
        RackDefinition rack = def.rack(2);
        assertNotNull(rack);
        assertEquals(1.5, rack.entry().y(), 1.0E-9);
        assertEquals(8.0F, rack.range(), 1.0E-6);
        assertTrue(rack.accepts("mh-6", "helicopter"));
        assertTrue(rack.accepts("a-10", "plane"));
        assertFalse(rack.accepts("m1a2", "tank"));
        assertEquals(1, def.exclusiveSeats.getFirst()[0]);
    }

    @Test
    void keepsOnlyWeaponBaysForCarriedWeapons() throws IOException {
        PlaneDefinition def = parse("bays", """
                AddSeat = 0, 1, 0
                AddWeapon = aim-9x, 0, 0, 0
                AddPartWeaponBay = aim-9x, 1, 1, 1, 0, 0, 1, 80
                AddPartWeaponBay = gbu-31, 1, 1, 1, 0, 0, 1, 80
                """, PlaneDefinition::new);
        assertEquals(1, def.weaponBays.size());
    }

    @Test
    void everyShippedPlaneTankAndVehicleLoadsCleanly() throws IOException {
        assertLoadsCleanly("planes", 51, PlaneDefinition::new);
        assertLoadsCleanly("tanks", 12, TankDefinition::new);
        assertLoadsCleanly("vehicles", 16, VehicleDefinition::new);
    }

    @Test
    void registryLoadsEveryKind() throws IOException {
        ContentRegistry registry = ContentRegistry.load(TestResources.assets());
        assertEquals(51, registry.planes().size());
        assertEquals(12, registry.tanks().size());
        assertEquals(16, registry.vehicles().size());
        assertEquals(33 + 51 + 12 + 16, registry.all().size());
        assertNotNull(registry.any("f-35b"));
        assertNotNull(registry.any("m1a2"));
        assertNotNull(registry.any("mk15"));
        assertNull(registry.any("nonexistent"));
    }

    private static <T extends AircraftDefinition> void assertLoadsCleanly(String folder, int expected, Function<String, T> factory) throws IOException {
        try (Stream<Path> files = Files.list(TestResources.assets().resolve(folder))) {
            List<Path> list = files.filter(p -> p.toString().endsWith(".txt")).toList();
            assertEquals(expected, list.size(), folder);
            for (Path file : list) {
                String name = file.getFileName().toString().replace(".txt", "");
                DefinitionReader.Result<T> result = DefinitionReader.read(file, factory.apply(name));
                assertTrue(result.isValid(), () -> folder + "/" + name + " rejected: " + result.problems());
                assertEquals(List.of(), result.problems(), folder + "/" + name);
            }
        }
    }

    private static <T extends AircraftDefinition> T read(String folder, String name, Function<String, T> factory) throws IOException {
        DefinitionReader.Result<T> result = DefinitionReader.read(TestResources.assets().resolve(folder + "/" + name + ".txt"), factory.apply(name));
        assertTrue(result.isValid(), () -> String.valueOf(result.problems()));
        return result.definition();
    }

    private static <T extends AircraftDefinition> T parse(String name, String text, Function<String, T> factory) throws IOException {
        Path file = Files.createTempFile("mcheli-" + name, ".txt");
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
            DefinitionReader.Result<T> result = DefinitionReader.read(file, factory.apply(name));
            assertTrue(result.isValid(), () -> String.valueOf(result.problems()));
            return result.definition();
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
