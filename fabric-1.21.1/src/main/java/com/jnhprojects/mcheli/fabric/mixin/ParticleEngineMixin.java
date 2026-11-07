package com.jnhprojects.mcheli.fabric.mixin;

import com.google.common.collect.ImmutableList;
import com.jnhprojects.mcheli.client.render.Glow;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Vanilla only draws the particle render types on its own list, which leaves our glow and smoke
 * particles invisible. Forge and NeoForge draw every render type in use.
 */
@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
    @Shadow
    @Final
    @Mutable
    private static List<ParticleRenderType> RENDER_ORDER;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void mcheli$addRenderTypes(CallbackInfo ci) {
        RENDER_ORDER = ImmutableList.<ParticleRenderType>builder().addAll(RENDER_ORDER).add(Glow.PARTICLE_LAYER, Glow.SMOKE_LAYER).build();
    }
}
