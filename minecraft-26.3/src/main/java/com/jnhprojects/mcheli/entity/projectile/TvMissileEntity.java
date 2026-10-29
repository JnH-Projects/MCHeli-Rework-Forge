package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A wire guided missile flown by the gunner's view, {@code MCH_EntityTvMissile}. It steers while
 * it is the vehicle's active TV missile; once another is fired it carries on straight.
 */
public class TvMissileEntity extends ProjectileEntity {
    private static final double MAX_DISTANCE_SQ = 1440000.0;

    public TvMissileEntity(EntityType<? extends TvMissileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        WeaponDefinition w = weapon();
        if (w != null && !w.disableSmoke) {
            trail(w.trajectoryParticle, 3, 5.0F * w.smokeSize * 0.5F);
        }
        Entity aimer = user();
        if (aimer != null) {
            if (distanceToSqr(aimer) > MAX_DISTANCE_SQ) {
                discard();
            }
            if (!level().isClientSide() && !isRemoved()) {
                steer(aimer);
            }
        } else if (!level().isClientSide()) {
            discard();
        }
    }

    private void steer(Entity aimer) {
        if (aimer.isRemoved() || !(shooter() instanceof AircraftEntity vehicle) || vehicle.tvMissile() != this) {
            return;
        }
        Vec3 dir = Vec3.directionFromRotation(aimer.getXRot(), aimer.getYRot());
        aimAlong(dir.x, dir.y, dir.z);
        setYRot(aimer.getYRot());
        setXRot(aimer.getXRot());
    }

    @Override
    public String defaultModel() {
        return "asmissile";
    }
}
