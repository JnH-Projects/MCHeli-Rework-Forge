package com.jnhprojects.mcheli.model;

import com.jnhprojects.mcheli.TestResources;
import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.VehicleDefinition;
import com.jnhprojects.mcheli.content.aircraft.part.MovingPart;
import com.jnhprojects.mcheli.content.aircraft.part.PropellerPart;
import com.jnhprojects.mcheli.content.aircraft.part.VehiclePart;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponPart;
import com.jnhprojects.mcheli.content.aircraft.part.WingPart;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the part names generated from the definitions ({@code hatch0}, {@code lg3},
 * {@code weapon2_1}, ...) resolve in the shipped models, so the numbering matches the original.
 */
class PartModelTest {

    /**
     * References that point at nothing in the original 1.0.3 content as well. They render nothing
     * there and here; the files are left as shipped.
     */
    private static final Map<String, List<String>> ORIGINAL_GAPS = Map.of(
            "helicopters/uh-1c", List.of("camera0"),
            "planes/f4a", List.of("lg9", "lg10"),
            "planes/su-47", List.of("weapon2", "weapon3"));

    @Test
    void everyModelLoadsAndHasABody() throws IOException {
        Path assets = TestResources.assets();
        for (AircraftDefinition def : ContentRegistry.load(assets).all()) {
            Mesh mesh = load(assets, def);
            assertFalse(mesh.body().isEmpty(), def.name());
            assertTrue(mesh.triangleCount() > 0, def.name());
        }
    }

    @Test
    void generatedPartNamesResolveInTheModels() throws IOException {
        Path assets = TestResources.assets();
        Map<String, List<String>> missing = new TreeMap<>();
        int checked = 0;
        for (AircraftDefinition def : ContentRegistry.load(assets).all()) {
            Mesh mesh = load(assets, def);
            for (String part : partNames(def)) {
                checked++;
                if (mesh.part("$" + part).isEmpty()) {
                    missing.computeIfAbsent(def.folder() + "/" + def.name(), k -> new ArrayList<>()).add(part);
                }
            }
        }
        assertTrue(checked > 900, "expected to check every part of every vehicle, got " + checked);
        assertEquals(ORIGINAL_GAPS, missing);
    }

    /** Every part name a definition makes the renderer look up. */
    private static List<String> partNames(AircraftDefinition def) {
        List<String> names = new ArrayList<>();
        def.hatches.forEach(p -> names.add(p.part()));
        def.weaponBays.forEach(p -> names.add(p.door().part()));
        def.landingGear.forEach(p -> names.add(p.part()));
        def.throttleParts.forEach(p -> names.add(p.part()));
        def.rotatingParts.forEach(p -> names.add(p.part()));
        def.cameraParts.forEach(p -> names.add(p.part()));
        def.wheels.forEach(p -> names.add(p.part()));
        def.steeringWheels.forEach(p -> names.add(p.part()));
        def.trackRollers.forEach(p -> names.add(p.part()));
        def.crawlerTracks.forEach(p -> names.add(p.part()));
        for (WeaponPart w : def.weaponParts) {
            names.add(w.part());
            w.children().forEach(c -> names.add(c.part()));
        }
        if (def instanceof PlaneDefinition plane) {
            // Nozzles and rotor housings are optional: most planes only animate the blades.
            for (WingPart wing : plane.wings) {
                names.add(wing.part());
                wing.pylons().forEach(p -> names.add(p.part()));
            }
            for (PropellerPart rotor : plane.propellers) {
                rotor.blades().forEach(b -> names.add(b.part()));
            }
        }
        if (def instanceof VehicleDefinition vehicle) {
            vehicle.parts.forEach(p -> addVehiclePart(p, names));
        }
        // The first canopy is listed as both canopy0 and canopy; a model carries one of the two.
        for (MovingPart canopy : def.canopies) {
            if (!canopy.part().equals("canopy") && !canopy.part().equals("canopy0")) {
                names.add(canopy.part());
            }
        }
        return names;
    }

    private static void addVehiclePart(VehiclePart part, List<String> names) {
        names.add(part.part());
        part.children().forEach(c -> addVehiclePart(c, names));
    }

    private static Mesh load(Path assets, AircraftDefinition def) throws IOException {
        Path model = assets.resolve("models/" + def.folder() + "/" + def.name() + ".mqo");
        if (!Files.exists(model)) {
            model = assets.resolve("models/" + def.folder() + "/" + def.name() + ".obj");
        }
        assertTrue(Files.exists(model), () -> "No model for " + def.folder() + "/" + def.name());
        try (InputStream in = Files.newInputStream(model)) {
            return MeshLoader.load(model.getFileName().toString(), in);
        }
    }
}
