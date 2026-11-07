package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.VehicleDefinition;
import com.jnhprojects.mcheli.content.aircraft.WeaponMount;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Static guns and launchers, ported from {@code MCH_EntityVehicle}.
 *
 * <p>The mount itself does not turn; the gunner looks around and the {@code AddPart} tree turns
 * with the view. {@code CanMove} lets W/S push the mount and A/D turn it, for the few that set it.
 */
public class GroundVehicleEntity extends AircraftEntity implements EngineSoundSource {
    private static final double MIN_SPEED = 0.07;
    private double currentSpeed = MIN_SPEED;

    public GroundVehicleEntity(EntityType<? extends GroundVehicleEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected @Nullable VehicleDefinition lookup(String name) {
        return MCHeli.content().vehicle(name);
    }

    @Override
    public @Nullable VehicleDefinition definition() {
        return (VehicleDefinition) super.definition();
    }

    /**
     * {@code MCH_EntityVehicle.useCurrentWeapon}: the mount stays put while its {@code AddPart}
     * tree turns with the gunner's view, so a weapon fires where the gunner looks. Only weapons
     * with both yaw limits set aim like turrets instead. The original checked the first
     * {@code AddWeapon} line of that weapon, and so does this.
     */
    @Override
    public boolean aimsWithUser(WeaponSet set) {
        WeaponMount mount = set.mounts().getFirst();
        return mount.minYaw() == 0.0F || mount.maxYaw() == 0.0F;
    }

    @Override
    protected void simulate() {
        VehicleDefinition def = definition();
        Vec3 m = getDeltaMovement();
        double mx = m.x;
        double my = m.y;
        double mz = m.z;

        if (getControllingPassenger() == null) {
            throttle = Math.max(throttle - 0.00125, 0.0);
        } else if (def.canMove) {
            double x = 0.0;
            double z = 0.0;
            double yaw = Math.toRadians(getYRot());
            if (input.throttleUp) {
                x += Math.sin(yaw);
                z += Math.cos(yaw);
            }
            if (input.throttleDown) {
                x -= Math.sin(yaw);
                z -= Math.cos(yaw);
            }
            if (input.left && !input.right) {
                setYRot(getYRot() - 0.5F);
            } else if (input.right && !input.left) {
                setYRot(getYRot() + 0.5F);
            }
            double d = Math.sqrt(x * x + z * z);
            if (d > 1.0E-6) {
                mx -= x / d * 0.03;
                mz += z / d * 0.03;
            }
        }
        if (isInWater()) {
            setXRot(getXRot() * 0.9F);
        }

        double prevMotion = Math.sqrt(mx * mx + mz * mz);
        double depth = def.floats ? waterDepth() : 0.0;
        if (depth == 0.0) {
            my += isInWater() ? def.gravityInWater : def.gravity;
        } else if (depth < 1.0) {
            my -= 1.0E-4;
            my += 0.007 * throttle;
        } else {
            if (my < 0.0) {
                my /= 2.0;
            }
            my += 0.007;
        }

        double motion = Math.sqrt(mx * mx + mz * mz);
        float limit = def.speed;
        if (motion > limit) {
            mx *= limit / motion;
            mz *= limit / motion;
            motion = limit;
        }
        if (motion > prevMotion && currentSpeed < limit) {
            currentSpeed = Math.min(limit, currentSpeed + (limit - currentSpeed) / 35.0);
        } else {
            currentSpeed = Math.max(MIN_SPEED, currentSpeed - (currentSpeed - MIN_SPEED) / 35.0);
        }
        if (onGround()) {
            mx *= 0.5;
            mz *= 0.5;
        }

        move(MoverType.SELF, new Vec3(mx, my, mz));
        setDeltaMovement(mx * 0.99, my * 0.95, mz * 0.99);
    }

    @Override
    public float engineVolume() {
        return visibleThrottle() * 2.0F;
    }

    @Override
    public float enginePitch() {
        return visibleThrottle() * 0.5F;
    }
}
