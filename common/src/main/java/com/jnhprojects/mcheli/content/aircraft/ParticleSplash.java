package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * {@code AddParticleSplash}: spray thrown up from {@code position} while it moves through water,
 * {@code count} puffs a tick of {@code size}, pushed back by the speed times {@code acceleration}.
 */
public record ParticleSplash(Vector3 position, int count, float size, float acceleration, int age, float motionY, float gravity) {
}
