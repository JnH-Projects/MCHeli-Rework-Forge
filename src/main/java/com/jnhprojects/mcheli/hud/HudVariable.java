package com.jnhprojects.mcheli.hud;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The numeric variables HUD expressions can read, the keys {@code MCH_HudItem.updateVarMap} fills
 * in. Names are the lower case enum names.
 */
public enum HudVariable {
    COLOR,
    CENTER_X,
    CENTER_Y,
    WIDTH,
    HEIGHT,
    TIME,
    TEST_MODE,
    PLYR_YAW,
    PLYR_PITCH,
    YAW,
    PITCH,
    ROLL,
    ALTITUDE,
    SEA_ALT,
    HAVE_RADAR,
    RADAR_ROT,
    HP,
    MAX_HP,
    HP_RTO,
    THROTTLE,
    POS_X,
    POS_Y,
    POS_Z,
    MOTION_X,
    MOTION_Y,
    MOTION_Z,
    SPEED,
    FUEL,
    LOW_FUEL,
    STICK_X,
    STICK_Y,
    RELOADING,
    RELOAD_TIME,
    WPN_HEAT,
    IS_HEAT_WPN,
    SIGHT_TYPE,
    LOCK,
    DSP_MT_DIST,
    MT_DIST,
    VTOL_STAT,
    FREE_LOOK,
    GUNNER_MODE,
    CAM_MODE,
    CAM_ZOOM,
    AUTO_PILOT,
    HAVE_FLARE,
    CAN_FLARE,
    INVENTORY,
    HOVERING,
    IS_UAV,
    UAV_FS;

    private static final Map<String, HudVariable> BY_NAME = new HashMap<>();

    static {
        for (HudVariable v : values()) {
            BY_NAME.put(v.key(), v);
        }
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HudVariable byKey(String key) {
        return BY_NAME.get(key);
    }

    /** A set of values for one frame. Unknown names read as 0. */
    public static final class Values implements Expression.Variables {
        private final double[] values = new double[HudVariable.values().length];

        public void set(HudVariable v, double value) {
            values[v.ordinal()] = value;
        }

        public void set(HudVariable v, boolean value) {
            values[v.ordinal()] = value ? 1.0 : 0.0;
        }

        public double get(HudVariable v) {
            return values[v.ordinal()];
        }

        @Override
        public double get(String name) {
            HudVariable v = BY_NAME.get(name);
            return v != null ? values[v.ordinal()] : 0.0;
        }

        public void clear() {
            Arrays.fill(values, 0.0);
        }
    }
}
