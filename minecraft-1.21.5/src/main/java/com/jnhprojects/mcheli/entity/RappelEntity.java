package com.jnhprojects.mcheli.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Carries someone down a helicopter's rope, {@code MCH_EntityHide}. It stays under its hook while
 * the helicopter drifts, slides down at a steady pace and lets its rider off at the ground or at
 * the end of the rope. Pulling the ropes in drops whoever is still on them.
 */
public class RappelEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_VEHICLE = SynchedEntityData.defineId(RappelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROPE = SynchedEntityData.defineId(RappelEntity.class, EntityDataSerializers.INT);
    private final InterpolationHandler interpolation = new VehicleInterpolation(this);

    public RappelEntity(EntityType<? extends RappelEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VEHICLE, 0);
        builder.define(DATA_ROPE, -1);
    }

    public void attach(AircraftEntity vehicle, int rope) {
        entityData.set(DATA_VEHICLE, vehicle.getId());
        entityData.set(DATA_ROPE, rope);
    }

    private @Nullable AircraftEntity vehicle() {
        return level().getEntity(entityData.get(DATA_VEHICLE)) instanceof AircraftEntity a && a.isAlive() ? a : null;
    }

    @Override
    public void tick() {
        super.tick();
        interpolation.interpolate();
        AircraftEntity vehicle = vehicle();
        Entity rider = getFirstPassenger();
        if (rider != null) {
            rider.resetFallDistance();
        }
        if (level().isClientSide()) {
            return;
        }
        if (vehicle == null || !vehicle.isRepelling() || rider == null) {
            arrive();
            return;
        }
        Vec3 hook = vehicle.ropePosition(entityData.get(DATA_ROPE));
        Vec3 m = getDeltaMovement();
        double fall = (m.y - (onGround() ? 0.01 : 0.03)) * 0.9;
        setPos(hook.x, getY(), hook.z);
        if (onGround()) {
            arrive();
            return;
        }
        move(MoverType.SELF, new Vec3(0.0, fall, 0.0));
        setDeltaMovement(0.0, fall, 0.0);
        if (Math.abs(getY() - hook.y) > Math.abs(vehicle.ropeLength()) + 5.0F) {
            arrive();
        }
    }

    private void arrive() {
        ejectPassengers();
        discard();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, -0.3, 0.0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
    }
}
