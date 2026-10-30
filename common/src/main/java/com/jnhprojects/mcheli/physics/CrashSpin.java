package com.jnhprojects.mcheli.physics;

/**
 * Not in the original: how a shot down aircraft goes in. A plane or jet rolls into a nose-down
 * spiral, and a helicopter that lost its tail rotor spins up with some wobble. The original only
 * nudged pitch and roll by a random amount picked on the server. The direction here comes from
 * the entity id, so the server and every client agree on it without syncing anything.
 *
 * <p>All rates are in degrees per tick; {@code ticks} counts from the moment of destruction.
 */
public final class CrashSpin {
    /** How far the nose drops in a plane's spiral. */
    public static final float PLANE_DIVE_PITCH = 55.0F;
    private static final float PLANE_PITCH_RATE = 1.2F;
    private static final float PLANE_MAX_ROLL_RATE = 9.0F;
    private static final float PLANE_YAW_RATE = 2.0F;

    private static final float HELICOPTER_MAX_YAW_RATE = 24.0F;
    private static final float HELICOPTER_WOBBLE_PITCH = 7.0F;
    private static final float HELICOPTER_WOBBLE_ROLL = 10.0F;

    private CrashSpin() {
    }

    /** 1 for a spin to the right, -1 to the left; the same for an entity id everywhere. */
    public static int direction(int entityId) {
        // Mix the bits so neighbouring ids, spawned one after the other, do not all spin alike.
        int h = entityId * 0x9E3779B9;
        h ^= h >>> 16;
        return (h & 1) == 0 ? 1 : -1;
    }

    /** A plane's roll rate: it builds up over the first two seconds of the fall. */
    public static float planeRollRate(int ticks, int direction) {
        return direction * Math.min(1.0F + ticks * 0.2F, PLANE_MAX_ROLL_RATE);
    }

    /** A plane's yaw rate, turning the roll into a spiral rather than a barrel roll in place. */
    public static float planeYawRate(int ticks, int direction) {
        return direction * Math.min(ticks * 0.05F, PLANE_YAW_RATE);
    }

    /** The plane's next pitch, easing towards the dive. Positive pitch is nose down. */
    public static float planePitch(float pitch) {
        if (pitch < PLANE_DIVE_PITCH) {
            return Math.min(pitch + PLANE_PITCH_RATE, PLANE_DIVE_PITCH);
        }
        return Math.max(pitch - PLANE_PITCH_RATE, PLANE_DIVE_PITCH);
    }

    /** A helicopter's yaw rate: faster than the original's 15, reached in about two seconds. */
    public static float helicopterYawRate(int ticks, int direction) {
        return direction * Math.min(2.0F + ticks * 0.6F, HELICOPTER_MAX_YAW_RATE);
    }

    /**
     * The pitch the helicopter wobbles around at this tick. The phase comes from the id, so two
     * helicopters going down together do not nod in step.
     */
    public static float helicopterWobblePitch(int ticks, int entityId) {
        return HELICOPTER_WOBBLE_PITCH * (float) Math.sin(ticks * 0.35 + phase(entityId));
    }

    /** The roll the helicopter wobbles around at this tick. */
    public static float helicopterWobbleRoll(int ticks, int entityId) {
        return HELICOPTER_WOBBLE_ROLL * (float) Math.cos(ticks * 0.27 + phase(entityId));
    }

    private static double phase(int entityId) {
        return (entityId & 0xFF) * (Math.PI * 2.0 / 256.0);
    }
}
