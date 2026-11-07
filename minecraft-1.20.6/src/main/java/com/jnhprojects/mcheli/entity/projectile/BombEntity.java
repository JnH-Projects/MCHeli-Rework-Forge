package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A free fall bomb, {@code MCH_EntityBomb}. Cluster bombs split into bomblets. */
public class BombEntity extends ProjectileEntity {
    public BombEntity(EntityType<? extends BombEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        WeaponDefinition w = weapon();
        if (!level().isClientSide() && w != null && !isRemoved()) {
            Vec3 m = getDeltaMovement();
            m = new Vec3(m.x * 0.999, m.y, m.z * 0.999);
            if (isInWater()) {
                m = m.scale(w.velocityInWater);
            }
            setDeltaMovement(m);
            float fuse = w.proximityFuseDist;
            if (fuse > 0.1F && age() % 10 == 0) {
                for (Entity entity : level().getEntities(this, getBoundingBox().inflate(fuse), this::canHit)) {
                    if (entity instanceof LivingEntity) {
                        impact(new Impact(position(), null, blockPosition(), Direction.UP), 1.0F);
                        break;
                    }
                }
            }
        }
        tickBomblets();
    }

    @Override
    protected void sprinkleBomblet() {
        WeaponDefinition w = weapon();
        Vec3 m = getDeltaMovement();
        BombEntity child = spawnChild(ModEntities.BOMB.get(), m.x, m.y, m.z, random.nextInt(360), 0.0F);
        if (child == null || w == null) {
            return;
        }
        float spread = w.bombletDiff;
        child.setDeltaMovement(m.x + (random.nextFloat() - 0.5F) * spread,
                m.y / 2.0 + (random.nextFloat() - 0.5F) * spread / 2.0F,
                m.z + (random.nextFloat() - 0.5F) * spread);
        child.setBomblet();
        level().addFreshEntity(child);
    }

    /** A cluster bomb that has not split yet is not drawn when it splits right away. */
    public boolean isVisible() {
        WeaponDefinition w = weapon();
        return w == null || isBomblet() || w.bomblet <= 0 || w.bombletSTime > 0;
    }

    @Override
    public String defaultModel() {
        return "bomb";
    }
}
