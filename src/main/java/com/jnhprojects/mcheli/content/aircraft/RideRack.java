package com.jnhprojects.mcheli.content.aircraft;

/**
 * {@code RideRack = carrier, rackNumber}: this vehicle may ride rack {@code rack} (1 based) of
 * {@code carrier} even if that rack does not list it.
 */
public record RideRack(String carrier, int rack) {
}
