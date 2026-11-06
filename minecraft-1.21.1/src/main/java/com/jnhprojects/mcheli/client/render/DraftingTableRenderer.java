package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.block.DraftingTableBlock;
import com.jnhprojects.mcheli.block.DraftingTableBlockEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** {@code MCH_DraftingTableRenderer}: the original model, turned to the block's eighth of a circle. */
public class DraftingTableRenderer implements BlockEntityRenderer<DraftingTableBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/block/drafting_table.png");
    /** The original greyed the table a little. */
    private static final int TINT = 0xFFBFBFBF;

    public DraftingTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DraftingTableBlockEntity table, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Mesh mesh = MeshCache.INSTANCE.get("blocks", "drafting_table");
        if (mesh == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-table.getBlockState().getValue(DraftingTableBlock.ROTATION) * 45.0F + 180.0F));
        MeshRenderer.emit(mesh.groups(), pose.last(), buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY, TINT);
        pose.popPose();
    }
}
