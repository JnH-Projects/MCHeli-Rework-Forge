package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

/** {@code AddPartCamera}: a sensor turret that follows the pilot's view yaw and, optionally, pitch. */
public record CameraPart(String part, Vector3 position, boolean yawSync, boolean pitchSync) {
}
