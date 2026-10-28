package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.projectile.FlareEntity;
import com.jnhprojects.mcheli.entity.projectile.ProjectileEntity;
import com.jnhprojects.mcheli.network.ExplosionEffectPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Port of {@code MCH_Explosion}.
 *
 * <p>The original explosion had two sizes: one for blocks and one, doubled, for entities, with its
 * own damage curve and the weapon's per-target {@code DamageFactor}. Entity damage is computed
 * here as it was; the blocks, particles and sound go through a vanilla explosion of the block size,
 * so mob griefing rules, protection mods and the client effects all behave as players expect.
 */
public final class Explosions {
    /** Players this far away still see the smoke of an explosion. */
    private static final double EFFECT_RANGE = 256.0;

    private Explosions() {
    }

    /** What an explosion request needs besides its position and size. */
    public record Params(@Nullable Entity exploder, @Nullable Entity cause, float size, float blockSize,
                         boolean flaming, boolean destroyBlocks, int fireSeconds, boolean inWater,
                         @Nullable WeaponDefinition weapon) {
    }

    /**
     * @return whether any entity took damage, which the shooter hears as a hit confirmation
     */
    public static boolean explode(ServerLevel level, double x, double y, double z, Params p) {
        Vec3 center = new Vec3(x, y, z);
        boolean hit = damageEntities(level, center, p);

        boolean breakBlocks = p.destroyBlocks && p.blockSize > 0.0F;
        float radius = breakBlocks ? p.blockSize : Math.max(p.size, 0.5F);
        level.explode(p.exploder, explosionSource(level, p), new Calculator(p.inWater, level),
                x, y, z, radius, p.flaming && breakBlocks,
                breakBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        ModNetwork.sendNear(level, x, y, z, EFFECT_RANGE, new ExplosionEffectPacket(x, y, z, p.size, p.inWater));
        if (!p.inWater) {
            throwDebris(level, x, y, z, p.size);
        }
        return hit;
    }

    /**
     * The look of a vehicle going up when it crashes: no damage or blocks of its own (the crash
     * explosion does those), but the {@link ExplosionEffect#crash big fireball} on every nearby
     * client and twice the burning debris. Drawn a little above the impact so its smoke spreads
     * into the air the way a missile hit's does, rather than straight into the ground.
     */
    public static void crashFireball(ServerLevel level, double x, double y, double z, float size) {
        float visual = Math.clamp(size * 2.0F, 6.0F, ExplosionEffect.MAX_CRASH_SIZE);
        double lift = visual * 0.25;
        ModNetwork.sendNear(level, x, y + lift, z, EFFECT_RANGE, new ExplosionEffectPacket(x, y + lift, z, visual, false, true));
        throwDebris(level, x, y, z, visual, 24);
    }

    private static void throwDebris(ServerLevel level, double x, double y, double z, float size) {
        throwDebris(level, x, y, z, size, 12);
    }

    /**
     * Big blasts throw burning debris that arcs away trailing smoke, which the original made of
     * flares. One piece per point of size, up to {@code maxPieces}.
     */
    private static void throwDebris(ServerLevel level, double x, double y, double z, float size, int maxPieces) {
        if (size < 4.0F) {
            return;
        }
        int pieces = Math.min((int) size * maxPieces / 12, maxPieces);
        for (int i = 0; i < pieces; i++) {
            double angle = Math.toRadians(level.getRandom().nextInt(360));
            double speed = Math.min(size / 12.0F, 0.6) * (0.5F + level.getRandom().nextFloat() * 0.5F);
            FlareEntity debris = ModEntities.FLARE.get().create(level, EntitySpawnReason.TRIGGERED);
            if (debris != null) {
                double spread = size / 2.0;
                debris.launch(x + (level.getRandom().nextDouble() - 0.5) * spread, y + 2.0, z + (level.getRandom().nextDouble() - 0.5) * spread,
                        Math.sin(angle) * speed, (1.0 + level.getRandom().nextFloat() * 0.2) * speed, Math.cos(angle) * speed, 2.0F, 0, false);
                level.addFreshEntity(debris);
            }
        }
    }

    private static DamageSource explosionSource(ServerLevel level, Params p) {
        return level.damageSources().explosion(p.exploder, p.cause);
    }

    /** {@code doExplosionA} for entities: twice the block size, the 1.7.10 damage curve and knockback. */
    private static boolean damageEntities(ServerLevel level, Vec3 center, Params p) {
        float size = p.size * 2.0F;
        if (size <= 0.0F) {
            return false;
        }
        AABB area = new AABB(center, center).inflate(size + 1.0);
        DamageSource source = explosionSource(level, p);
        boolean hit = false;
        for (Entity entity : level.getEntities(p.exploder, area)) {
            double d7 = Math.sqrt(entity.distanceToSqr(center)) / size;
            if (d7 > 1.0) {
                continue;
            }
            double dx = entity.getX() - center.x;
            double dy = entity.getEyeY() - center.y;
            double dz = entity.getZ() - center.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist == 0.0) {
                continue;
            }
            dx /= dist;
            dy /= dist;
            dz /= dist;
            double exposure = ServerExplosion.getSeenPercent(center, entity);
            double impact = (1.0 - d7) * exposure;
            float damage = (int) ((impact * impact + impact) / 2.0 * 8.0 * size + 1.0);
            if (damage > 0.0F && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb)
                    && !(entity instanceof FallingBlockEntity)) {
                // Our own rounds caught in the blast do not count as hitting something.
                if (!(entity instanceof ProjectileEntity bullet) || bullet.user() != p.cause) {
                    hit = true;
                }
            }
            Combat.hurt(entity, source, damage, p.weapon);
            if (!(entity instanceof ProjectileEntity)) {
                entity.push(dx * impact * 0.4, dy * impact * 0.1, dz * impact * 0.4);
            }
            if (damage > 0.0F && p.fireSeconds > 0) {
                double fire = 1.0 - dist / size;
                if (fire > 0.0) {
                    entity.igniteForSeconds((float) (fire * p.fireSeconds));
                }
            }
        }
        return hit;
    }

    /**
     * Vanilla explosion settings for the block pass: no entity damage or knockback (done above),
     * and under water the blocks crumble the way the original's water charges made them.
     */
    private static final class Calculator extends ExplosionDamageCalculator {
        private final boolean inWater;
        private final Level level;

        Calculator(boolean inWater, Level level) {
            this.inWater = inWater;
            this.level = level;
        }

        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter getter, BlockPos pos,
                                                           BlockState block, FluidState fluid) {
            if (!inWater) {
                return super.getBlockExplosionResistance(explosion, getter, pos, block, fluid);
            }
            if (block.isAir()) {
                return Optional.empty();
            }
            float resistance = block.getExplosionResistance(getter, pos, explosion);
            return Optional.of(resistance * (level.getRandom().nextFloat() * 0.2F + 0.2F));
        }

        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return 0.0F;
        }
    }

    /** {@code playExplosionSound}: the bang of a round that has no explosive charge. */
    public static void playSound(Level level, double x, double y, double z) {
        float pitch = (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F;
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, pitch);
    }
}
