package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * One passenger position. Seat zero is always the pilot.
 *
 * @param camera        camera override for gunner seats, {@code null} to use the aircraft camera
 * @param switchGunner  whether the occupant can toggle between gunner and passenger views
 * @param fixedRotation whether the occupant's view is locked to {@code fixedYaw}/{@code fixedPitch}
 * @param rotatingSeat  whether the seat model turns with the occupant's view
 */
public record SeatDefinition(
        Vector3 position,
        boolean gunner,
        CameraPosition camera,
        boolean switchGunner,
        boolean fixedRotation,
        float fixedYaw,
        float fixedPitch,
        float minPitch,
        float maxPitch,
        boolean rotatingSeat) {

    static final float DEFAULT_MIN_PITCH = -30.0F;
    static final float DEFAULT_MAX_PITCH = 70.0F;

    static SeatDefinition passenger(Vector3 position, boolean rotatingSeat) {
        return new SeatDefinition(position, false, null, false, false, 0, 0, DEFAULT_MIN_PITCH, DEFAULT_MAX_PITCH, rotatingSeat);
    }
}
