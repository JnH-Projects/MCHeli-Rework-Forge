package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.ParachuteEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshGroup;
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

import java.util.List;

/**
 * {@code MCH_RenderParachute}: the item's round canopy, or the ejection seat with its canopy,
 * from the models the import brought in.
 */
public class ParachuteRenderer extends EntityRenderer<ParachuteEntity, ParachuteRenderer.State> {
    private static final ResourceLocation ROUND = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/parachute1.png");
    private static final ResourceLocation SEAT = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/parachute2.png");

    public ParachuteRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    public static class State extends EntityRenderState {
        float yaw;
        ParachuteEntity.Kind kind = ParachuteEntity.Kind.ITEM;
        boolean open;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ParachuteEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getYRot(partialTicks);
        state.kind = entity.kind();
        state.open = entity.isOpen();
    }

    @Override
    public void render(State state, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        List<MeshGroup> groups = groups(state);
        if (!groups.isEmpty()) {
            ResourceLocation texture = state.kind == ParachuteEntity.Kind.ITEM ? ROUND : SEAT;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            int light = packedLight;
            Geometry.submit(buffers, pose, RenderType.entityCutoutNoCull(texture),
                    (p, buffer) -> MeshRenderer.emit(groups, p, buffer, light, OverlayTexture.NO_OVERLAY));
            pose.popPose();
        }
        super.render(state, pose, buffers, packedLight);
    }

    private static List<MeshGroup> groups(State state) {
        if (state.kind == ParachuteEntity.Kind.ITEM) {
            Mesh mesh = MeshCache.INSTANCE.get("", "parachute1");
            return mesh != null ? mesh.groups() : List.of();
        }
        Mesh mesh = MeshCache.INSTANCE.get("", "parachute2");
        boolean canopy = state.kind == ParachuteEntity.Kind.DROP || state.open;
        return mesh != null ? mesh.part(canopy ? "$parachute" : "$seat") : List.of();
    }
}
