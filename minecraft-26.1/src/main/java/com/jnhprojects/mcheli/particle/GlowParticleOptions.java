package com.jnhprojects.mcheli.particle;

import com.jnhprojects.mcheli.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * A soft ball of light blended on top of the scene: the flash of an explosion, a burning wreck, an
 * ember. Spawned on the client only, but the codecs let {@code /particle mcheli:glow} show it too.
 *
 * @param color    RGB of the light
 * @param size     radius in blocks at its largest
 * @param lifetime ticks until it has faded out
 * @param flicker  whether it wavers like a fire rather than fading smoothly like a flash
 */
public record GlowParticleOptions(int color, float size, int lifetime, boolean flicker) implements ParticleOptions {
    /** White hot at the centre of a blast. */
    public static final int FLASH = 0xFFE2B0;
    /** Burning fuel and the fire inside the smoke. */
    public static final int FIRE = 0xFF8A2E;
    /** Embers and the deeper red at the edge of a fireball. */
    public static final int EMBER = 0xFF5A1E;

    public static final MapCodec<GlowParticleOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(GlowParticleOptions::color),
                    ExtraCodecs.POSITIVE_FLOAT.fieldOf("size").forGetter(GlowParticleOptions::size),
                    ExtraCodecs.POSITIVE_INT.fieldOf("lifetime").forGetter(GlowParticleOptions::lifetime),
                    Codec.BOOL.optionalFieldOf("flicker", false).forGetter(GlowParticleOptions::flicker))
            .apply(i, GlowParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GlowParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, GlowParticleOptions::color,
            ByteBufCodecs.FLOAT, GlowParticleOptions::size,
            ByteBufCodecs.VAR_INT, GlowParticleOptions::lifetime,
            ByteBufCodecs.BOOL, GlowParticleOptions::flicker,
            GlowParticleOptions::new);

    public static GlowParticleOptions flash(int color, float size, int lifetime) {
        return new GlowParticleOptions(color, size, lifetime, false);
    }

    public static GlowParticleOptions fire(int color, float size, int lifetime) {
        return new GlowParticleOptions(color, size, lifetime, true);
    }

    @Override
    public ParticleType<GlowParticleOptions> getType() {
        return ModParticles.GLOW.get();
    }
}
