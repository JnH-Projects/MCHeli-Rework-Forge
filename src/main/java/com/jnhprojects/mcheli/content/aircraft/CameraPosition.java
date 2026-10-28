package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

/** A third-person or gunner camera anchor, relative to the aircraft origin. */
public record CameraPosition(Vector3 position, boolean fixedRotation, float yaw, float pitch) {
    public static final CameraPosition DEFAULT = new CameraPosition(Vector3.ZERO, false, 0, 0);

    public CameraPosition(Vector3 position) {
        this(position, false, 0, 0);
    }
}
