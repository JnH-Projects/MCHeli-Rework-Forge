package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.ClientConfig;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.effect.NightGlow;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Not in the original: light that adds to the scene instead of covering it, for tracers, rocket
 * motors, flares and fireballs. Everything here blends additively and leaves the depth buffer
 * alone, so overlapping glows brighten each other and never cut holes in the smoke behind them.
 */
public final class Glow {
    /** A white disc fading to nothing at the rim; also the {@code mcheli:glow} particle sprite. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/particle/glow.png");

    /** The particle render type for {@code mcheli:glow}: additive, depth tested, no depth writes. */
    public static final ParticleRenderType PARTICLE_LAYER = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "MCHELI_GLOW";
        }
    };

    private static final RenderType HALO = Types.halo();

    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    private Glow() {
    }

    /**
     * How strongly something at {@code pos} glows: faint by day, full at night, in caves and
     * under water. 0 when the player turned the glow off.
     */
    public static float strength(Level level, BlockPos pos) {
        if (!ClientConfig.nightGlow()) {
            return 0.0F;
        }
        return NightGlow.strength(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos), level.getSkyDarken());
    }

    /**
     * A disc of light facing the camera, centred {@code offset} away from the current pose.
     *
     * @param camera the camera's orientation, from the entity render dispatcher
     * @param color  RGB; {@code strength} becomes the alpha, which scales the light added
     */
    public static void halo(PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 offset, float radius, int color,
            float strength) {
        if (strength <= 0.0F || radius <= 0.0F) {
            return;
        }
        int argb = FastColor.ARGB32.color(alpha(strength), color);
        pose.pushPose();
        pose.translate(offset.x, offset.y, offset.z);
        pose.mulPose(camera);
        PoseStack.Pose p = pose.last();
        VertexConsumer buffer = buffers.getBuffer(HALO);
        // Both windings, so culling never hides it whichever way the quad ends up facing.
        vertex(buffer, p, -radius, -radius, 0.0F, 0.0F, 1.0F, argb);
        vertex(buffer, p, radius, -radius, 0.0F, 1.0F, 1.0F, argb);
        vertex(buffer, p, radius, radius, 0.0F, 1.0F, 0.0F, argb);
        vertex(buffer, p, -radius, radius, 0.0F, 0.0F, 0.0F, argb);
        vertex(buffer, p, -radius, radius, 0.0F, 0.0F, 0.0F, argb);
        vertex(buffer, p, radius, radius, 0.0F, 1.0F, 0.0F, argb);
        vertex(buffer, p, radius, -radius, 0.0F, 1.0F, 1.0F, argb);
        vertex(buffer, p, -radius, -radius, 0.0F, 0.0F, 1.0F, argb);
        pose.popPose();
    }

    /**
     * A tracer: a ribbon of light from the current pose back along {@code tail}, turned to face
     * the camera, bright at the head and fading out towards the end.
     *
     * @param toCamera from the camera to the head, so the ribbon can be turned side on to the view
     */
    public static void streak(PoseStack pose, MultiBufferSource buffers, Vec3 toCamera, Vec3 tail, float width, int color, float strength) {
        if (strength <= 0.0F || tail.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 side = tail.cross(toCamera);
        if (side.lengthSqr() < 1.0E-8) {
            // Seen exactly end on; the halo at the head covers this.
            return;
        }
        side = side.normalize().scale(width);
        int head = FastColor.ARGB32.color(alpha(strength), color);
        int end = FastColor.ARGB32.color(0, color);
        float sx = (float) side.x;
        float sy = (float) side.y;
        float sz = (float) side.z;
        float tx = (float) tail.x;
        float ty = (float) tail.y;
        float tz = (float) tail.z;
        PoseStack.Pose p = pose.last();
        VertexConsumer buffer = buffers.getBuffer(HALO);
        // The middle row of the disc texture gives the ribbon soft edges.
        vertex(buffer, p, sx, sy, sz, 0.0F, 0.5F, head);
        vertex(buffer, p, -sx, -sy, -sz, 1.0F, 0.5F, head);
        vertex(buffer, p, tx - sx, ty - sy, tz - sz, 1.0F, 0.5F, end);
        vertex(buffer, p, tx + sx, ty + sy, tz + sz, 0.0F, 0.5F, end);
        vertex(buffer, p, tx + sx, ty + sy, tz + sz, 0.0F, 0.5F, end);
        vertex(buffer, p, tx - sx, ty - sy, tz - sz, 1.0F, 0.5F, end);
        vertex(buffer, p, -sx, -sy, -sz, 1.0F, 0.5F, head);
        vertex(buffer, p, sx, sy, sz, 0.0F, 0.5F, head);
    }

    private static int alpha(float strength) {
        return Math.clamp(Math.round(strength * 255.0F), 0, 255);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb) {
        buffer.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /**
     * Builds the halo render type. A subclass only to reach the protected render state shards;
     * {@code RenderType.create} is not public on every loader, the constructor is.
     */
    private abstract static class Types extends RenderType {
        private Types() {
            super("", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 0, false, false, () -> {
            }, () -> {
            });
        }

        /** The eyes shader (full bright, no light map), additive, depth tested without depth writes. */
        static RenderType halo() {
            RenderStateShard[] shards = {
                    RENDERTYPE_EYES_SHADER,
                    new TextureStateShard(TEXTURE, false, false),
                    LIGHTNING_TRANSPARENCY,
                    COLOR_WRITE,
                    LEQUAL_DEPTH_TEST
            };
            return new RenderType(MCHeli.MOD_ID + "_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    TRANSIENT_BUFFER_SIZE, false, true,
                    () -> {
                        for (RenderStateShard shard : shards) {
                            shard.setupRenderState();
                        }
                    },
                    () -> {
                        for (RenderStateShard shard : shards) {
                            shard.clearRenderState();
                        }
                    }) {
            };
        }
    }
}
