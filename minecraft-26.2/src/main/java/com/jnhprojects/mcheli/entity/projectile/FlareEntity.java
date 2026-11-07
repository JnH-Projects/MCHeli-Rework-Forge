package com.jnhprojects.mcheli.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A burning flare, or with a fuse a smoke grenade that bursts into a cloud. Port of
 * {@code MCH_EntityFlare}. Flares only look the part: decoying happens in the dispenser.
 */
public class FlareEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(FlareEntity.class, EntityDataSerializers.INT);

    private double gravity = -0.013;
    private double airResistance = 0.992;
    private float size = 6.0F;

    public FlareEntity(EntityType<? extends FlareEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE, 0);
    }

    /**
     * @param fuse  ticks until a smoke grenade bursts, 0 for a flare
     * @param slow  the long hanging type 4 flare
     */
    public void launch(double x, double y, double z, double mx, double my, double mz, float size, int fuse, boolean slow) {
        snapTo(x, y, z, random.nextFloat() * 360.0F, random.nextFloat() * 360.0F);
        setDeltaMovement(mx, my, mz);
        this.size = size;
        entityData.set(DATA_FUSE, fuse);
        if (slow) {
            gravity *= 0.6;
            airResistance = 0.995;
        }
    }

    private int fuse() {
        return entityData.get(DATA_FUSE);
    }

    /** A smoke grenade rather than a burning flare. */
    public boolean isSmoke() {
        return fuse() > 0;
    }

    @Override
    public void tick() {
        if (fuse() > 0 && tickCount >= fuse()) {
            // Removal bursts the grenade on the client.
            discard();
            return;
        }
        boolean server = !level().isClientSide();
        if (server && (!level().isLoaded(blockPosition()) || tickCount > 300)) {
            discard();
            return;
        }
        super.tick();
        Vec3 m = getDeltaMovement();
        if (server) {
            HitResult hit = level().clip(new ClipContext(position(), position().add(m), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) {
                discard();
                return;
            }
        } else {
            trail();
        }
        setPos(getX() + m.x, getY() + m.y, getZ() + m.z);
        setDeltaMovement(m.x * airResistance, m.y + gravity, m.z * airResistance);
        if (server && (isInWater() || onGround())) {
            discard();
        }
    }

    private void trail() {
        double x = (getX() - xo) / 2.0;
        double y = (getY() - yo) / 2.0;
        double z = (getZ() - zo) / 2.0;
        for (int i = 0; i < 2; i++) {
            if (size < 5.0F && random.nextInt(2) == 0) {
                continue;
            }
            level().addAlwaysVisibleParticle(fuse() > 0 ? ParticleTypes.CLOUD : ParticleTypes.LARGE_SMOKE, true,
                    xo + x * i, yo + y * i, zo + z * i, 0.0, 0.0, 0.0);
            if (fuse() == 0) {
                level().addAlwaysVisibleParticle(ParticleTypes.FLAME, true, xo + x * i, yo + y * i, zo + z * i, 0.0, 0.0, 0.0);
            }
        }
    }

    /** A smoke grenade bursting into a cloud that hangs around for ten seconds or more. */
    private void burst() {
        if (!level().isClientSide()) {
            return;
        }
        for (int i = 0; i < 20; i++) {
            level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                    getX() + (random.nextDouble() - 0.5) * 10.0,
                    getY() + (random.nextDouble() - 0.5) * 10.0,
                    getZ() + (random.nextDouble() - 0.5) * 10.0,
                    (random.nextDouble() - 0.5) * 0.045, 0.005, (random.nextDouble() - 0.5) * 0.045);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level().isClientSide() && fuse() > 0) {
            burst();
            entityData.set(DATA_FUSE, 0);
        }
        super.remove(reason);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = getBoundingBox().getSize() * 4.0 * 64.0;
        return distance < d * d;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
