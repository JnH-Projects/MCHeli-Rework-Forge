package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.config.ConfigSpec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Server rules, the part of the original {@code mcheli.cfg} that decides how the game plays.
 * The file lives per world in {@code serverconfig/mcheli-server.toml}, where Forge kept it, and is
 * never sent to clients (only the server applies these rules). It is read when the world starts
 * and again on {@code /mcheli reload}.
 *
 * <p>The values only exist while a world is loaded. Before that, and on clients, the getters return
 * the defaults, which match 1.0.0.
 */
public final class ServerConfig {
    public static final ConfigSpec SPEC;

    private static final ConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    private static final ConfigSpec.BooleanValue EXPLOSIONS_START_FIRES;
    private static final ConfigSpec.DoubleValue VEHICLE_DAMAGE_FACTOR;
    private static final ConfigSpec.BooleanValue INFINITE_AMMO;
    private static final ConfigSpec.BooleanValue INFINITE_FUEL;
    private static final ConfigSpec.StringListValue DISABLED_VEHICLES;
    private static final ConfigSpec.StringListValue PACK_ALLOW_LIST;
    private static final ConfigSpec.IntValue MAX_PACK_SIZE;
    private static final ConfigSpec.IntValue MAX_TOTAL_SIZE;

    static {
        ConfigSpec.Builder builder = ConfigSpec.builder();

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
                .defineList("disabled", List.of());
        INFINITE_FUEL = builder
                .comment("Whether vehicles never run out of fuel. Original: InfinityFuel")
                .define("infiniteFuel", false);
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
                .defineList("allowList", List.of());
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

    /** Reads the world's file, writing it with the defaults if it is new. */
    public static void load(Path worldDir) {
        Path file = worldDir.resolve("serverconfig").resolve(MCHeli.MOD_ID + "-server.toml");
        try {
            SPEC.load(file);
        } catch (IOException e) {
            MCHeli.LOGGER.warn("Could not read {}; using the defaults", file, e);
        }
    }

    /** The world closed: back to the defaults. */
    public static void unload() {
        SPEC.unload();
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

    public static boolean infiniteFuel() {
        return SPEC.isLoaded() ? INFINITE_FUEL.get() : INFINITE_FUEL.getDefault();
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
