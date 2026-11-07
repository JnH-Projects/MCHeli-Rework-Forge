package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.block.DraftingTableBlock;
import com.jnhprojects.mcheli.block.DraftingTableBlockEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** {@code MCH_DraftingTableRenderer}: the original model, turned to the block's eighth of a circle. */
public class DraftingTableRenderer implements BlockEntityRenderer<DraftingTableBlockEntity, DraftingTableRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/block/drafting_table.png");
    /** The original greyed the table a little. */
    private static final int TINT = 0xFFBFBFBF;

    public DraftingTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        int rotation;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DraftingTableBlockEntity table, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTicks, cameraPosition, breakProgress);
        state.rotation = table.getBlockState().getValue(DraftingTableBlock.ROTATION);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Mesh mesh = MeshCache.INSTANCE.get("blocks", "drafting_table");
        if (mesh == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-state.rotation * 45.0F + 180.0F));
        int light = state.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE),
                (p, buffer) -> MeshRenderer.emit(mesh.groups(), p, buffer, light, OverlayTexture.NO_OVERLAY, TINT));
        pose.popPose();
    }
}
