package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.entity.projectile.FlareEntity;
import com.jnhprojects.mcheli.particle.GlowParticleOptions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Flares and the burning debris of a crash were only their particle trails. Not in the original:
 * a burning flare now glows, which is what makes a flare salvo light up the night sky. Smoke
 * grenades stay invisible.
 */
public class FlareRenderer extends EntityRenderer<FlareEntity, FlareRenderer.State> {
    private static final float RADIUS = 1.4F;

    public FlareRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static class State extends EntityRenderState {
        float glow;
        float flicker = 1.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FlareEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.glow = entity.isSmoke() || entity.isInWater() ? 0.0F : Glow.strength(entity.level(), entity.blockPosition());
        // A cheap waver keyed to the entity, so neighbouring flares do not pulse in step.
        state.flicker = 0.8F + 0.2F * (float) Math.sin((entity.tickCount + partialTicks) * 1.7F + entity.getId());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Glow.halo(pose, collector, camera, Vec3.ZERO, RADIUS * state.flicker, GlowParticleOptions.FIRE, state.glow);
        Glow.halo(pose, collector, camera, Vec3.ZERO, RADIUS * 0.35F, GlowParticleOptions.FLASH, state.glow);
        super.submit(state, pose, collector, camera);
    }
}
