package com.jnhprojects.mcheli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Draws straight into the frame's buffers. Later versions queue geometry for a submit step instead,
 * so this keeps the renderers reading the same on every version.
 */
final class Geometry {
    private Geometry() {
    }

    /** Fills a buffer of {@code type} with what {@code renderer} emits, at the pose as it is now. */
    static void submit(MultiBufferSource buffers, PoseStack pose, RenderType type, Renderer renderer) {
        renderer.emit(pose.last(), buffers.getBuffer(type));
    }

    interface Renderer {
        void emit(PoseStack.Pose pose, VertexConsumer buffer);
    }
}
