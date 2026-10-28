package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** An unguided rocket, {@code MCH_EntityRocket}, which may burst into bomblets. */
public class RocketEntity extends ProjectileEntity {
    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean boostsAcceleration() {
        return !isBomblet();
    }

    @Override
    public void tick() {
        super.tick();
        tickBomblets();
        WeaponDefinition w = weapon();
        if (!isBomblet() && w != null && !w.disableSmoke) {
            trail(w.trajectoryParticle, 3, 5.0F * w.smokeSize * 0.5F);
        }
    }

    @Override
    protected void sprinkleBomblet() {
        WeaponDefinition w = weapon();
        Vec3 m = getDeltaMovement();
        RocketEntity child = spawnChild(ModEntities.ROCKET.get(), m.x, m.y, m.z, getYRot(), getXRot());
        if (child == null || w == null) {
            return;
        }
        float spread = w.bombletDiff;
        child.setDeltaMovement(child.getDeltaMovement().add(
                (random.nextFloat() - 0.5) * spread, (random.nextFloat() - 0.5) * spread, (random.nextFloat() - 0.5) * spread));
        child.setBomblet();
        level().addFreshEntity(child);
    }

    @Override
    public String defaultModel() {
        return "rocket";
    }
}
