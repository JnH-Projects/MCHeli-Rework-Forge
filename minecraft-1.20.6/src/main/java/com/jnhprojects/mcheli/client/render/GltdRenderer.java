package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.GltdEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshGroup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
public class GltdRenderer extends EntityRenderer<GltdEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MCHeli.MOD_ID, "textures/gltd.png");
    private static final float HEAD_PIVOT = 0.45F;
    private static final double LASER_LENGTH = 100.0;
    private static final int LASER = 0xFF2020;

    public GltdRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(GltdEntity entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        float bodyYaw = entity.getViewYRot(partialTicks);
        Entity viewer = entity.getFirstPassenger();
        Minecraft mc = Minecraft.getInstance();
        boolean drawHead = viewer == null || viewer != mc.player || !mc.options.getCameraType().isFirstPerson();
        float headYaw = viewer != null ? viewer.getViewYRot(partialTicks) : bodyYaw;
        float headPitch = viewer != null ? viewer.getViewXRot(partialTicks) : 0.0F;
        Mesh mesh = MeshCache.INSTANCE.get("", "gltd");
        if (mesh != null) {
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            draw(mesh.part("$body"), pose, buffer, light);
            pose.popPose();
            if (drawHead) {
                pose.pushPose();
                pose.translate(0.0F, HEAD_PIVOT, 0.0F);
                pose.mulPose(Axis.YP.rotationDegrees(-headYaw));
                pose.mulPose(Axis.XP.rotationDegrees(headPitch));
                pose.translate(0.0F, -HEAD_PIVOT, 0.0F);
                draw(mesh.part("$head"), pose, buffer, light);
                pose.popPose();
            }
        }
        if (viewer != null) {
            float yaw = headYaw * Mth.DEG_TO_RAD;
            float pitch = headPitch * Mth.DEG_TO_RAD;
            Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
            Vec3 head = new Vec3(0.0, HEAD_PIVOT + 0.2, 0.0);
            Vec3 toCamera = entity.getPosition(partialTicks).add(head).subtract(entityRenderDispatcher.camera.getPosition());
            pose.pushPose();
            pose.translate(head.x, head.y, head.z);
            // Brightest at the tripod, fading out along the beam.
            Glow.streak(pose, buffers, toCamera, look.scale(LASER_LENGTH), 0.02F, LASER, 0.25F);
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GltdEntity entity) {
        return TEXTURE;
    }

    private static void draw(List<MeshGroup> groups, PoseStack pose, VertexConsumer buffer, int light) {
        if (!groups.isEmpty()) {
            MeshRenderer.emit(groups, pose.last(), buffer, light, OverlayTexture.NO_OVERLAY);
        }
    }
}
