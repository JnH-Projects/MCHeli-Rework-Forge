package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * {@code AddPartRotation}: a part that spins at a constant {@code speed} in degrees per tick, for
 * radar dishes and similar.
 *
 * @param always spin even when nobody is aboard
 */
public record RotatingPart(String part, Vector3 position, Vector3 axis, float speed, boolean always) {
}
