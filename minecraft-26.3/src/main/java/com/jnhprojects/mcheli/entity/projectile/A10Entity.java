package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The A-10 that answers a close air support call, {@code MCH_EntityA10}. It dives in for a second,
 * strafes the target with forty GAU-8 rounds and climbs away.
 */
public class A10Entity extends Entity {
    private static final EntityDataAccessor<String> DATA_WEAPON = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_DIRECTION = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.INT);
    private static final int LIFETIME = 70;

    private int despawnCount;
    private int shotCount;
    public int power = 32;
    public int explosionPower = 1;
    public float acceleration = 4.0F;
    private @Nullable Entity shooter;
    private @Nullable Entity user;

    public A10Entity(EntityType<? extends A10Entity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WEAPON, "");
        builder.define(DATA_DIRECTION, 0);
    }

    /**
     * @param direction 0 heading south (+Z), then clockwise in steps of 90 degrees
     */
    public void setup(String weapon, int direction, double mx, double mz, @Nullable Entity shooter, @Nullable Entity user) {
        entityData.set(DATA_WEAPON, weapon);
        entityData.set(DATA_DIRECTION, direction);
        setYRot(90 * direction);
        yRotO = getYRot();
        setDeltaMovement(mx, 0.0, mz);
        this.shooter = shooter;
        this.user = user;
    }

    public int direction() {
        return entityData.get(DATA_DIRECTION);
    }

    /** Hidden during the first second, while it is still out of sight. */
    public boolean isVisible() {
        return despawnCount > 20;
    }

    @Override
    public void tick() {
        super.tick();
        despawnCount++;
        if (!level().isClientSide()) {
            if (despawnCount > LIFETIME) {
                discard();
                return;
            }
            if (shotCount < 40) {
                for (int i = 0; i < 2; i++) {
                    fireGau8(shotCount);
                    shotCount++;
                }
                if (shotCount == 38) {
                    ModSounds.playAt(this, "gau-8_snd", 150.0F, 1.0F);
                }
            }
        }
        if (despawnCount <= 20) {
            setDeltaMovement(getDeltaMovement().x, -0.3, getDeltaMovement().z);
        } else {
            setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
            setDeltaMovement(getDeltaMovement().add(0.0, 0.02, 0.0));
        }
    }

    private void fireGau8(int count) {
        int dir = direction();
        double x = getX();
        double z = getZ();
        double tx = random.nextDouble() - 0.5;
        double ty = -2.6;
        double tz = random.nextDouble() - 0.5;
        switch (dir) {
            case 0 -> {
                tz += 10.0;
                z += count * 0.6;
            }
            case 1 -> {
                tx -= 10.0;
                x -= count * 0.6;
            }
            case 2 -> {
                tz -= 10.0;
                z -= count * 0.6;
            }
            default -> {
                tx += 10.0;
                x += count * 0.6;
            }
        }
        BulletEntity round = ModEntities.BULLET.get().create(level(), EntitySpawnReason.TRIGGERED);
        if (round == null) {
            return;
        }
        round.launch(x, getY(), z, tx, ty, tz, 90 * dir, 30.0F, acceleration);
        round.setWeapon(entityData.get(DATA_WEAPON));
        round.explosionPower = shotCount % 4 == 0 ? explosionPower : 0;
        round.power = power;
        round.setOwners(shooter, user);
        level().addFreshEntity(round);
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0 * 512.0;
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
