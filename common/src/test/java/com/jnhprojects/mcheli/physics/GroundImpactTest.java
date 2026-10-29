package com.jnhprojects.mcheli.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The share of hit points a wall impact or a long drop costs a tank or a car. */
class GroundImpactTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void nothingUpToTheHarmlessValue() {
        assertEquals(0.0F, GroundImpact.wall(0.1), EPSILON);
        assertEquals(0.0F, GroundImpact.drop(4.0), EPSILON);
    }

    @Test
    void growsWithTheSquareBetween() {
        // A drop of 10 blocks: (6 / 20)^2.
        assertEquals(0.09F, GroundImpact.drop(10.0), EPSILON);
        // The GT-R into a wall at its top speed of 0.8 blocks per tick: (0.5 / 0.9)^2.
        assertEquals(0.3086F, GroundImpact.wall(0.8), EPSILON);
    }

    @Test
    void neverMoreThanEverything() {
        assertEquals(1.0F, GroundImpact.drop(24.0), EPSILON);
        assertEquals(1.0F, GroundImpact.drop(60.0), EPSILON);
    }
}
