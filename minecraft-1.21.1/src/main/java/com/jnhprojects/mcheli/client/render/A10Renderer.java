package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.projectile.A10Entity;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** The close air support A-10, {@code MCH_RenderA10}. */
public class A10Renderer extends EntityRenderer<A10Entity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/bullets/a10.png");

    public A10Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 10.5F;
    }

    @Override
    public void render(A10Entity entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        Mesh mesh = entity.isVisible() ? MeshCache.INSTANCE.get("", "a-10") : null;
        if (mesh != null) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot(partialTicks)));
            pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot(partialTicks)));
            MeshRenderer.emit(mesh.groups(), pose.last(), buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(A10Entity entity) {
        return TEXTURE;
    }
}
