package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.UavStationEntity;
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
 * The UAV station and the portable controller, {@code MCH_RenderUavStation}. The station's screen
 * lights up while a drone is flown from it; the controller's lid, keyboard half and display fold
 * open as it is opened, and light up once fully open.
 */
public class UavStationRenderer extends EntityRenderer<UavStationEntity, UavStationRenderer.State> {
    private static final String[] MODEL = {"uav_station", "uav_portable_controller"};
    /** The original greyed both a little. */
    private static final int TINT = 0xFFBFBFBF;

    public UavStationRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.0F;
    }

    public static class State extends EntityRenderState {
        float yaw;
        int kind;
        boolean lit;
        float cover;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(UavStationEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getYRot(partialTicks);
        state.kind = entity.kind();
        state.cover = entity.cover(partialTicks);
        state.lit = state.kind == UavStationEntity.PORTABLE ? state.cover > 0.95F : entity.isLinked();
    }

    @Override
    public void render(State state, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        int index = state.kind == UavStationEntity.PORTABLE ? 1 : 0;
        Mesh mesh = MeshCache.INSTANCE.get("", MODEL[index]);
        if (mesh != null) {
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/" + MODEL[index] + (state.lit ? "_on" : "") + ".png");
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            int light = packedLight;
            if (index == 0) {
                draw(pose, buffers, texture, mesh.groups(), light);
            } else {
                draw(pose, buffers, texture, mesh.part("$body"), light);
                folding(pose, buffers, texture, mesh.part("$cover"), state.cover * 60.0F, -0.1812F, -0.3186F, light);
                folding(pose, buffers, texture, mesh.part("$laptop_cover"), state.cover * 95.0F, -0.1808F, -0.0422F, light);
                folding(pose, buffers, texture, mesh.part("$display"), state.cover * -85.0F, -0.1807F, 0.2294F, light);
            }
            pose.popPose();
        }
        super.render(state, pose, buffers, packedLight);
    }

    /** {@code renderRotPart}: turns a part about the x axis through its hinge. */
    private static void folding(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, List<MeshGroup> part,
                                float angle, float y, float z, int light) {
        pose.pushPose();
        pose.translate(0.0F, y, z);
        pose.mulPose(Axis.XN.rotationDegrees(angle));
        pose.translate(0.0F, -y, -z);
        draw(pose, buffers, texture, part, light);
        pose.popPose();
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, List<MeshGroup> groups, int light) {
        if (!groups.isEmpty()) {
            Geometry.submit(buffers, pose, RenderType.entityCutoutNoCull(texture),
                    (p, buffer) -> MeshRenderer.emit(groups, p, buffer, light, OverlayTexture.NO_OVERLAY, TINT));
        }
    }
}
