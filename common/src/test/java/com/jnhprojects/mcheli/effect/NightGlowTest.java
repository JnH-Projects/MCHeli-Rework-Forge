package com.jnhprojects.mcheli.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NightGlowTest {
    private static final float EPSILON = 1.0E-6F;

    @Test
    void middayLeavesOnlyTheDaylightShimmer() {
        assertEquals(NightGlow.DAYLIGHT, NightGlow.strength(0, 15, 0), EPSILON);
    }

    @Test
    void pitchDarkGlowsInFull() {
        assertEquals(1.0F, NightGlow.strength(0, 0, 0), EPSILON);
    }

    @Test
    void midnightGlowsFarMoreThanNoon() {
        float midnight = NightGlow.strength(0, 15, 11);
        assertTrue(midnight > 0.8F, "midnight glow " + midnight);
    }

    @Test
    void aTorchNearbyDimsTheGlowAtNight() {
        assertTrue(NightGlow.strength(14, 15, 11) < NightGlow.strength(0, 15, 11));
    }

    @Test
    void dimmingIsGradualAndNeverGoesBackUp() {
        float previous = 0.0F;
        for (int darken = 0; darken <= 15; darken++) {
            float s = NightGlow.strength(0, 15, darken);
            assertTrue(s >= previous, "glow dropped at sky darken " + darken);
            assertTrue(s - previous < 0.2F || darken == 0, "glow jumped at sky darken " + darken);
            previous = s;
        }
    }

    @Test
    void outOfRangeLightIsClamped() {
        assertEquals(NightGlow.DAYLIGHT, NightGlow.strength(40, 40, 0), EPSILON);
        assertEquals(1.0F, NightGlow.strength(-3, 0, 20), EPSILON);
    }
}
