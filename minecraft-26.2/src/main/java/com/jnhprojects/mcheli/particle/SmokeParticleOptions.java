package com.jnhprojects.mcheli.particle;

import com.jnhprojects.mcheli.registry.ModParticles;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * A puff of coloured smoke that swells and pales as it drifts, {@code MCH_EntityParticleSmoke}
 * with {@code toWhite} set. The smoke grenades use it to mark a spot.
 *
 * @param color    RGB the puff starts with
 * @param size     the original's particle size; a puff ends up about {@code size / 5} blocks across
 * @param lifetime ticks the puff lasts, or 0 for the grenade smoke's three to five seconds
 */
public record SmokeParticleOptions(int color, float size, int lifetime) implements ParticleOptions {
    public SmokeParticleOptions(int color, float size) {
        this(color, size, 0);
    }

    public static final MapCodec<SmokeParticleOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(SmokeParticleOptions::color),
                    ExtraCodecs.POSITIVE_FLOAT.fieldOf("size").forGetter(SmokeParticleOptions::size),
                    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("lifetime", 0).forGetter(SmokeParticleOptions::lifetime))
            .apply(i, SmokeParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SmokeParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SmokeParticleOptions::color,
            ByteBufCodecs.FLOAT, SmokeParticleOptions::size,
            ByteBufCodecs.VAR_INT, SmokeParticleOptions::lifetime,
            SmokeParticleOptions::new);

    @Override
    public ParticleType<SmokeParticleOptions> getType() {
        return ModParticles.SMOKE.get();
    }
}
