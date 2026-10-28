package com.jnhprojects.mcheli.content.weapon;

/**
 * One {@code AddMuzzleFlash} or {@code AddMuzzleFlashSmoke} line.
 *
 * @param distance how far ahead of the muzzle the puff appears
 * @param range    spread of the smoke puffs, zero for flashes
 * @param age      lifetime in ticks
 * @param count    puffs per shot
 */
public record MuzzleFlash(float distance, float size, float range, int age, WeaponColor color, int count) {
}
