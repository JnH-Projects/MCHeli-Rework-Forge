package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** An air to air missile homing on its locked target, {@code MCH_EntityAAMissile}. */
public class AAMissileEntity extends ProjectileEntity {
    private static final double MAX_DISTANCE_SQ = 3422500.0;

    public AAMissileEntity(EntityType<? extends AAMissileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        WeaponDefinition w = weapon();
        if (age() > 4 && w != null && !w.disableSmoke) {
            trail(w.trajectoryParticle, 3, 7.0F * w.smokeSize * 0.5F);
        }
        if (level().isClientSide() || w == null || isRemoved()) {
            return;
        }
        Entity t = target();
        if (user() == null || t == null || t.isRemoved()) {
            discard();
            return;
        }
        double d = distanceToSqr(t);
        if (d > MAX_DISTANCE_SQ) {
            discard();
        } else if (age() > w.rigidityTime) {
            // The original compared the squared distance with the plain fuse distance.
            if (w.proximityFuseDist >= 0.1F && d < w.proximityFuseDist) {
                setPos((t.getX() + getX()) / 2.0, (t.getY() + getY()) / 2.0, (t.getZ() + getZ()) / 2.0);
                impact(new Impact(position(), t, null, Direction.UP), 1.0F);
            } else {
                guideTo(t.getX(), t.getY(), t.getZ());
            }
        }
    }

    @Override
    public String defaultModel() {
        return "aamissile";
    }
}
