package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.ChainEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * {@code MCH_RenderChain}: a line of links from the towed end to the end that pulls, one link
 * every 0.95 blocks, each turned along the chain.
 */
public class ChainRenderer extends EntityRenderer<ChainEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/chain.png");
    private static final double LINK = 0.95;
    private static final int GREY = 0xFF808080;

    public ChainRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    /** The chain hangs over the end that pulls but reaches to the other end; keep both in view. */
    @Override
    public boolean shouldRender(ChainEntity chain, Frustum frustum, double x, double y, double z) {
        Entity towed = chain.towed();
        return super.shouldRender(chain, frustum, x, y, z) || towed != null && frustum.isVisible(towed.getBoundingBox());
    }

    @Override
    public void render(ChainEntity chain, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity tow = chain.tow();
        Entity towed = chain.towed();
        Mesh mesh = MeshCache.INSTANCE.get("", "chain");
        if (mesh != null && tow != null && towed != null) {
            Vec3 start = towed.getPosition(partialTicks);
            Vec3 from = start.subtract(chain.getPosition(partialTicks));
            Vec3 span = tow.getPosition(partialTicks).subtract(start);
            double length = span.length();
            if (length > LINK) {
                Vec3 step = span.scale(LINK / length);
                float yaw = (float) Math.toDegrees(Math.atan2(step.z, step.x));
                float pitch = (float) Math.toDegrees(Math.atan2(step.y, Math.sqrt(step.x * step.x + step.z * step.z)));
                VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
                pose.pushPose();
                pose.translate(from.x, from.y, from.z);
                for (double left = length; left > LINK; left -= LINK) {
                    pose.translate(step.x, step.y, step.z);
                    pose.pushPose();
                    pose.mulPose(Axis.YP.rotationDegrees(-yaw));
                    pose.mulPose(Axis.ZP.rotationDegrees(pitch));
                    MeshRenderer.emit(mesh.groups(), pose.last(), buffer, light, OverlayTexture.NO_OVERLAY, GREY);
                    pose.popPose();
                }
                pose.popPose();
            }
        }
        super.render(chain, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ChainEntity chain) {
        return TEXTURE;
    }
}
