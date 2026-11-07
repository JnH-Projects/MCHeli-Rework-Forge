package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.entity.ChainEntity;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@code MCH_RenderChain}: a line of links from the towed end to the end that pulls, one link
 * every 0.95 blocks, each turned along the chain.
 */
public class ChainRenderer extends EntityRenderer<ChainEntity, ChainRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/chain.png");
    private static final double LINK = 0.95;
    private static final int GREY = 0xFF808080;

    public ChainRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static class State extends EntityRenderState {
        /** Towed end, relative to the chain entity. */
        Vec3 from = Vec3.ZERO;
        /** From the towed end to the end that pulls. */
        Vec3 span = Vec3.ZERO;
    }

    /** The chain hangs over the end that pulls but reaches to the other end; keep both in view. */
    @Override
    protected AABB getBoundingBoxForCulling(ChainEntity chain) {
        AABB box = super.getBoundingBoxForCulling(chain);
        Entity towed = chain.towed();
        return towed != null ? box.minmax(towed.getBoundingBox()) : box;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChainEntity chain, State state, float partialTicks) {
        super.extractRenderState(chain, state, partialTicks);
        Entity tow = chain.tow();
        Entity towed = chain.towed();
        if (tow == null || towed == null) {
            state.span = Vec3.ZERO;
            return;
        }
        Vec3 here = chain.getPosition(partialTicks);
        Vec3 start = towed.getPosition(partialTicks);
        state.from = start.subtract(here);
        state.span = tow.getPosition(partialTicks).subtract(start);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Mesh mesh = MeshCache.INSTANCE.get("", "chain");
        double length = state.span.length();
        if (mesh != null && length > LINK) {
            Vec3 step = state.span.scale(LINK / length);
            float yaw = (float) Math.toDegrees(Math.atan2(step.z, step.x));
            float pitch = (float) Math.toDegrees(Math.atan2(step.y, Math.sqrt(step.x * step.x + step.z * step.z)));
            int light = state.lightCoords;
            pose.pushPose();
            pose.translate(state.from);
            for (double left = length; left > LINK; left -= LINK) {
                pose.translate(step);
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(-yaw));
                pose.mulPose(Axis.ZP.rotationDegrees(pitch));
                collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE),
                        (p, buffer) -> MeshRenderer.emit(mesh.groups(), p, buffer, light, OverlayTexture.NO_OVERLAY, GREY));
                pose.popPose();
            }
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }
}
