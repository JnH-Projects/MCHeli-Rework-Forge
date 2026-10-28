package com.jnhprojects.mcheli.content.aircraft;

import java.util.List;

/**
 * Consecutive {@code AddWeapon} lines with the same weapon type form one selectable group.
 * That matches how the original cycled weapons: one entry per group, fired from every mount.
 */
public record WeaponGroup(String weaponType, List<WeaponMount> mounts) {
}
