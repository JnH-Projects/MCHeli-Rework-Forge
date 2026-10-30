package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * An air to surface missile flying to the block the gunner aimed at, {@code MCH_EntityASMissile}.
 * Powered ones climb for half a second first; gliding bombs only steer sideways.
 */
public class ASMissileEntity extends ProjectileEntity {
    public double targetX;
    public double targetY;
    public double targetZ;

    public ASMissileEntity(EntityType<? extends ASMissileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public float gravity() {
        return isBomblet() ? -0.03F : super.gravity();
    }

    @Override
    public float gravityInWater() {
        return isBomblet() ? -0.03F : super.gravityInWater();
    }

    @Override
    public void tick() {
        super.tick();
        WeaponDefinition w = weapon();
        if (w != null && !w.disableSmoke && !isBomblet()) {
            trail(w.trajectoryParticle, 3, 10.0F * w.smokeSize * 0.5F);
        }
        if (w != null && !level().isClientSide() && !isBomblet() && !isRemoved()) {
            BlockPos aim = BlockPos.containing(targetX, targetY, targetZ);
            if (!level().getBlockState(aim).isAir()) {
                double dist = Math.sqrt(distanceToSqr(targetX, targetY, targetZ));
                if (dist < w.proximityFuseDist) {
                    if (w.bomblet > 0) {
                        for (int i = 0; i < w.bomblet; i++) {
                            sprinkleBomblet();
                        }
                    } else {
                        impact(new Impact(position(), null, blockPosition(), Direction.UP), 1.0F);
                    }
                    discard();
                } else if (gravity() == 0.0F) {
                    double up = age() < 10 ? 20.0 : 0.0;
                    aimAlong(targetX - getX(), targetY + up - getY(), targetZ - getZ());
                } else {
                    double dx = targetX - getX();
                    double dy = (targetY - getY()) * 0.3;
                    double dz = targetZ - getZ();
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    Vec3 m = getDeltaMovement();
                    setDeltaMovement(dx * acceleration / d, m.y, dz * acceleration / d);
                }
            }
        }
        faceMotion();
        tickBomblets();
    }

    @Override
    protected void sprinkleBomblet() {
        WeaponDefinition w = weapon();
        Vec3 m = getDeltaMovement();
        ASMissileEntity child = spawnChild(ModEntities.AS_MISSILE.get(), m.x, m.y, m.z, random.nextInt(360), 0.0F);
        if (child == null || w == null) {
            return;
        }
        float spread = w.bombletDiff;
        child.setDeltaMovement(m.x * 0.5 + (random.nextFloat() - 0.5F) * spread,
                m.y * 0.5 / 2.0 + (random.nextFloat() - 0.5F) * spread / 2.0F,
                m.z * 0.5 + (random.nextFloat() - 0.5F) * spread);
        child.setBomblet();
        level().addFreshEntity(child);
    }

    @Override
    public String defaultModel() {
        return "asmissile";
    }
}
