package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.projectile.A10Entity;
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

/** The close air support A-10, {@code MCH_RenderA10}. */
public class A10Renderer extends EntityRenderer<A10Entity, A10Renderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/bullets/a10.png");

    public A10Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 10.5F;
    }

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
        boolean visible;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(A10Entity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getYRot(partialTicks);
        state.pitch = entity.getXRot(partialTicks);
        state.visible = entity.isVisible();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Mesh mesh = state.visible ? MeshCache.INSTANCE.get("", "a-10") : null;
        if (mesh != null) {
            pose.pushPose();
            pose.rotateDegrees(Axis.YP, -state.yaw);
            pose.rotateDegrees(Axis.XP, -state.pitch);
            int light = state.lightCoords;
            collector.submitCustomGeometry(pose, RenderTypes.entityCutoutCull(TEXTURE),
                    (p, buffer) -> MeshRenderer.emit(mesh.groups(), p, buffer, light, OverlayTexture.NO_OVERLAY));
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }
}
