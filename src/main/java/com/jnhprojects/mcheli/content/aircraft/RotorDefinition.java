package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * {@code AddRotor = blades, bladeStep, x, y, z, axisX, axisY, axisZ[, foldable]}.
 *
 * <p>The model is expected to contain a part named {@code $blade<index>}. With
 * {@code blades > 1} that part is drawn once per blade, offset by {@code bladeStep} degrees.
 *
 * @param legacyPivot set by {@code AddRotorOld}; translates once around the whole rotor instead of
 *                    pivoting each blade, which a handful of early models rely on
 */
public record RotorDefinition(
        int blades,
        int bladeStep,
        Vector3 position,
        Vector3 axis,
        String part,
        boolean foldable,
        boolean legacyPivot) {
}
