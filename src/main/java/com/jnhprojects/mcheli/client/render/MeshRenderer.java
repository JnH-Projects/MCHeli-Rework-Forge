package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.model.MeshGroup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/** Streams {@link MeshGroup} triangles into an entity render type. */
final class MeshRenderer {
    private static final int WHITE = 0xFFFFFFFF;

    private MeshRenderer() {
    }

    /**
     * Entity render types draw quads, so each triangle is sent as a quad whose last corner repeats
     * the third one. The GPU discards the degenerate half for free.
     */
    static void emit(List<MeshGroup> groups, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        emit(groups, pose, buffer, light, overlay, WHITE);
    }

    /** As above, tinted with an ARGB colour, for bullets drawn in their {@code BulletColor}. */
    static void emit(List<MeshGroup> groups, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        for (MeshGroup group : groups) {
            float[] p = group.positions();
            float[] n = group.normals();
            float[] uv = group.uvs();
            int vertices = group.vertexCount();
            for (int v = 0; v < vertices; v += 3) {
                vertex(buffer, pose, p, n, uv, v, light, overlay, color);
                vertex(buffer, pose, p, n, uv, v + 1, light, overlay, color);
                vertex(buffer, pose, p, n, uv, v + 2, light, overlay, color);
                vertex(buffer, pose, p, n, uv, v + 2, light, overlay, color);
            }
        }
    }

    /**
     * As above, but only the triangles that face the camera. No vanilla type is both culled and
     * free of depth writes, so glass is culled here. Back to back glass faces would otherwise sort
     * in an arbitrary order and swap places as the view moves.
     *
     * <p>Entities are submitted in world-aligned space with the camera at the origin, so a front
     * face (counter-clockwise, as in the original's GL setup) is one whose normal points back
     * towards the origin.
     */
    static void emitFrontFaces(List<MeshGroup> groups, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        Matrix4f m = pose.pose();
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        for (MeshGroup group : groups) {
            float[] p = group.positions();
            float[] n = group.normals();
            float[] uv = group.uvs();
            int vertices = group.vertexCount();
            for (int v = 0; v < vertices; v += 3) {
                m.transformPosition(p[v * 3], p[v * 3 + 1], p[v * 3 + 2], a);
                m.transformPosition(p[v * 3 + 3], p[v * 3 + 4], p[v * 3 + 5], b);
                m.transformPosition(p[v * 3 + 6], p[v * 3 + 7], p[v * 3 + 8], c);
                b.sub(a);
                c.sub(a);
                if (b.cross(c).dot(a) >= 0.0F) {
                    continue;
                }
                vertex(buffer, pose, p, n, uv, v, light, overlay, color);
                vertex(buffer, pose, p, n, uv, v + 1, light, overlay, color);
                vertex(buffer, pose, p, n, uv, v + 2, light, overlay, color);
                vertex(buffer, pose, p, n, uv, v + 2, light, overlay, color);
            }
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float[] p, float[] n, float[] uv, int i, int light, int overlay, int color) {
        int a = i * 3;
        int t = i * 2;
        buffer.addVertex(pose, p[a], p[a + 1], p[a + 2])
                .setColor(color)
                .setUv(uv[t], uv[t + 1])
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, n[a], n[a + 1], n[a + 2]);
    }

    static int overlay(float hurtTime) {
        return OverlayTexture.pack(0, OverlayTexture.v(hurtTime > 0.0F));
    }
}
