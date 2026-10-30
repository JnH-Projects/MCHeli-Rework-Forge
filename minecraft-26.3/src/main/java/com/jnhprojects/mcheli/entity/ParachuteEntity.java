package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A parachute with someone hanging under it, {@code MCH_EntityParachute}. It sinks at about two
 * blocks a second, the rider steers it with the forward key, and it folds away a moment after it
 * touches the ground or the water.
 */
public class ParachuteEntity extends Entity {
    /** What the parachute looks like and how it opens, the original's model type. */
    public enum Kind {
        /** From the parachute item: open from the start. */
        ITEM,
        /** An ejection seat: the rider falls in the seat and the canopy opens once it drops. */
        EJECTION_SEAT,
        /** Troops dropped from a rack or a cargo door: open from the start, no seat. */
        DROP
    }

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(ParachuteEntity.class, EntityDataSerializers.INT);
    private static final double MAX_SPEED = 0.35;
    private static final double MIN_STEER = 0.07;

    private double steer = MIN_STEER;
    private int onGroundCount;
    /** Who boards on the next server tick; boarding in the same tick as the spawn can be lost. */
    private @Nullable Entity user;

    public ParachuteEntity(EntityType<? extends ParachuteEntity> type, Level level) {
        super(type, level);
    }

    /**
     * Opens a parachute above {@code user}, carrying over their speed, and puts them under it.
     * Server only.
     */
    public static ParachuteEntity open(Level level, Entity user, Kind kind) {
        ParachuteEntity parachute = new ParachuteEntity(ModEntities.PARACHUTE.get(), level);
        double lift = kind == Kind.ITEM ? 3.5 : 0.0;
        parachute.setPos(user.getX(), user.getY() + lift, user.getZ());
        parachute.setYRot(user.getYRot());
        parachute.setDeltaMovement(user.getDeltaMovement());
        parachute.entityData.set(DATA_KIND, kind.ordinal());
        parachute.user = user;
        user.resetFallDistance();
        level.addFreshEntity(parachute);
        return parachute;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, Kind.ITEM.ordinal());
    }

    public Kind kind() {
        Kind[] kinds = Kind.values();
        int i = entityData.get(DATA_KIND);
        return i >= 0 && i < kinds.length ? kinds[i] : Kind.ITEM;
    }

    /** An ejection seat only opens its canopy once it has started to fall. */
    public boolean isOpen() {
        return kind() != Kind.EJECTION_SEAT || getDeltaMovement().y < -0.1;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (user != null) {
            if (!user.isPassenger() && user.isAlive()) {
                user.startRiding(this, true, true);
                setYRot(user.getYRot());
            }
            user = null;
        }
        Entity rider = getFirstPassenger();
        if (rider == null && tickCount > 20 || isInWater()) {
            ejectPassengers();
            discard();
            return;
        }
        if (rider != null) {
            rider.resetFallDistance();
        }
        simulate(rider);
    }

    /** {@code onUpdateServer}. */
    private void simulate(@Nullable Entity rider) {
        Vec3 m = getDeltaMovement();
        double mx = m.x;
        double my = m.y;
        double mz = m.z;
        double prevSpeed = Math.sqrt(mx * mx + mz * mz);
        boolean seatLaunch = kind() == Kind.EJECTION_SEAT && tickCount < 20;
        my -= onGround() || seatLaunch ? 0.01 : 0.03;

        if (isOpen()) {
            if (rider instanceof ServerPlayer player && player.getLastClientInput().forward()) {
                double yaw = Math.toRadians(rider.getYRot());
                mx += -Math.sin(yaw) * steer * 0.05;
                mz += Math.cos(yaw) * steer * 0.05;
            }
            double speed = Math.sqrt(mx * mx + mz * mz);
            if (speed > MAX_SPEED) {
                mx *= MAX_SPEED / speed;
                mz *= MAX_SPEED / speed;
                speed = MAX_SPEED;
            }
            // Holding course builds up speed; letting go settles back to a slow drift.
            if (speed > prevSpeed) {
                steer = Math.min(steer + (MAX_SPEED - steer) / 35.0, MAX_SPEED);
            } else {
                steer = Math.max(steer - (steer - MIN_STEER) / 35.0, MIN_STEER);
            }
        }

        if (onGround() && ++onGroundCount > 5) {
            ejectPassengers();
            discard();
            return;
        }

        move(MoverType.SELF, new Vec3(mx, my, mz));
        my *= seatLaunch ? 0.95 : 0.9;
        double drag = isOpen() ? 0.95 : 0.99;
        setDeltaMovement(mx * drag, my, mz * drag);
        if (rider != null) {
            setYRot(rider.getYRot());
        }
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
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(DATA_KIND, input.getIntOr("Kind", Kind.ITEM.ordinal()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Kind", kind().ordinal());
    }
}
