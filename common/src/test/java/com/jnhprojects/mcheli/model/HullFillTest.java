package com.jnhprojects.mcheli.model;

import com.jnhprojects.mcheli.TestResources;
import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.BoundingBoxDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullFillTest {
    /** A fin: a vertical square from y 4 to 8, two blocks long, in the plane x = 0.2. */
    private static MeshGroup fin(String name) {
        float[] p = {
                0.2F, 4.0F, -50.0F, 0.2F, 8.0F, -50.0F, 0.2F, 8.0F, -48.0F,
                0.2F, 4.0F, -50.0F, 0.2F, 8.0F, -48.0F, 0.2F, 4.0F, -48.0F};
        return new MeshGroup(name, p, new float[p.length], new float[p.length / 3 * 2]);
    }

    private static boolean covers(List<BoundingBoxDefinition> boxes, double x, double y, double z) {
        for (BoundingBoxDefinition box : boxes) {
            Vector3 c = box.offset();
            if (Math.abs(x - c.x()) <= box.width() / 2.0 && Math.abs(y - c.y()) <= box.height() / 2.0
                    && Math.abs(z - c.z()) <= box.width() / 2.0) {
                return true;
            }
        }
        return false;
    }

    @Test
    void coversAnUnlistedFinWithColumns() {
        List<BoundingBoxDefinition> boxes = HullFill.fill(List.of(fin("fin")), List.of());
        assertTrue(covers(boxes, 0.2, 6.0, -49.0));
        assertTrue(covers(boxes, 0.2, 7.9, -48.1));
        // Two or three cells along z, each one column tall.
        assertTrue(boxes.size() <= 3, boxes::toString);
        for (BoundingBoxDefinition box : boxes) {
            assertEquals(HullFill.CELL, box.width(), 1.0E-6);
            assertEquals(1.0F, box.damageFactor());
        }
    }

    @Test
    void leavesOutWhatTheDefinitionListsAndWhatSpins() {
        BoundingBoxDefinition listed = new BoundingBoxDefinition(new Vector3(0.0, 6.0, -49.0), 4.0F, 6.0F, 0.5F);
        assertTrue(HullFill.fill(List.of(fin("fin")), List.of(listed)).isEmpty());
        MeshGroup rotor = new MeshGroup("$rotor0", new float[0], new float[0], new float[0]);
        assertTrue(HullFill.fill(List.of(rotor, fin("blade")), List.of()).isEmpty());
        assertFalse(HullFill.fill(List.of(rotor, new MeshGroup("$body", new float[0], new float[0], new float[0]), fin("hull")), List.of()).isEmpty());
    }

    @Test
    void fillsTheMd90TailFin() {
        ContentRegistry content = ContentRegistry.load(TestResources.assets());
        AircraftDefinition md90 = content.any("md90");
        assertNotNull(md90);
        assertFalse(md90.generatedBoxes().isEmpty());
        // The fin stands between the fuselage (top at y 4.5) and the T-tail (y 8.5), around z -47.
        boolean fin = false;
        for (double y = 5.0; y <= 8.0 && !fin; y += 0.5) {
            for (double z = -50.0; z <= -44.0 && !fin; z += 0.5) {
                fin = covers(md90.generatedBoxes(), 0.0, y, z);
            }
        }
        assertTrue(fin, "no generated box in the MD-90 fin");
        assertTrue(md90.hull().size() > md90.boundingBoxes.size());
    }
}
