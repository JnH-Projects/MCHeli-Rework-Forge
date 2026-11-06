package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** A gun round, {@code MCH_EntityBullet}. High explosive rounds may carry a proximity fuse. */
public class BulletEntity extends ProjectileEntity {
    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean boostsAcceleration() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        WeaponDefinition w = weapon();
        if (isRemoved() || level().isClientSide() || age() <= 1 || w == null || explosionPower <= 0) {
            return;
        }
        float fuse = w.proximityFuseDist;
        if (fuse > 0.1F) {
            fuse++;
            float range = fuse + Math.abs(w.acceleration);
            for (Entity entity : level().getEntities(this, getBoundingBox().inflate(range), this::canHit)) {
                if (entity.distanceToSqr(this) < fuse * fuse) {
                    setPos((entity.getX() + getX()) / 2.0, (entity.getY() + getY()) / 2.0, (entity.getZ() + getZ()) / 2.0);
                    impact(new Impact(position(), null, blockPosition(), Direction.UP), 1.0F);
                    break;
                }
            }
        }
    }

    @Override
    public String defaultModel() {
        return "bullet";
    }
}
