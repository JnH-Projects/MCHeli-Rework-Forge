package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.ParachuteEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshGroup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * {@code MCH_RenderParachute}: the item's round canopy, or the ejection seat with its canopy,
 * from the models the import brought in.
 */
public class ParachuteRenderer extends EntityRenderer<ParachuteEntity> {
    private static final ResourceLocation ROUND = new ResourceLocation(MCHeli.MOD_ID, "textures/parachute1.png");
    private static final ResourceLocation SEAT = new ResourceLocation(MCHeli.MOD_ID, "textures/parachute2.png");

    public ParachuteRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(ParachuteEntity entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        List<MeshGroup> groups = groups(entity);
        if (!groups.isEmpty()) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-entity.getViewYRot(partialTicks)));
            MeshRenderer.emit(groups, pose.last(), buffers.getBuffer(RenderType.entityCutout(getTextureLocation(entity))), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ParachuteEntity entity) {
        return entity.kind() == ParachuteEntity.Kind.ITEM ? ROUND : SEAT;
    }

    private static List<MeshGroup> groups(ParachuteEntity entity) {
        if (entity.kind() == ParachuteEntity.Kind.ITEM) {
            Mesh mesh = MeshCache.INSTANCE.get("", "parachute1");
            return mesh != null ? mesh.groups() : List.of();
        }
        Mesh mesh = MeshCache.INSTANCE.get("", "parachute2");
        boolean canopy = entity.kind() == ParachuteEntity.Kind.DROP || entity.isOpen();
        return mesh != null ? mesh.part(canopy ? "$parachute" : "$seat") : List.of();
    }
}
