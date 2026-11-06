package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * A marking rocket, {@code MCH_EntityMarkerRocket}. Where it lands under open sky it pops red
 * smoke, and five seconds later a stick of bombs falls on the mark.
 */
public class MarkerRocketEntity extends ProjectileEntity {
    private static final EntityDataAccessor<Byte> DATA_STATUS = SynchedEntityData.defineId(MarkerRocketEntity.class, EntityDataSerializers.BYTE);

    /** 0 inert, 1 flying, 2 marking. */
    private int countDown;

    public MarkerRocketEntity(EntityType<? extends MarkerRocketEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATUS, (byte) 0);
    }

    public void setStatus(int status) {
        if (!level().isClientSide()) {
            entityData.set(DATA_STATUS, (byte) status);
        }
    }

    public int status() {
        return entityData.get(DATA_STATUS);
    }

    @Override
    public void tick() {
        int status = status();
        if (level().isClientSide()) {
            if (weapon() == null) {
                super.tick();
            }
            WeaponDefinition w = weapon();
            if (w != null && !w.disableSmoke && status != 0) {
                if (status == 1) {
                    super.tick();
                    trail(w.trajectoryParticle, 3, 5.0F * w.smokeSize * 0.5F);
                } else {
                    float gb = random.nextFloat() * 0.3F;
                    int rgb = (int) ((random.nextFloat() * 0.2F + 0.8F) * 255) << 16 | (int) (gb * 255) << 8 | (int) (gb * 255);
                    for (int i = 0; i < 5; i++) {
                        level().addAlwaysVisibleParticle(new DustParticleOptions(rgb, 4.0F), true, getX(), getY(), getZ(),
                                (random.nextFloat() - 0.5F) * 0.7F, 0.3F + random.nextFloat() * 0.3F, (random.nextFloat() - 0.5F) * 0.7F);
                    }
                }
            }
        } else if (status == 0 || isInWater()) {
            discard();
        } else if (status == 1) {
            super.tick();
        } else if (countDown > 0) {
            countDown--;
            if (countDown == 40) {
                dropBombs();
            }
        } else {
            discard();
        }
    }

    private void dropBombs() {
        int count = 6 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            BombEntity bomb = ModEntities.BOMB.get().create(level(), EntitySpawnReason.TRIGGERED);
            if (bomb == null) {
                continue;
            }
            bomb.launch(getX() + (random.nextFloat() - 0.5F) * 15.0F, 260.0F + random.nextFloat() * 10.0F + i * 30,
                    getZ() + (random.nextFloat() - 0.5F) * 15.0F, 0.0, -0.5, 0.0, 0.0F, 90.0F, 4.0);
            bomb.setWeapon(weaponName());
            bomb.explosionPower = 3 + random.nextInt(2);
            bomb.explosionPowerInWater = 0;
            bomb.power = 30;
            bomb.piercing = 0;
            bomb.setOwners(shooter, user);
            level().addFreshEntity(bomb);
        }
    }

    @Override
    protected void impact(Impact hit, float damageFactor) {
        if (level().isClientSide() || hit.entity() != null || hit.block() == null) {
            return;
        }
        BlockPos mark = hit.block().relative(hit.side());
        if (!level().getBlockState(mark).isAir()) {
            discard();
            return;
        }
        level().setBlockAndUpdate(mark, Blocks.FIRE.defaultBlockState());
        int solid = 0;
        for (int y = mark.getY() + 1; y < level().getMaxY(); y++) {
            if (!level().getBlockState(new BlockPos(mark.getX(), y, mark.getZ())).isAir() && ++solid >= 5) {
                break;
            }
        }
        if (solid < 5) {
            setStatus(2);
            setPos(mark.getX() + 0.5, mark.getY() + 1.1, mark.getZ() + 0.5);
            xo = getX();
            yo = getY();
            zo = getZ();
            countDown = 100;
        } else {
            discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public String defaultModel() {
        return "rocket";
    }
}
