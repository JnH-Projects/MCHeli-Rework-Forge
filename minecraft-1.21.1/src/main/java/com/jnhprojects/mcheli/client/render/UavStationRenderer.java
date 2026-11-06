package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.UavStationEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshGroup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The UAV station and the portable controller, {@code MCH_RenderUavStation}. The station's screen
 * lights up while a drone is flown from it; the controller's lid, keyboard half and display fold
 * open as it is opened, and light up once fully open.
 */
public class UavStationRenderer extends EntityRenderer<UavStationEntity> {
    private static final String[] MODEL = {"uav_station", "uav_portable_controller"};
    /** The original greyed both a little. */
    private static final int TINT = 0xFFBFBFBF;

    public UavStationRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.0F;
    }

    @Override
    public void render(UavStationEntity entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        int index = entity.kind() == UavStationEntity.PORTABLE ? 1 : 0;
        float cover = entity.cover(partialTicks);
        boolean lit = index == 1 ? cover > 0.95F : entity.isLinked();
        Mesh mesh = MeshCache.INSTANCE.get("", MODEL[index]);
        if (mesh != null) {
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/" + MODEL[index] + (lit ? "_on" : "") + ".png");
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(texture));
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-entity.getViewYRot(partialTicks)));
            if (index == 0) {
                draw(pose, buffer, mesh.groups(), light);
            } else {
                draw(pose, buffer, mesh.part("$body"), light);
                folding(pose, buffer, mesh.part("$cover"), cover * 60.0F, -0.1812F, -0.3186F, light);
                folding(pose, buffer, mesh.part("$laptop_cover"), cover * 95.0F, -0.1808F, -0.0422F, light);
                folding(pose, buffer, mesh.part("$display"), cover * -85.0F, -0.1807F, 0.2294F, light);
            }
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(UavStationEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/" + MODEL[entity.kind() == UavStationEntity.PORTABLE ? 1 : 0] + ".png");
    }

    /** {@code renderRotPart}: turns a part about the x axis through its hinge. */
    private static void folding(PoseStack pose, VertexConsumer buffer, List<MeshGroup> part, float angle, float y, float z, int light) {
        pose.pushPose();
        pose.translate(0.0F, y, z);
        pose.mulPose(Axis.XN.rotationDegrees(angle));
        pose.translate(0.0F, -y, -z);
        draw(pose, buffer, part, light);
        pose.popPose();
    }

    private static void draw(PoseStack pose, VertexConsumer buffer, List<MeshGroup> groups, int light) {
        if (!groups.isEmpty()) {
            MeshRenderer.emit(groups, pose.last(), buffer, light, OverlayTexture.NO_OVERLAY, TINT);
        }
    }
}
