package com.jnhprojects.mcheli.team;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * What this client's team has spotted, {@code MCH_GuiTargetMarker}: the spotted entities, each for
 * as long as it was spotted, and the last marked point. Filled by {@code MarkerPacket} on the
 * client's main thread and drawn by the HUD; a dedicated server never touches it.
 */
public final class Markers {
    /** A marked point stays up this long, unless another replaces it. */
    public static final int POINT_TICKS = 60 * 20;

    private static final Map<Integer, Integer> SPOTTED = new HashMap<>();
    private static @Nullable BlockPos point;
    private static int pointTicks;

    private Markers() {
    }

    public static void spotted(int[] entities, int ticks) {
        for (int id : entities) {
            SPOTTED.merge(id, ticks, Math::max);
        }
    }

    public static void point(BlockPos pos) {
        point = pos;
        pointTicks = POINT_TICKS;
    }

    /** Entity ids and ticks left. */
    public static Map<Integer, Integer> spotted() {
        return SPOTTED;
    }

    public static @Nullable BlockPos point() {
        return point;
    }

    public static void tick() {
        SPOTTED.replaceAll((id, ticks) -> ticks - 1);
        SPOTTED.values().removeIf(ticks -> ticks <= 0);
        if (pointTicks > 0 && --pointTicks == 0) {
            point = null;
        }
    }

    public static void clear() {
        SPOTTED.clear();
        point = null;
        pointTicks = 0;
    }
}
