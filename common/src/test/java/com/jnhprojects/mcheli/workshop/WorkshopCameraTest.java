package com.jnhprojects.mcheli.workshop;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopCameraTest {
    private static final float[] BOX_MIN = {-1, 0, -2};
    private static final float[] BOX_MAX = {1, 2, 2};

    @Test
    void framingPutsTheMiddleOfTheBoxInTheMiddleOfTheView() {
        WorkshopCamera camera = new WorkshopCamera();
        camera.frame(BOX_MIN, BOX_MAX, 200, 100);
        Vector3f middle = camera.project(0, 1, 0);
        assertEquals(0.0F, middle.x, 1.0E-3F);
        assertEquals(0.0F, middle.y, 1.0E-3F);
    }

    @Test
    void framingFitsTheBox() {
        WorkshopCamera camera = new WorkshopCamera();
        camera.frame(BOX_MIN, BOX_MAX, 200, 100);
        for (float x : new float[]{-1, 1}) {
            for (float y : new float[]{0, 2}) {
                for (float z : new float[]{-2, 2}) {
                    Vector3f corner = camera.project(x, y, z);
                    assertTrue(Math.abs(corner.x) <= 100.0F && Math.abs(corner.y) <= 50.0F, corner.toString());
                }
            }
        }
    }

    @Test
    void upOnTheVehicleIsUpOnTheScreen() {
        WorkshopCamera camera = new WorkshopCamera();
        camera.frame(BOX_MIN, BOX_MAX, 200, 100);
        // GUI Y grows downwards.
        assertTrue(camera.project(0, 2, 0).y < camera.project(0, 0, 0).y);
    }

    @Test
    void theStartingViewShowsTheNose() {
        WorkshopCamera camera = new WorkshopCamera();
        camera.frame(BOX_MIN, BOX_MAX, 200, 100);
        // Z grows away from the viewer, and the nose points along +Z.
        assertTrue(camera.project(0, 0, 2).z < camera.project(0, 0, -2).z);
    }

    @Test
    void theProjectionAgreesWithRotationAndTranslation() {
        WorkshopCamera camera = new WorkshopCamera();
        camera.frame(BOX_MIN, BOX_MAX, 200, 100);
        camera.orbit(50, 20);
        camera.pan(10, -4);
        Vector3f projected = camera.project(0.5, 1.5, -1);
        Vector3f drawn = camera.rotation().transform(new Vector3f(0.5F, 1.5F, -1)).add(camera.translation()).mul(camera.scale());
        assertEquals(projected.x, drawn.x, 1.0E-3F);
        assertEquals(projected.y, drawn.y, 1.0E-3F);
    }

    @Test
    void unprojectUndoesTheProjection() {
        WorkshopCamera camera = new WorkshopCamera();
        camera.frame(BOX_MIN, BOX_MAX, 200, 100);
        camera.orbit(33, -12);
        Vector3f before = camera.project(1, 1, 1);
        Vector3f move = camera.unproject(15, -7);
        Vector3f after = camera.project(1 + move.x, 1 + move.y, 1 + move.z);
        assertEquals(before.x + 15, after.x, 1.0E-2F);
        assertEquals(before.y - 7, after.y, 1.0E-2F);
        assertEquals(before.z, after.z, 1.0E-3F);
    }

    @Test
    void panningMovesWhatIsUnderTheMouseWithIt() {
        WorkshopCamera camera = new WorkshopCamera();
        camera.frame(BOX_MIN, BOX_MAX, 200, 100);
        Vector3f before = camera.project(0, 0, 0);
        camera.pan(20, 10);
        Vector3f after = camera.project(0, 0, 0);
        assertEquals(before.x + 20, after.x, 1.0E-2F);
        assertEquals(before.y + 10, after.y, 1.0E-2F);
    }

    @Test
    void lookingUpAndDownStopsAtThePoles() {
        WorkshopCamera camera = new WorkshopCamera();
        camera.orbit(0, 100000);
        Vector3f up = camera.project(0, 1, 0);
        // Nearly straight down onto the vehicle, so its height barely shows on the screen.
        assertTrue(Math.abs(up.y) < camera.scale() * 0.05F);
    }
}
