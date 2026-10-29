package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * Extra hit box from {@code BoundingBox = x, y, z, width, height[, damageFactor]}.
 * The box is centred on {@code offset} horizontally and starts {@code height / 2} below it.
 */
public record BoundingBoxDefinition(Vector3 offset, float width, float height, float damageFactor) {
}
