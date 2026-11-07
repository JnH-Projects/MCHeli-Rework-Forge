package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.projectile.A10Entity;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** The close air support A-10, {@code MCH_RenderA10}. */
public class A10Renderer extends EntityRenderer<A10Entity, A10Renderer.State> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/bullets/a10.png");

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
    public void render(State state, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        Mesh mesh = state.visible ? MeshCache.INSTANCE.get("", "a-10") : null;
        if (mesh != null) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(-state.pitch));
            int light = packedLight;
            Geometry.submit(buffers, pose, RenderType.entityCutout(TEXTURE),
                    (p, buffer) -> MeshRenderer.emit(mesh.groups(), p, buffer, light, OverlayTexture.NO_OVERLAY));
            pose.popPose();
        }
        super.render(state, pose, buffers, packedLight);
    }
}
