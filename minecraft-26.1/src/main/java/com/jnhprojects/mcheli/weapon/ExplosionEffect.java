package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * The look of an MCHeli explosion, ported from {@code MCH_Explosion.effectExplosion} and
 * {@code effectExplosionInWater}: a fireball, ground dust thrown up from the crater, and columns
 * and rings of smoke that hang around for seconds. It runs on each client that receives the
 * explosion; the burning debris arcs are real flares spawned by the server.
 */
public final class ExplosionEffect {
    /** The largest crash fireball drawn; beyond this the particle count gets out of hand. */
    public static final float MAX_CRASH_SIZE = 12.0F;
    /** The widest explosion flash, in blocks; past this it washes out the whole screen. */
    private static final float MAX_FLASH_RADIUS = 30.0F;

    private ExplosionEffect() {
    }

    /**
     * Not in the original: a crashing vehicle goes up in a far bigger show than its fuel explosion
     * alone makes. The same shell of smoke as a missile hit in mid-air, a cluster of fireballs
     * around it, a plume of smoke that keeps climbing for seconds and burning fuel thrown about.
     * The fire glows for several seconds afterwards, which at night lights up the smoke above it.
     */
    public static void crash(Level level, double x, double y, double z, float size) {
        play(level, x, y, z, size, false);
        RandomSource random = level.getRandom();
        level.addAlwaysVisibleParticle(GlowParticleOptions.flash(GlowParticleOptions.FLASH, Math.min(size * 3.0F, MAX_FLASH_RADIUS), 20),
                true, x, y + size * 0.3, z, 0.0, 0.0, 0.0);
        int fireballs = Math.max(3, (int) (size / 2.0F));
        for (int i = 0; i < fireballs; i++) {
            double r = size * 0.5 * random.nextDouble();
            double a = random.nextDouble() * Math.PI * 2.0;
            double fx = x + Math.cos(a) * r;
            double fy = y + random.nextDouble() * size * 0.4;
            double fz = z + Math.sin(a) * r;
            level.addAlwaysVisibleParticle(ParticleTypes.EXPLOSION_EMITTER, true, fx, fy, fz, 1.0, 0.0, 0.0);
            level.addAlwaysVisibleParticle(GlowParticleOptions.fire(GlowParticleOptions.FIRE, size * (0.4F + random.nextFloat() * 0.3F),
                    80 + random.nextInt(60)), true, fx, fy, fz, 0.0, 0.01, 0.0);
        }
        for (int i = 0; i < size * 4; i++) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                    x + (random.nextDouble() - 0.5) * size * 0.6, y + random.nextDouble() * 2.0, z + (random.nextDouble() - 0.5) * size * 0.6,
                    (random.nextDouble() - 0.5) * 0.02, 0.05 + random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.02);
        }
        for (int i = 0; i < size * 10; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double speed = (0.2 + random.nextDouble() * 0.4) * size / 6.0;
            double up = random.nextDouble() * speed;
            level.addAlwaysVisibleParticle(random.nextInt(4) == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, true, x, y + 0.5, z,
                    Math.cos(a) * speed, up, Math.sin(a) * speed);
            if (i % 4 == 0) {
                level.addAlwaysVisibleParticle(GlowParticleOptions.fire(GlowParticleOptions.EMBER, 0.5F + random.nextFloat() * 0.4F,
                        20 + random.nextInt(20)), true, x, y + 0.5, z, Math.cos(a) * speed, up, Math.sin(a) * speed);
            }
        }
    }

    /**
     * Not in the original: the light of the blast itself, a white hot flash inside a wider red
     * glow, and a few embers thrown out. Faint by day; at night it lights up the smoke.
     */
    private static void flash(Level level, double x, double y, double z, float size) {
        RandomSource random = level.getRandom();
        float radius = Math.min(2.0F + size * 1.5F, MAX_FLASH_RADIUS);
        level.addAlwaysVisibleParticle(GlowParticleOptions.flash(GlowParticleOptions.FLASH, radius, 8 + (int) size), true, x, y, z, 0.0, 0.0, 0.0);
        level.addAlwaysVisibleParticle(GlowParticleOptions.flash(GlowParticleOptions.EMBER, Math.min(radius * 1.6F, MAX_FLASH_RADIUS),
                14 + (int) (size * 2.0F)), true, x, y, z, 0.0, 0.0, 0.0);
        int embers = Math.min((int) (size * 2.0F), 24);
        for (int i = 0; i < embers; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.2 + random.nextDouble() * 0.3 * Math.min(size, 4.0F);
            level.addAlwaysVisibleParticle(GlowParticleOptions.fire(GlowParticleOptions.EMBER, 0.3F + random.nextFloat() * 0.3F,
                    15 + random.nextInt(15)), true, x, y, z, Math.cos(a) * speed, 0.1 + random.nextDouble() * speed, Math.sin(a) * speed);
        }
    }

    public static void play(Level level, double x, double y, double z, float size, boolean inWater) {
        if (size <= 0.0F) {
            return;
        }
        if (inWater) {
            splash(level, x, y, z, size);
            return;
        }
        RandomSource random = level.getRandom();
        level.addAlwaysVisibleParticle(size >= 2.0F ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, true, x, y, z, 1.0, 0.0, 0.0);
        flash(level, x, y, z, size);

        int count = 0;
        int puffEvery = (int) Math.max(size, 4.0F);
        for (BlockPos pos : affectedBlocks(level, x, y, z, size)) {
            count++;
            double bx = pos.getX() + random.nextFloat();
            double by = pos.getY() + random.nextFloat();
            double bz = pos.getZ() + random.nextFloat();
            double mx = bx - x;
            double my = by - y;
            double mz = bz - z;
            double d = Math.sqrt(mx * mx + my * my + mz * mz);
            if (d == 0.0) {
                continue;
            }
            double push = 0.5 / (d / size + 0.1) * (random.nextFloat() * random.nextFloat() + 0.3F) * 0.5;
            mx = mx / d * push;
            my = my / d * push;
            mz = mz / d * push;
            double px = (bx + x) / 2.0;
            double py = (by + y) / 2.0;
            double pz = (bz + z) / 2.0;
            double angle = Math.toRadians(random.nextInt(360));

            if (count % 4 == 0) {
                // Dirt and stone thrown up from the ground under the blast.
                BlockState ground = level.getBlockState(BlockPos.containing(px + 0.5, py - 0.5, pz + 0.5));
                if (!ground.isAir()) {
                    float force = Math.min(size / 3.0F, 2.0F) * (0.5F + random.nextFloat() * 0.5F);
                    BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, ground);
                    for (int i = 0; i < 3; i++) {
                        level.addAlwaysVisibleParticle(dust, true, px, py + 1.0, pz,
                                Math.sin(angle) * force * 0.5, 0.5 + my / 5.0 * force, Math.cos(angle) * force * 0.5);
                    }
                }
            }
            if (size <= 1.0F || count % puffEvery == 0) {
                // Smoke either shoots up as a column or rolls out along the ground.
                if (random.nextBoolean()) {
                    my *= 3.0;
                    mx *= 0.1;
                    mz *= 0.1;
                } else {
                    my *= 0.2;
                    mx *= 3.0;
                    mz *= 3.0;
                }
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, px, py, pz, mx * 0.3, my * 0.3, mz * 0.3);
                for (int i = 0; i < 2; i++) {
                    level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, px, py, pz, mx, my, mz);
                }
                if (random.nextInt(3) == 0) {
                    level.addAlwaysVisibleParticle(ParticleTypes.FLAME, true, px, py, pz, mx * 0.5, my * 0.5, mz * 0.5);
                }
            }
        }
    }

    /**
     * The blocks the original's effect ray marched through: sixteen by sixteen rays from the
     * centre, each running until block resistance used up its strength.
     */
    @SuppressWarnings("deprecation") // The plain block resistance is what the original used here.
    private static Set<BlockPos> affectedBlocks(Level level, double x, double y, double z, float size) {
        Set<BlockPos> out = new HashSet<>();
        RandomSource random = level.getRandom();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    if (i != 0 && i != 15 && j != 0 && j != 15 && k != 0 && k != 15) {
                        continue;
                    }
                    double dx = i / 15.0F * 2.0F - 1.0F;
                    double dy = j / 15.0F * 2.0F - 1.0F;
                    double dz = k / 15.0F * 2.0F - 1.0F;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= len;
                    dy /= len;
                    dz /= len;
                    float strength = size * (0.7F + random.nextFloat() * 0.6F);
                    double cx = x;
                    double cy = y;
                    double cz = z;
                    for (float step = 0.3F; strength > 0.0F; strength -= step * 0.75F) {
                        cursor.set(Mth.floor(cx), Mth.floor(cy), Mth.floor(cz));
                        BlockState state = level.getBlockState(cursor);
                        if (!state.isAir()) {
                            strength -= (state.getBlock().getExplosionResistance() + 0.3F) * step;
                        }
                        if (strength > 0.0F) {
                            out.add(cursor.immutable());
                        }
                        cx += dx * step;
                        cy += dy * step;
                        cz += dz * step;
                    }
                }
            }
        }
        return out;
    }

    /** {@code effectExplosionInWater}: a dome of spray from the water around the charge. */
    private static void splash(Level level, double x, double y, double z, float size) {
        RandomSource random = level.getRandom();
        int range = Math.max((int) (size + 0.5), 1);
        int ex = (int) (x + 0.5);
        int ey = (int) (y + 0.5);
        int ez = (int) (z + 0.5);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -range; dy <= range; dy++) {
            for (int dx = -range; dx <= range; dx++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (dx * dx + dy * dy + dz * dz >= range * range) {
                        continue;
                    }
                    cursor.set(ex + dx, ey + dy, ez + dz);
                    if (!level.getFluidState(cursor).is(FluidTags.WATER)) {
                        continue;
                    }
                    for (int n = random.nextInt(2); n > 0; n--) {
                        double up = 1.0 - Math.sqrt(dx * dx + dz * dz) / range + random.nextFloat() * 0.4 * range * 0.4;
                        level.addAlwaysVisibleParticle(ParticleTypes.SPLASH, true, cursor.getX(), cursor.getY() + 1.0, cursor.getZ(),
                                (double) dx / range * (random.nextFloat() - 0.2), up, (double) dz / range * (random.nextFloat() - 0.2));
                        level.addAlwaysVisibleParticle(ParticleTypes.BUBBLE, true, cursor.getX(), cursor.getY(), cursor.getZ(), 0.0, up * 0.5, 0.0);
                    }
                }
            }
        }
    }
}
