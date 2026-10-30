package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.particle.GlowParticleOptions;
import com.jnhprojects.mcheli.platform.Registered;
import com.jnhprojects.mcheli.platform.Registration;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class ModParticles {
    /** Ignores the particle limit: an explosion that loses its flash to the cap looks broken. */
    public static final Registered<ParticleType<GlowParticleOptions>> GLOW = Registration.register(Registries.PARTICLE_TYPE, "glow",
            key -> new ParticleType<>(true) {
                @Override
                public MapCodec<GlowParticleOptions> codec() {
                    return GlowParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, GlowParticleOptions> streamCodec() {
                    return GlowParticleOptions.STREAM_CODEC;
                }
            });

    private ModParticles() {
    }

    /** Declares the entries; the loader module registers them. */
    public static void init() {
    }
}
