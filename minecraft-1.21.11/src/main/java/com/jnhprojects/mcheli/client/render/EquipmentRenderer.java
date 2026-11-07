package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * Equipment drawn as one model from the models root with the texture of the same name, turned to
 * the entity's heading: the container, {@code MCH_RenderContainer}.
 */
public class EquipmentRenderer<T extends Entity> extends EntityRenderer<T, EquipmentRenderer.State> {
    private final String model;
    private final Identifier texture;
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
        this.texture = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/" + model + ".png");
        this.yOffset = yOffset;
        this.tint = tint;
        this.shadowRadius = shadow;
    }

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getYRot(partialTicks);
        state.pitch = entity.getXRot(partialTicks);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Mesh mesh = MeshCache.INSTANCE.get("", model);
        if (mesh != null) {
            pose.pushPose();
            pose.translate(0.0F, -yOffset, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(state.pitch));
            int light = state.lightCoords;
            collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(texture),
                    (p, buffer) -> MeshRenderer.emit(mesh.groups(), p, buffer, light, OverlayTexture.NO_OVERLAY, tint));
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }
}
