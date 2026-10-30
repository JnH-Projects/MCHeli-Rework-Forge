package com.jnhprojects.mcheli.physics;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrashSpinTest {
    @Test
    void directionIsStableAndBothWaysOccur() {
        Set<Integer> seen = new HashSet<>();
        for (int id = 0; id < 64; id++) {
            int direction = CrashSpin.direction(id);
            assertEquals(direction, CrashSpin.direction(id));
            assertTrue(direction == 1 || direction == -1);
            seen.add(direction);
        }
        assertEquals(Set.of(1, -1), seen);
    }

    @Test
    void planeRollBuildsUpAndLevelsOff() {
        assertTrue(Math.abs(CrashSpin.planeRollRate(0, 1)) < Math.abs(CrashSpin.planeRollRate(20, 1)));
        assertEquals(CrashSpin.planeRollRate(200, 1), CrashSpin.planeRollRate(400, 1));
        assertEquals(-CrashSpin.planeRollRate(30, 1), CrashSpin.planeRollRate(30, -1));
    }

    @Test
    void planeNoseDropsToTheDiveAndStays() {
        float pitch = -10.0F;
        for (int i = 0; i < 200; i++) {
            pitch = CrashSpin.planePitch(pitch);
        }
        assertEquals(CrashSpin.PLANE_DIVE_PITCH, pitch);
        assertTrue(CrashSpin.planePitch(80.0F) < 80.0F);
    }

    @Test
    void helicopterSpinsFasterThanTheOriginal() {
        assertTrue(Math.abs(CrashSpin.helicopterYawRate(100, 1)) > 15.0F);
        assertTrue(Math.abs(CrashSpin.helicopterWobblePitch(5, 7)) <= 7.0F);
        assertTrue(Math.abs(CrashSpin.helicopterWobbleRoll(5, 7)) <= 10.0F);
    }
}
