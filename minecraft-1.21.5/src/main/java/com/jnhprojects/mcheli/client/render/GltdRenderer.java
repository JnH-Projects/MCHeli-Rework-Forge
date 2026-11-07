package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.GltdEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshGroup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * {@code MCH_RenderGLTD}: the tripod, its head turned to where the viewer aims, and a faint red
 * laser out of the head while someone uses it. The viewer's own head part is left out in first
 * person, where it would fill the view.
 */
public class GltdRenderer extends EntityRenderer<GltdEntity, GltdRenderer.State> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/gltd.png");
    private static final float HEAD_PIVOT = 0.45F;
    private static final double LASER_LENGTH = 100.0;
    private static final int LASER = 0xFF2020;

    public GltdRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    public static class State extends EntityRenderState {
        float yaw;
        float headYaw;
        float headPitch;
        boolean drawHead = true;
        boolean laser;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GltdEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getYRot(partialTicks);
        Entity viewer = entity.getFirstPassenger();
        Minecraft mc = Minecraft.getInstance();
        state.laser = viewer != null;
        state.drawHead = viewer == null || viewer != mc.player || !mc.options.getCameraType().isFirstPerson();
        state.headYaw = viewer != null ? viewer.getYRot(partialTicks) : state.yaw;
        state.headPitch = viewer != null ? viewer.getXRot(partialTicks) : 0.0F;
    }

    @Override
    public void render(State state, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Mesh mesh = MeshCache.INSTANCE.get("", "gltd");
        if (mesh != null) {
            int light = packedLight;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            draw(mesh.part("$body"), pose, buffers, light);
            pose.popPose();
            if (state.drawHead) {
                pose.pushPose();
                pose.translate(0.0F, HEAD_PIVOT, 0.0F);
                pose.mulPose(Axis.YP.rotationDegrees(-state.headYaw));
                pose.mulPose(Axis.XP.rotationDegrees(state.headPitch));
                pose.translate(0.0F, -HEAD_PIVOT, 0.0F);
                draw(mesh.part("$head"), pose, buffers, light);
                pose.popPose();
            }
        }
        if (state.laser) {
            float yaw = state.headYaw * Mth.DEG_TO_RAD;
            float pitch = state.headPitch * Mth.DEG_TO_RAD;
            Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
            Vec3 head = new Vec3(0.0, HEAD_PIVOT + 0.2, 0.0);
            Vec3 toCamera = new Vec3(state.x, state.y, state.z).add(head).subtract(camera.getPosition());
            pose.pushPose();
            pose.translate(head);
            // Brightest at the tripod, fading out along the beam.
            Glow.streak(pose, buffers, toCamera, look.scale(LASER_LENGTH), 0.02F, LASER, 0.25F);
            pose.popPose();
        }
        super.render(state, pose, buffers, packedLight);
    }

    private static void draw(List<MeshGroup> groups, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!groups.isEmpty()) {
            Geometry.submit(buffers, pose, RenderType.entityCutoutNoCull(TEXTURE),
                    (p, buffer) -> MeshRenderer.emit(groups, p, buffer, light, OverlayTexture.NO_OVERLAY));
        }
    }
}
