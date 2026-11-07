package com.jnhprojects.mcheli.physics;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /** The view matrix Forge builds on Minecraft 1.20.6 from what its camera angle event returns. */
    private static Matrix4f forgeView(float[] angles) {
        return new Matrix4f().rotationXYZ((float) Math.toRadians(angles[1]), (float) (Math.toRadians(angles[0]) + Math.PI), (float) Math.toRadians(angles[2]));
    }

    /** The inverse of {@link #camera}: what every other version builds as its view matrix. */
    private static Matrix4f expectedView(float yaw, float pitch, float roll) {
        return new Matrix4f().rotation(camera(yaw, pitch, roll).conjugate());
    }

    private static void assertSameRotation(Matrix4f expected, Matrix4f actual) {
        Vector3f[] axes = {new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(0.0F, 0.0F, 1.0F)};
        for (Vector3f axis : axes) {
            Vector3f a = expected.transformDirection(new Vector3f(axis));
            Vector3f b = actual.transformDirection(new Vector3f(axis));
            assertEquals(a.x, b.x, EPSILON);
            assertEquals(a.y, b.y, EPSILON);
            assertEquals(a.z, b.z, EPSILON);
        }
    }

    @Test
    void rollInXyzOrderBuildsTheSameViewOnEveryHeading() {
        for (float yaw = -180.0F; yaw <= 180.0F; yaw += 15.0F) {
            for (float pitch = -80.0F; pitch <= 80.0F; pitch += 20.0F) {
                for (float roll = -170.0F; roll <= 170.0F; roll += 34.0F) {
                    assertSameRotation(expectedView(yaw, pitch, roll), forgeView(AircraftRotation.rollInXyzOrder(yaw, pitch, roll)));
                }
            }
        }
    }

    @Test
    void rollInXyzOrderSurvivesGimbalLock() {
        // Due east or west and level, a hair of roll leaves the middle angle at its limit.
        for (float yaw : new float[]{-90.0F, 90.0F}) {
            for (float roll : new float[]{0.0F, 1.0E-4F, -1.0E-4F, 1.0F}) {
                assertSameRotation(expectedView(yaw, 0.0F, roll), forgeView(AircraftRotation.rollInXyzOrder(yaw, 0.0F, roll)));
            }
        }
    }

    @Test
    void forgesPlainAnglesWouldBeWrong() {
        // Why rollInXyzOrder exists: going east, the plain angles do not give the same view at all.
        Matrix4f expected = expectedView(-90.0F, 0.0F, -30.0F);
        Matrix4f plain = forgeView(new float[]{-90.0F, 0.0F, -30.0F});
        Vector3f expectedUp = expected.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f plainUp = plain.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F));
        assertTrue(expectedUp.distance(plainUp) > 0.1F);
    }
}
