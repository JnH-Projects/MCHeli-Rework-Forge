package com.jnhprojects.mcheli.effect;

/**
 * Not in the original: how strongly tracers, rocket motors and fireballs glow, from the light
 * around them. In daylight the glow is a faint shimmer on top of the model; at night, in a cave or
 * under water it is what you see from far away. Light levels are Minecraft's 0 to 15 scale.
 */
public final class NightGlow {
    /** The glow left at full daylight, so a tracer still catches the eye against a dark hillside. */
    public static final float DAYLIGHT = 0.25F;

    private static final int MAX_LIGHT = 15;

    private NightGlow() {
    }

    /**
     * @param blockLight light from torches, lava and the like at the glowing thing
     * @param skyLight   sky light at the same spot, before the time of day is applied
     * @param skyDarken  how far the sky has dimmed: 0 at noon, 11 at midnight, more in a storm
     * @return the glow strength, {@link #DAYLIGHT} in full light up to 1 in darkness
     */
    public static float strength(int blockLight, int skyLight, int skyDarken) {
        int ambient = Math.clamp(Math.max(blockLight, skyLight - skyDarken), 0, MAX_LIGHT);
        float dark = 1.0F - (float) ambient / MAX_LIGHT;
        // Smoothstep, so dusk fades the glow in rather than switching it on at one light level.
        float eased = dark * dark * (3.0F - 2.0F * dark);
        return DAYLIGHT + (1.0F - DAYLIGHT) * eased;
    }
}
