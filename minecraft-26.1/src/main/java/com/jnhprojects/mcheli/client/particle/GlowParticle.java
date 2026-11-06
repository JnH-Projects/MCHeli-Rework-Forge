package com.jnhprojects.mcheli.client.particle;

import com.jnhprojects.mcheli.client.render.Glow;
import com.jnhprojects.mcheli.particle.GlowParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * {@code mcheli:glow}: a ball of light added on top of the scene. A flash swells quickly and fades
 * out; a fire holds its brightness, wavering, and only fades towards the end. How bright it gets
 * depends on the light where it starts, so the same explosion is a flicker at noon and lights up
 * the sky at night.
 */
public class GlowParticle extends SingleQuadParticle {
    private final float size;
    private final float peak;
    private final boolean flicker;
    private float waver = 1.0F;

    GlowParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, GlowParticleOptions options,
            TextureAtlasSprite sprite, float strength) {
        super(level, x, y, z, sprite);
        setParticleSpeed(xd, yd, zd);
        this.size = options.size();
        this.peak = strength;
        this.flicker = options.flicker();
        this.quadSize = size;
        this.lifetime = options.lifetime();
        this.friction = 0.9F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        setColor(ARGB.redFloat(options.color()), ARGB.greenFloat(options.color()), ARGB.blueFloat(options.color()));
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (flicker) {
            waver = 0.7F + random.nextFloat() * 0.3F;
        }
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        float t = Mth.clamp((age + partialTicks) / lifetime, 0.0F, 1.0F);
        if (flicker) {
            // Burning: hold, then die down over the last third.
            alpha = peak * waver * Math.min(1.0F, (1.0F - t) * 3.0F);
        } else {
            // A flash: brightest at once, gone quickly.
            float left = 1.0F - t;
            alpha = peak * left * left;
        }
        super.extract(state, camera, partialTicks);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float t = Mth.clamp((age + partialTicks) / lifetime, 0.0F, 1.0F);
        return flicker ? size * (0.85F + 0.15F * waver) : size * (0.6F + 0.4F * Math.min(1.0F, t * 4.0F));
    }

    @Override
    protected int getLightCoords(float partialTicks) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    protected Layer getLayer() {
        return Glow.PARTICLE_LAYER;
    }

    public static class Provider implements ParticleProvider<GlowParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(GlowParticleOptions options, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd, RandomSource random) {
            float strength = Glow.strength(level, BlockPos.containing(x, y, z));
            if (strength <= 0.0F) {
                return null;
            }
            return new GlowParticle(level, x, y, z, xd, yd, zd, options, sprites.first(), strength);
        }
    }
}
