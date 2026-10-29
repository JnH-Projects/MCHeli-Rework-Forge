package com.jnhprojects.mcheli;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Per player look and feel, in {@code config/mcheli-client.toml}. Nothing here changes how the
 * game plays, so each player picks for themselves and the server never sees it.
 */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue NIGHT_GLOW;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("effects");
        NIGHT_GLOW = builder
                .comment("Whether tracers, rocket motors, flares and explosions glow. The glow is faint by day",
                        "and strongest at night, in caves and under water.")
                .define("nightGlow", true);
        builder.pop();

        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static boolean nightGlow() {
        return SPEC.isLoaded() ? NIGHT_GLOW.get() : NIGHT_GLOW.getDefault();
    }
}
