package com.jnhprojects.mcheli.content.weapon;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The {@code Type} key of a weapon file. Each value is one of the {@code MCH_Weapon*} classes the
 * original created in {@code MCH_WeaponCreator}.
 */
public enum WeaponType {
    MACHINE_GUN_1("machinegun1", "MachineGun"),
    MACHINE_GUN_2("machinegun2", "MachineGun"),
    TV_MISSILE("tvmissile", "TV Missile"),
    TORPEDO("torpedo", "Torpedo"),
    CAS("cas", "CAS"),
    ROCKET("rocket", "Rocket"),
    AS_MISSILE("asmissile", "AS Missile"),
    AA_MISSILE("aamissile", "AA Missile"),
    AT_MISSILE("atmissile", "AT Missile"),
    BOMB("bomb", "Bomb"),
    MARKER_ROCKET("mkrocket", "Mk Rocket"),
    DUMMY("dummy", "Dummy"),
    SMOKE("smoke", "Smoke"),
    DISPENSER("dispenser", "Dispenser"),
    TARGETING_POD("targetingpod", "Targeting Pod");

    private final String key;
    private final String displayName;

    WeaponType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    /** The lower case name used in the files. */
    public String key() {
        return key;
    }

    /** {@code getWeaponTypeName}: the label the original showed next to a weapon. */
    public String displayName() {
        return displayName;
    }

    /** Missiles that need a locked target before they can be fired. */
    public boolean seeksEntity() {
        return this == AA_MISSILE || this == AT_MISSILE;
    }

    public static @Nullable WeaponType of(String key) {
        String k = key.trim().toLowerCase(Locale.ROOT);
        for (WeaponType type : values()) {
            if (type.key.equals(k)) {
                return type;
            }
        }
        return null;
    }
}
