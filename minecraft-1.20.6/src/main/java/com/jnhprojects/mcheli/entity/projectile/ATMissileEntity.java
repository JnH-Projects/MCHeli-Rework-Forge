package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * An anti tank missile, {@code MCH_EntityATMissile}. In the default mode it flies straight at the
 * locked target; in top attack mode it climbs 150 blocks above the shooter and dives.
 */
public class ATMissileEntity extends ProjectileEntity {
    private static final double MAX_DISTANCE_SQ = 2250000.0;

    /** 0 direct, 1 top attack. */
    public int guidanceType;

    public ATMissileEntity(EntityType<? extends ATMissileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        WeaponDefinition w = weapon();
        if (w != null && !w.disableSmoke && tickCount >= w.trajectoryParticleStartTick) {
            trail(w.trajectoryParticle, 3, 5.0F * w.smokeSize * 0.5F);
        }
        if (!level().isClientSide() && !isRemoved()) {
            Entity t = target();
            if (user() != null && t != null && !t.isRemoved() && w != null) {
                steer(w, t);
            } else {
                discard();
            }
        }
        faceMotion();
    }

    private void steer(WeaponDefinition w, Entity t) {
        double x = t.getX() - getX();
        double y = t.getY() - getY();
        double z = t.getZ() - getZ();
        double d = x * x + y * y + z * z;
        if (d > MAX_DISTANCE_SQ) {
            discard();
            return;
        }
        if (w.proximityFuseDist >= 0.1F && d < w.proximityFuseDist) {
            impact(new Impact(position(), null, blockPosition(), Direction.UP), 1.0F);
            return;
        }
        int rigidity = w.rigidityTime;
        if (age() <= rigidity) {
            return;
        }
        float af = age() < rigidity + w.trajectoryParticleStartTick ? 0.5F : 1.0F;
        if (guidanceType == 1) {
            Entity shooter = user();
            if (age() <= rigidity + 20) {
                guideTo(t.getX(), shooter.getY() + 150.0, t.getZ());
            } else if (age() <= rigidity + 30) {
                guideTo(t.getX(), shooter.getY(), t.getZ());
            } else {
                if (age() == rigidity + 35) {
                    power = (int) (power * 1.2F);
                    if (explosionPower > 0) {
                        explosionPower++;
                    }
                }
                guideTo(t.getX(), t.getY(), t.getZ());
            }
        } else {
            d = Math.sqrt(d);
            setDeltaMovement(x * acceleration / d * af, y * acceleration / d * af, z * acceleration / d * af);
        }
    }

    @Override
    public String defaultModel() {
        return "asmissile";
    }
}
