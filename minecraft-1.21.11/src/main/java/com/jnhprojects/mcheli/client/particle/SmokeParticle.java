package com.jnhprojects.mcheli.client.particle;

import com.jnhprojects.mcheli.client.render.Glow;
import com.jnhprojects.mcheli.particle.SmokeParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * {@code mcheli:smoke}: a puff of coloured smoke, {@code MCH_EntityParticleSmoke}. It swells to its
 * full size over the first part of its life, its colour drifts towards white, it rises slowly and
 * fades out at the end. The sprites are vanilla's campfire smoke.
 */
public class SmokeParticle extends SingleQuadParticle {
    private final float size;

    SmokeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SmokeParticleOptions options,
            SpriteSet sprites, RandomSource random) {
        super(level, x, y, z, sprites.get(random));
        setParticleSpeed(xd, yd, zd);
        // The original drew a puff 0.1 x its size out from the middle, so size 10 is two blocks across.
        this.size = options.size() * 0.1F * (0.8F + random.nextFloat() * 0.4F);
        this.quadSize = size * 0.4F;
        this.lifetime = options.lifetime() > 0 ? options.lifetime() : 60 + random.nextInt(40);
        this.friction = 0.96F;
        this.gravity = -0.002F;
        this.hasPhysics = true;
        setColor(ARGB.redFloat(options.color()), ARGB.greenFloat(options.color()), ARGB.blueFloat(options.color()));
        this.alpha = 0.9F;
    }

    @Override
    public void tick() {
        super.tick();
        // toWhite: pale towards the brightest channel while the colour is still strong.
        float max = Math.max(rCol, Math.max(gCol, bCol));
        float min = Math.min(rCol, Math.min(gCol, bCol));
        if (max - min > 0.2F) {
            rCol += (max - rCol) * 0.016F;
            gCol += (max - gCol) * 0.016F;
            bCol += (max - bCol) * 0.016F;
        }
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        float t = Mth.clamp((age + partialTicks) / lifetime, 0.0F, 1.0F);
        alpha = 0.9F * Math.min(1.0F, (1.0F - t) * 4.0F);
        super.extract(state, camera, partialTicks);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float t = Mth.clamp((age + partialTicks) / lifetime, 0.0F, 1.0F);
        return size * (0.4F + 0.6F * Math.min(1.0F, t * 3.0F));
    }

    @Override
    protected Layer getLayer() {
        return Glow.SMOKE_LAYER;
    }

    public static class Provider implements ParticleProvider<SmokeParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SmokeParticleOptions options, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd, RandomSource random) {
            return new SmokeParticle(level, x, y, z, xd, yd, zd, options, sprites, random);
        }
    }
}
