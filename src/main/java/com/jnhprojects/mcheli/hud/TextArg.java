package com.jnhprojects.mcheli.hud;

import java.util.Locale;

/**
 * Values a {@code DrawString} line can format, {@code MCH_HudItemStringArgs}. Unlike the numeric
 * variables these keep their own types, so {@code %s} gets the weapon name and {@code %d} an int.
 */
public enum TextArg {
    NONE,
    NAME,
    ALTITUDE,
    DATE,
    MC_THOR,
    MC_TMIN,
    MC_TSEC,
    MAX_HP,
    HP,
    HP_PER,
    POS_X,
    POS_Y,
    POS_Z,
    MOTION_X,
    MOTION_Y,
    MOTION_Z,
    INVENTORY,
    WPN_NAME,
    WPN_AMMO,
    WPN_RM_AMMO,
    RELOAD_PER,
    RELOAD_SEC,
    MORTAR_DIST,
    MC_VER,
    MOD_VER,
    MOD_NAME,
    YAW,
    PITCH,
    ROLL,
    PLYR_YAW,
    PLYR_PITCH,
    TVM_POS_X,
    TVM_POS_Y,
    TVM_POS_Z,
    TVM_DIFF,
    CAM_ZOOM,
    UAV_DIST,
    KEY_GUI,
    THROTTLE;

    /** Unknown names format as {@code null}, as in the original. */
    public static TextArg of(String name) {
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    /** Whether the line is skipped while no weapon is selected. */
    public boolean needsWeapon() {
        return switch (this) {
            case WPN_NAME, WPN_AMMO, WPN_RM_AMMO, RELOAD_PER, RELOAD_SEC, MORTAR_DIST -> true;
            default -> false;
        };
    }

    /** Whether the line is skipped while the selected weapon has no magazine. */
    public boolean needsMagazine() {
        return this == WPN_AMMO || this == WPN_RM_AMMO;
    }
}
