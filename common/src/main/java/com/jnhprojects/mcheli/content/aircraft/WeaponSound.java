package com.jnhprojects.mcheli.content.aircraft;

import org.jspecify.annotations.Nullable;

/**
 * What one weapon sounds like on this vehicle, from {@code WeaponSound = weapon, sound[, pitch]}.
 * Weapon files are shared between vehicles, so a vehicle with its own gun sound says so here rather
 * than in the weapon's file.
 *
 * @param sound the sound's name, as in {@code sounds/<name>.ogg}
 * @param pitch the pitch to play it at, or {@code null} for what the weapon's file says
 */
public record WeaponSound(String sound, @Nullable Float pitch) {
}
