package com.jnhprojects.mcheli.physics;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the camera tilt against the hull. The camera rotation is rebuilt the way
 * {@code Camera.setRotation(yRot, xRot, z)} builds it in Minecraft 26.3, so a sign mix-up between
 * the two conventions shows up here rather than as a horizon tilting the wrong way in game.
 */
class AircraftRotationTest {
    private static final float EPSILON = 1.0E-3F;

    private static Quaternionf camera(float yaw, float pitch, float roll) {
        return new Quaternionf().rotationYXZ(
                (float) (Math.PI - Math.toRadians(yaw)), (float) -Math.toRadians(pitch), (float) Math.toRadians(roll));
    }

    private static Vector3f up(Quaternionf q) {
        return q.transform(new Vector3f(0.0F, 1.0F, 0.0F));
    }

    @Test
    void lookingDownTheNoseFollowsTheHullExactly() {
        float yaw = 37.0F;
        float pitch = -12.0F;
        float roll = 28.0F;
        float cameraRoll = AircraftRotation.cameraRoll(yaw, pitch, roll, yaw, pitch);
        assertEquals(-roll, cameraRoll, EPSILON);

        Vector3f hullUp = AircraftRotation.orientation(yaw, pitch, roll).transform(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f cameraUp = up(camera(yaw, pitch, cameraRoll));
        assertEquals(hullUp.x, cameraUp.x, EPSILON);
        assertEquals(hullUp.y, cameraUp.y, EPSILON);
        assertEquals(hullUp.z, cameraUp.z, EPSILON);
    }

    @Test
    void levelHullLeavesTheViewLevel() {
        assertEquals(0.0F, AircraftRotation.cameraRoll(120.0F, 0.0F, 0.0F, 300.0F, 35.0F), EPSILON);
    }

    @Test
    void lookingSidewaysTurnsHullPitchIntoRoll() {
        float pitch = 20.0F;
        float sideways = AircraftRotation.cameraRoll(0.0F, pitch, 0.0F, 90.0F, 0.0F);
        assertEquals(pitch, Math.abs(sideways), EPSILON);
        // The other side tilts the other way.
        assertEquals(-sideways, AircraftRotation.cameraRoll(0.0F, pitch, 0.0F, -90.0F, 0.0F), EPSILON);
    }

    @Test
    void cameraUpStaysOnTheHullsSideOfTheView() {
        float yaw = -64.0F;
        float pitch = 15.0F;
        float roll = -40.0F;
        float viewYaw = 10.0F;
        float viewPitch = 25.0F;
        Vector3f hullUp = AircraftRotation.orientation(yaw, pitch, roll).transform(new Vector3f(0.0F, 1.0F, 0.0F));
        Quaternionf view = camera(viewYaw, viewPitch, AircraftRotation.cameraRoll(yaw, pitch, roll, viewYaw, viewPitch));
        Vector3f forward = view.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f expected = new Vector3f(hullUp).fma(-hullUp.dot(forward), forward).normalize();
        Vector3f cameraUp = up(view);
        assertEquals(expected.x, cameraUp.x, EPSILON);
        assertEquals(expected.y, cameraUp.y, EPSILON);
        assertEquals(expected.z, cameraUp.z, EPSILON);
    }
}
