package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * A single firing point from {@code AddWeapon} or {@code AddTurretWeapon}.
 *
 * @param seatIndex zero based seat that controls this weapon
 * @param turret    whether the mount follows the turret rotation instead of the hull
 */
public record WeaponMount(
        Vector3 position,
        float yaw,
        float pitch,
        boolean usableByPilot,
        int seatIndex,
        float defaultYaw,
        float minYaw,
        float maxYaw,
        float minPitch,
        float maxPitch,
        boolean turret) {
}
