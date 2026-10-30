package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.entity.projectile.FlareEntity;
import com.jnhprojects.mcheli.particle.GlowParticleOptions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Flares and the burning debris of a crash were only their particle trails. Not in the original:
 * a burning flare now glows, which is what makes a flare salvo light up the night sky. Smoke
 * grenades stay invisible.
 */
public class FlareRenderer extends EntityRenderer<FlareEntity> {
    private static final float RADIUS = 1.4F;

    public FlareRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(FlareEntity entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        float glow = entity.isSmoke() || entity.isInWater() ? 0.0F : Glow.strength(entity.level(), entity.blockPosition());
        // A cheap waver keyed to the entity, so neighbouring flares do not pulse in step.
        float flicker = 0.8F + 0.2F * (float) Math.sin((entity.tickCount + partialTicks) * 1.7F + entity.getId());
        Quaternionf camera = entityRenderDispatcher.cameraOrientation();
        Glow.halo(pose, buffers, camera, Vec3.ZERO, RADIUS * flicker, GlowParticleOptions.FIRE, glow);
        Glow.halo(pose, buffers, camera, Vec3.ZERO, RADIUS * 0.35F, GlowParticleOptions.FLASH, glow);
        super.render(entity, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FlareEntity entity) {
        return Glow.TEXTURE;
    }
}
