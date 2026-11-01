package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * {@code AddSearchLight}, {@code AddFixedSearchLight} and {@code AddSteeringSearchLight}: a cone of
 * light from {@code position}, fading from {@code colorStart} to {@code colorEnd} (both ARGB) over
 * {@code height} blocks to a disc {@code width} across. A plain search light points where the
 * gunner, or else the pilot, looks; a fixed one points along the vehicle, turned by {@code yaw} and
 * {@code pitch}, and a steering one also turns with the front wheels.
 */
public record SearchLight(Vector3 position, int colorStart, int colorEnd, float height, float width, boolean fixed,
                          float yaw, float pitch, boolean steering, float steeringRotation) {
}
