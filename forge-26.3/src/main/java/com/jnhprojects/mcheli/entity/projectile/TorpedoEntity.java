package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A torpedo, {@code MCH_EntityTorpedo}. Once in the water it settles on its running speed and
 * either holds course or, when guided, runs to the point the gunner aimed at.
 */
public class TorpedoEntity extends ProjectileEntity {
    public double targetX;
    public double targetY;
    public double targetZ;
    public double accelerationInWater = 2.0;

    public TorpedoEntity(EntityType<? extends TorpedoEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        WeaponDefinition w = weapon();
        boolean guided = w != null && w.guidedTorpedo;
        if (!level().isClientSide() && isInWater()) {
            if (acceleration < accelerationInWater) {
                acceleration += 0.1;
            } else if (acceleration > accelerationInWater + 0.2F) {
                acceleration -= 0.1;
            }
            if (guided) {
                aimAlong(targetX - getX(), targetY - getY(), targetZ - getZ());
            } else {
                Vec3 m = getDeltaMovement();
                aimAlong(m.x, m.y * 0.8F, m.z);
            }
        }
        if (isInWater()) {
            Vec3 m = getDeltaMovement();
            setYRot((float) Math.toDegrees(Math.atan2(m.z, m.x)) - 90.0F);
            if (guided) {
                setXRot(-(float) Math.toDegrees(Math.atan2(m.y, Math.sqrt(m.x * m.x + m.z * m.z))));
            }
            if (w != null && !w.disableSmoke) {
                trail(w.trajectoryParticle, 3, 5.0F * w.smokeSize * 0.5F);
            }
        }
    }

    @Override
    public String defaultModel() {
        return "torpedo";
    }
}
