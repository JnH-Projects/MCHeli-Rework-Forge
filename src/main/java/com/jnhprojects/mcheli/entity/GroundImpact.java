package com.jnhprojects.mcheli.entity;

/**
 * Not in the original: how hard a wall or a long drop hits a tank or a car, as a share of its hit
 * points. Both grow with the square between a harmless and a fatal value, so a gentle bump costs
 * next to nothing and a hard one a lot. The vehicle's armour still applies afterwards.
 */
final class GroundImpact {
    /** Speed into a wall, in blocks per tick, that starts to hurt: about 6 m/s. */
    static final double WALL_SPEED = 0.3;
    /** Speed into a wall that costs every hit point: about 24 m/s. */
    static final double WALL_FATAL_SPEED = 1.2;
    /** A drop in blocks that does no harm. */
    static final double SAFE_DROP = 4.0;
    /** A drop that costs every hit point. */
    static final double FATAL_DROP = 24.0;

    private GroundImpact() {
    }

    static float wall(double speed) {
        return severity(speed, WALL_SPEED, WALL_FATAL_SPEED);
    }

    static float drop(double height) {
        return severity(height, SAFE_DROP, FATAL_DROP);
    }

    /** 0 at {@code harmless}, 1 at {@code fatal}, growing with the square in between. */
    static float severity(double value, double harmless, double fatal) {
        double t = Math.max(value - harmless, 0.0) / (fatal - harmless);
        return (float) Math.min(t * t, 1.0);
    }
}
