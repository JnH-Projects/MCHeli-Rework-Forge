package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.projectile.ThrowableEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** {@code MCH_RenderThrowable}: the grenade's own model from {@code models/throwable}. */
public class ThrowableRenderer extends EntityRenderer<ThrowableEntity, ThrowableRenderer.State> {
    public ThrowableRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static class State extends EntityRenderState {
        String name = "";
        float yaw;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ThrowableEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.name = entity.definitionName();
        state.yaw = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Mesh mesh = state.name.isEmpty() ? null : MeshCache.INSTANCE.get("throwable", state.name);
        if (mesh != null) {
            Identifier texture = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/throwable/" + state.name + ".png");
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            int light = state.lightCoords;
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture),
                    (p, buffer) -> MeshRenderer.emit(mesh.groups(), p, buffer, light, OverlayTexture.NO_OVERLAY));
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }
}
