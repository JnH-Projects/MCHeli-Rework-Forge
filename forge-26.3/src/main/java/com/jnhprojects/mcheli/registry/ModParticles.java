package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.particle.GlowParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, MCHeli.MOD_ID);

    /** Ignores the particle limit: an explosion that loses its flash to the cap looks broken. */
    public static final RegistryObject<ParticleType<GlowParticleOptions>> GLOW = PARTICLE_TYPES.register("glow",
            () -> new ParticleType<>(true) {
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

    public static void register(BusGroup modBus) {
        PARTICLE_TYPES.register(modBus);
    }
}
