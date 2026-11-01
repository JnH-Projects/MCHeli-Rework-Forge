package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * {@code AddRepellingHook}: where a rope hangs from a hovering helicopter, and how many ticks pass
 * between two passengers sliding down it.
 */
public record RepellingHook(Vector3 position, int interval) {
}
