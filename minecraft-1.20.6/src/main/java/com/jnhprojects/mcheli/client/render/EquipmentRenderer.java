package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Equipment drawn as one model from the models root with the texture of the same name, turned to
 * the entity's heading: the container, {@code MCH_RenderContainer}.
 */
public class EquipmentRenderer<T extends Entity> extends EntityRenderer<T> {
    private final String model;
    private final ResourceLocation texture;
    private final float yOffset;
    private final int tint;

    /**
     * @param model   the file name under {@code models/}, without extension
     * @param yOffset how far the model sits below the entity's position
     * @param tint    ARGB the model is multiplied by; the original greyed the container a little
     */
    public EquipmentRenderer(EntityRendererProvider.Context context, String model, float yOffset, int tint, float shadow) {
        super(context);
        this.model = model;
        this.texture = new ResourceLocation(MCHeli.MOD_ID, "textures/" + model + ".png");
        this.yOffset = yOffset;
        this.tint = tint;
        this.shadowRadius = shadow;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        Mesh mesh = MeshCache.INSTANCE.get("", model);
        if (mesh != null) {
            pose.pushPose();
            pose.translate(0.0F, -yOffset, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(-entity.getViewYRot(partialTicks)));
            pose.mulPose(Axis.XP.rotationDegrees(entity.getViewXRot(partialTicks)));
            MeshRenderer.emit(mesh.groups(), pose.last(), buffers.getBuffer(RenderType.entityCutout(texture)), light, OverlayTexture.NO_OVERLAY, tint);
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
