package com.jnhprojects.mcheli;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Locale;

/**
 * Server rules, the part of the original {@code mcheli.cfg} that decides how the game plays.
 * Forge keeps the file per world in {@code serverconfig/mcheli-server.toml}, sends nothing to
 * clients (only the server applies these rules) and picks up edits while the server runs.
 *
 * <p>The values only exist while a world is loaded. Before that, and on clients, the getters return
 * the defaults, which match 1.0.0.
 */
public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    private static final ForgeConfigSpec.BooleanValue EXPLOSIONS_START_FIRES;
    private static final ForgeConfigSpec.DoubleValue VEHICLE_DAMAGE_FACTOR;
    private static final ForgeConfigSpec.BooleanValue INFINITE_AMMO;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_VEHICLES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PACK_ALLOW_LIST;
    private static final ForgeConfigSpec.IntValue MAX_PACK_SIZE;
    private static final ForgeConfigSpec.IntValue MAX_TOTAL_SIZE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("explosions");
        EXPLOSIONS_BREAK_BLOCKS = builder
                .comment("Whether weapons and crashing vehicles break blocks. The mobGriefing game rule also applies.",
                        "Original: Explosion_DestroyBlock")
                .define("breakBlocks", true);
        EXPLOSIONS_START_FIRES = builder
                .comment("Whether incendiary weapons and crashes set blocks on fire. Original: Explosion_FlamingBlock")
                .define("startFires", true);
        builder.pop();

        builder.push("vehicles");
        VEHICLE_DAMAGE_FACTOR = builder
                .comment("Multiplies the damage every vehicle takes, after armour. 0.5 makes them twice as tough.")
                .defineInRange("damageFactor", 1.0, 0.0, 100.0);
        DISABLED_VEHICLES = builder
                .comment("Vehicles that cannot be placed, by definition name (the file name, such as \"ah-64\").",
                        "Vehicles already in the world stay.")
                .defineListAllowEmpty("disabled", List.of(), o -> o instanceof String);
        builder.pop();

        builder.push("weapons");
        INFINITE_AMMO = builder
                .comment("Whether vehicle weapons never run out of ammunition. Original: InfinityAmmo")
                .define("infiniteAmmo", false);
        builder.pop();

        builder.comment("Content packs from <game dir>/mcheli/packs. Players get the packs from the server when they join.",
                        "Changes apply when the world starts or after /mcheli reload.")
                .push("packs");
        PACK_ALLOW_LIST = builder
                .comment("Pack ids the server loads, such as \"rework-example\". Empty loads every pack in the folder.")
                .defineListAllowEmpty("allowList", List.of(), o -> o instanceof String);
        MAX_PACK_SIZE = builder
                .comment("Largest pack the server loads and sends to players, in megabytes.")
                .defineInRange("maxPackSize", 8, 1, 512);
        MAX_TOTAL_SIZE = builder
                .comment("Most the server loads and sends in all, in megabytes. Packs are taken in id order until",
                        "the next one would go over.")
                .defineInRange("maxTotalSize", 64, 1, 4096);
        builder.pop();

        SPEC = builder.build();
    }

    private ServerConfig() {
    }

    public static boolean explosionsBreakBlocks() {
        return SPEC.isLoaded() ? EXPLOSIONS_BREAK_BLOCKS.get() : EXPLOSIONS_BREAK_BLOCKS.getDefault();
    }

    public static boolean explosionsStartFires() {
        return SPEC.isLoaded() ? EXPLOSIONS_START_FIRES.get() : EXPLOSIONS_START_FIRES.getDefault();
    }

    public static float vehicleDamageFactor() {
        double factor = SPEC.isLoaded() ? VEHICLE_DAMAGE_FACTOR.get() : VEHICLE_DAMAGE_FACTOR.getDefault();
        return (float) factor;
    }

    public static boolean infiniteAmmo() {
        return SPEC.isLoaded() ? INFINITE_AMMO.get() : INFINITE_AMMO.getDefault();
    }

    /** Whether the allow list lets the server load this pack. An empty list allows every pack. */
    public static boolean isPackAllowed(String id) {
        if (!SPEC.isLoaded() || PACK_ALLOW_LIST.get().isEmpty()) {
            return true;
        }
        for (String allowed : PACK_ALLOW_LIST.get()) {
            if (allowed.trim().toLowerCase(Locale.ROOT).equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static long maxPackBytes() {
        return (long) (SPEC.isLoaded() ? MAX_PACK_SIZE.get() : MAX_PACK_SIZE.getDefault()) << 20;
    }

    public static long maxTotalPackBytes() {
        return (long) (SPEC.isLoaded() ? MAX_TOTAL_SIZE.get() : MAX_TOTAL_SIZE.getDefault()) << 20;
    }

    public static boolean isDisabled(String vehicle) {
        if (!SPEC.isLoaded()) {
            return false;
        }
        for (String name : DISABLED_VEHICLES.get()) {
            if (name.trim().toLowerCase(Locale.ROOT).equals(vehicle)) {
                return true;
            }
        }
        return false;
    }
}
