package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.projectile.ThrowableEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** {@code MCH_RenderThrowable}: the grenade's own model from {@code models/throwable}. */
public class ThrowableRenderer extends EntityRenderer<ThrowableEntity> {
    public ThrowableRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ThrowableEntity entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        String name = entity.definitionName();
        Mesh mesh = name.isEmpty() ? null : MeshCache.INSTANCE.get("throwable", name);
        if (mesh != null) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-entity.getViewYRot(partialTicks)));
            MeshRenderer.emit(mesh.groups(), pose.last(), buffers.getBuffer(RenderType.entityCutout(getTextureLocation(entity))), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrowableEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/throwable/" + entity.definitionName() + ".png");
    }
}
