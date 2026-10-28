package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * {@code AddPartThrottle}: a lever that rotates by {@code throttle * angle} and slides by
 * {@code throttle * slide}.
 */
public record ThrottlePart(String part, Vector3 position, Vector3 axis, float angle, Vector3 slide) {
}
