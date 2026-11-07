package com.jnhprojects.mcheli.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.phys.Vec3;

/**
 * Eases server position updates in, and keeps the target when one carries only a rotation. On this
 * version the client answers those by interpolating to where the entity is right now, which would
 * leave a turning vehicle short of its real position until the next full sync.
 */
public final class VehicleInterpolation extends InterpolationHandler {
    private final Entity entity;

    public VehicleInterpolation(Entity entity) {
        super(entity, 3);
        this.entity = entity;
    }

    @Override
    public void interpolateTo(Vec3 position, float yRot, float xRot) {
        boolean rotationOnly = hasActiveInterpolation() && position.equals(entity.position());
        super.interpolateTo(rotationOnly ? position() : position, yRot, xRot);
    }
}
