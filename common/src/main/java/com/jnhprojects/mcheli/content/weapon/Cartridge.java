package com.jnhprojects.mcheli.content.weapon;

/**
 * {@code SetCartridge = model, acceleration, yaw, pitch, scale, gravity, bound}: the spent case a
 * gun throws out with every shot.
 */
public record Cartridge(String model, float acceleration, float yaw, float pitch, float scale, float gravity, float bound) {
}
