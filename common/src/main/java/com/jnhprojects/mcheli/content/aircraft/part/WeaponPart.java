package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code AddPartWeapon}, {@code AddPartRotWeapon}, {@code AddPartTurretWeapon},
 * {@code AddPartTurretRotWeapon} and {@code AddPartWeaponMissile}, plus their
 * {@code AddPartWeaponChild} lines.
 *
 * <p>The part is drawn turned towards where its weapon aims: yaw, pitch or both. Until weapons
 * exist the aim is the view of the crew member operating the part, which is the fallback the
 * original used for a part without a weapon.
 *
 * @param weapons       weapon names this part belongs to, the first one decides the aim
 * @param rotatingBarrel spins around {@code axis} while firing (gatling guns)
 * @param missile       hidden while its weapon is reloading
 * @param hideInFirstPerson hidden for the crew member using it in first person
 * @param recoil        how far the part kicks back along Z when firing
 * @param turret        additionally turns with the turret around {@code TurretPosition}
 */
public record WeaponPart(
        String part,
        List<String> weapons,
        boolean rotatingBarrel,
        boolean missile,
        boolean hideInFirstPerson,
        boolean yaw,
        boolean pitch,
        Vector3 position,
        Vector3 axis,
        float recoil,
        boolean turret,
        List<Child> children) {

    public WeaponPart {
        children = new ArrayList<>(children);
    }

    /** A sub part that follows its parent and can add its own yaw or pitch. */
    public record Child(String part, boolean yaw, boolean pitch, Vector3 position, float recoil) {
    }
}
