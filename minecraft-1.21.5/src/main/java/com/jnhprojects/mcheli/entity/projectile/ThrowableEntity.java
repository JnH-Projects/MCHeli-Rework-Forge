package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.throwable.ThrowableDefinition;
import com.jnhprojects.mcheli.entity.VehicleInterpolation;
import com.jnhprojects.mcheli.particle.SmokeParticleOptions;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.weapon.Explosions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A thrown grenade, {@code MCH_EntityThrowable}. It flies under its definition's gravity, bounces
 * off blocks losing speed, and when its fuse runs out either explodes or, for a smoke grenade,
 * pours out coloured smoke until it disappears.
 */
public class ThrowableEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_NAME = SynchedEntityData.defineId(ThrowableEntity.class, EntityDataSerializers.STRING);

    private final InterpolationHandler interpolation = new VehicleInterpolation(this);
    private @Nullable Entity thrower;
    private @Nullable ThrowableDefinition definition;
    private int definitionGeneration = -1;

    public ThrowableEntity(EntityType<? extends ThrowableEntity> type, Level level) {
        super(type, level);
    }

    /**
     * Throws {@code def} from {@code thrower}'s eyes the way it looks.
     *
     * @param strength 0.2 for a flick up to 1.4 for a full draw, the original's acceleration
     */
    public static ThrowableEntity thrown(Level level, Entity thrower, ThrowableDefinition def, float strength) {
        ThrowableEntity entity = new ThrowableEntity(ModEntities.THROWABLE.get(), level);
        Vec3 look = thrower.getLookAngle();
        entity.setPos(thrower.getX() + look.x * 0.3, thrower.getEyeY() - 0.1, thrower.getZ() + look.z * 0.3);
        entity.setYRot(thrower.getYRot());
        entity.setDeltaMovement(look.scale(1.5 * strength).add(thrower.getDeltaMovement()));
        entity.thrower = thrower;
        entity.setDefinition(def);
        return entity;
    }

    /** One fired from a dispenser: no thrower, straight along {@code direction}. */
    public static ThrowableEntity dispensed(Level level, Vec3 at, Vec3 direction, ThrowableDefinition def) {
        ThrowableEntity entity = new ThrowableEntity(ModEntities.THROWABLE.get(), level);
        entity.setPos(at);
        entity.setDeltaMovement(direction.normalize().scale(def.dispenseAcceleration));
        entity.setDefinition(def);
        return entity;
    }

    private void setDefinition(ThrowableDefinition def) {
        entityData.set(DATA_NAME, def.name());
        definition = def;
        definitionGeneration = MCHeli.content().generation();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_NAME, "");
    }

    /** The definition by name, looked up again after a content reload. */
    public @Nullable ThrowableDefinition definition() {
        if (definitionGeneration != MCHeli.content().generation()) {
            definition = MCHeli.content().throwable(entityData.get(DATA_NAME));
            definitionGeneration = MCHeli.content().generation();
        }
        return definition;
    }

    public String definitionName() {
        return entityData.get(DATA_NAME);
    }

    @Override
    public void tick() {
        super.tick();
        interpolation.interpolate();
        ThrowableDefinition def = definition();
        if (def == null) {
            // The original gave a missing definition ten ticks to arrive.
            if (tickCount > 10 && !level().isClientSide()) {
                discard();
            }
            return;
        }
        if (level().isClientSide()) {
            if (def.smokes() && tickCount >= def.timeFuse) {
                smoke(def);
            }
            return;
        }
        if (tickCount >= def.timeFuse && !def.smokes()) {
            Explosions.explode((ServerLevel) level(), getX(), getY(), getZ(), new Explosions.Params(this, thrower,
                    def.explosion, def.explosion, false, true, 0, isInWater(), null));
            discard();
            return;
        }
        if (tickCount >= def.aliveTime) {
            discard();
            return;
        }
        fly(def);
    }

    /** {@code onUpdate} and {@code boundBullet}: gravity of its own, and a bounce off whatever it hits. */
    private void fly(ThrowableDefinition def) {
        boolean water = isInWater();
        Vec3 m = getDeltaMovement().add(0.0, water ? def.gravityInWater : def.gravity, 0.0);
        Vec3 from = position();
        move(MoverType.SELF, m);
        // Whichever axis came up short hit something.
        Vec3 moved = position().subtract(from);
        double mx = blocked(moved.x, m.x) ? -m.x * def.bound : m.x;
        double my = m.y;
        double mz = blocked(moved.z, m.z) ? -m.z * def.bound : m.z;
        if (blocked(moved.y, m.y)) {
            mx *= 0.9;
            mz *= 0.9;
            // Off the floor or the ceiling it keeps a share of its speed, unless that would be a crawl.
            my = Math.abs(my) > 0.05 ? -my * def.bound : 0.0;
        }
        double drag = water ? 0.8 : 0.99;
        setDeltaMovement(mx * drag, my * drag, mz * drag);
    }

    private static boolean blocked(double moved, double wanted) {
        return Math.abs(moved) < Math.abs(wanted) - 1.0E-6;
    }

    private void smoke(ThrowableDefinition def) {
        int color = def.smokeColor;
        for (int i = 0; i < def.smokeNum; i++) {
            // A little noise per puff, keeping greys grey.
            float r = ARGB.redFloat(color) * 0.9F + random.nextFloat() * 0.1F;
            float g = ARGB.greenFloat(color) * 0.9F + random.nextFloat() * 0.1F;
            float b = ARGB.blueFloat(color) * 0.9F + random.nextFloat() * 0.1F;
            if (ARGB.red(color) == ARGB.green(color)) {
                g = r;
            }
            if (ARGB.red(color) == ARGB.blue(color)) {
                b = r;
            }
            if (ARGB.green(color) == ARGB.blue(color)) {
                b = g;
            }
            float size = def.smokeSize + random.nextFloat() * def.smokeSize / 3.0F;
            level().addAlwaysVisibleParticle(new SmokeParticleOptions(ARGB.colorFromFloat(1.0F, r, g, b) & 0xFFFFFF, size), true,
                    getX(), getY() + 0.5, getZ(),
                    def.smokeVelocityHorizontal * (random.nextFloat() - 0.5F) * 0.2,
                    def.smokeVelocityVertical * random.nextFloat() * 0.2,
                    def.smokeVelocityHorizontal * (random.nextFloat() - 0.5F) * 0.2);
        }
    }

    /** The server moves it; clients ease towards each position it sends. */
    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
