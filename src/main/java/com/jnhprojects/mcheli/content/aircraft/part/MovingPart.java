package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * A model part that swings or slides open, driven by a 0 to 90 degree part state.
 *
 * <p>Covers hatches, canopies, weapon bays, nozzles and pylons. A swinging part rotates
 * {@code state * factor} degrees around {@code axis} through {@code position}. A sliding part
 * ignores the axis and moves by {@code position * state / 90} instead, which is how the original
 * {@code AddPartSlide*} keys reuse the position fields as the slide offset.
 *
 * @param part   model part name without the leading {@code $}
 * @param factor maximum angle divided by 90
 */
public record MovingPart(String part, Vector3 position, Vector3 axis, float factor, boolean slide) {
}
