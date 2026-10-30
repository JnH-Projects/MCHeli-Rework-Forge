package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.ClientConfig;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.effect.NightGlow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Not in the original: light that adds to the scene instead of covering it, for tracers, rocket
 * motors, flares and fireballs. Everything here blends additively and leaves the depth buffer
 * alone, so overlapping glows brighten each other and never cut holes in the smoke behind them.
 * With order independent transparency on, the vanilla translucent pipelines stand in.
 */
public final class Glow {
    /** A white disc fading to nothing at the rim; also the {@code mcheli:glow} particle sprite. */
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/particle/glow.png");

    private static final DepthStencilState TEST_ONLY = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false);

    private static final RenderPipeline PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "pipeline/glow_particle"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(TEST_ONLY)
            .build();

    private static final RenderPipeline ENTITY_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "pipeline/glow"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(TEST_ONLY)
            .build();

    /** The particle layer for {@code mcheli:glow}. */
    @SuppressWarnings("deprecation") // Vanilla's own particle layers still name the atlas this way.
    public static final SingleQuadParticle.Layer PARTICLE_LAYER = new SingleQuadParticle.Layer(
            true, TextureAtlas.LOCATION_PARTICLES, PARTICLE_PIPELINE, RenderPipelines.OIT_PARTICLE);

    private static final RenderType HALO = RenderType.create(MCHeli.MOD_ID + "_glow", RenderSetup.builder(ENTITY_PIPELINE)
            .setOitPipelines(RenderPipelines.OIT_EYES)
            .withTexture("Sampler0", TEXTURE)
            .sortOnUpload()
            .createRenderSetup());

    private static final int FULL_BRIGHT = LightCoordsUtil.FULL_BRIGHT;

    private Glow() {
    }

    /**
     * Hands both glow pipelines to Iris, so a shader pack draws them with a matching program: the
     * halos and tracers as emissive entities, the particle as a translucent particle. Called once
     * the mods are loaded; without Iris it does nothing.
     */
    public static void assignShaderPrograms() {
        IrisCompat.assign(ENTITY_PIPELINE, "EMISSIVE_ENTITIES");
        IrisCompat.assign(PARTICLE_PIPELINE, "PARTICLES_TRANSLUCENT");
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
     * @param color RGB; {@code strength} becomes the alpha, which scales the light added
     */
    public static void halo(PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, Vec3 offset, float radius, int color,
            float strength) {
        if (strength <= 0.0F || radius <= 0.0F) {
            return;
        }
        int argb = ARGB.color(alpha(strength), color);
        pose.pushPose();
        pose.translate(offset);
        pose.rotate(camera.orientation);
        collector.submitCustomGeometry(pose, HALO, (p, buffer) -> {
            // Both windings, so culling never hides it whichever way the quad ends up facing.
            vertex(buffer, p, -radius, -radius, 0.0F, 0.0F, 1.0F, argb);
            vertex(buffer, p, radius, -radius, 0.0F, 1.0F, 1.0F, argb);
            vertex(buffer, p, radius, radius, 0.0F, 1.0F, 0.0F, argb);
            vertex(buffer, p, -radius, radius, 0.0F, 0.0F, 0.0F, argb);
            vertex(buffer, p, -radius, radius, 0.0F, 0.0F, 0.0F, argb);
            vertex(buffer, p, radius, radius, 0.0F, 1.0F, 0.0F, argb);
            vertex(buffer, p, radius, -radius, 0.0F, 1.0F, 1.0F, argb);
            vertex(buffer, p, -radius, -radius, 0.0F, 0.0F, 1.0F, argb);
        });
        pose.popPose();
    }

    /**
     * A tracer: a ribbon of light from the current pose back along {@code tail}, turned to face
     * the camera, bright at the head and fading out towards the end.
     *
     * @param toCamera from the camera to the head, so the ribbon can be turned side on to the view
     */
    public static void streak(PoseStack pose, SubmitNodeCollector collector, Vec3 toCamera, Vec3 tail, float width, int color, float strength) {
        if (strength <= 0.0F || tail.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 side = tail.cross(toCamera);
        if (side.lengthSqr() < 1.0E-8) {
            // Seen exactly end on; the halo at the head covers this.
            return;
        }
        side = side.normalize().scale(width);
        int head = ARGB.color(alpha(strength), color);
        int end = ARGB.color(0, color);
        float sx = (float) side.x;
        float sy = (float) side.y;
        float sz = (float) side.z;
        float tx = (float) tail.x;
        float ty = (float) tail.y;
        float tz = (float) tail.z;
        collector.submitCustomGeometry(pose, HALO, (p, buffer) -> {
            // The middle row of the disc texture gives the ribbon soft edges.
            vertex(buffer, p, sx, sy, sz, 0.0F, 0.5F, head);
            vertex(buffer, p, -sx, -sy, -sz, 1.0F, 0.5F, head);
            vertex(buffer, p, tx - sx, ty - sy, tz - sz, 1.0F, 0.5F, end);
            vertex(buffer, p, tx + sx, ty + sy, tz + sz, 0.0F, 0.5F, end);
            vertex(buffer, p, tx + sx, ty + sy, tz + sz, 0.0F, 0.5F, end);
            vertex(buffer, p, tx - sx, ty - sy, tz - sz, 1.0F, 0.5F, end);
            vertex(buffer, p, -sx, -sy, -sz, 1.0F, 0.5F, head);
            vertex(buffer, p, sx, sy, sz, 0.0F, 0.5F, head);
        });
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
}
