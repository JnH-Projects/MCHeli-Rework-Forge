package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A tow chain, {@code MCH_EntityChain}: it hangs above the vehicle that pulls and drags the towed
 * one along, letting it drift up to the chain's length away and never more than two blocks past
 * that. It goes when either end does.
 */
public class ChainEntity extends Entity {
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 15;
    /** Ticks a chain loaded from disk waits for both ends to load before it gives up. */
    private static final int SEARCH_TICKS = 50;

    private static final EntityDataAccessor<Integer> DATA_TOW = SynchedEntityData.defineId(ChainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOWED = SynchedEntityData.defineId(ChainEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID towId;
    private @Nullable UUID towedId;
    private @Nullable Entity tow;
    private @Nullable Entity towed;
    private int length = 4;
    private int searchTicks = SEARCH_TICKS;

    public ChainEntity(EntityType<? extends ChainEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Chains {@code towed} to {@code tow}, at the distance they are apart now. Server only. */
    public static ChainEntity connect(Level level, Entity towed, Entity tow) {
        ChainEntity chain = new ChainEntity(ModEntities.CHAIN.get(), level);
        chain.setPos(tow.position().add(towed.position()).scale(0.5));
        chain.length = Mth.clamp((int) towed.distanceTo(tow), MIN_LENGTH, MAX_LENGTH);
        chain.attach(towed, tow);
        level.addFreshEntity(chain);
        ModSounds.playAt(chain.tow, "chain_ct", 1.0F, 1.0F);
        return chain;
    }

    /** Whether {@code entity} can pull the other end: only vehicles can. */
    public static boolean canTow(Entity entity) {
        return entity instanceof AircraftEntity;
    }

    /**
     * Only a vehicle pulls. Hooked the other way round, a container would hold a helicopter down,
     * so the ends swap whichever order they were clicked in (and chains saved that way are fixed
     * when they load).
     */
    private void attach(Entity towed, Entity tow) {
        if (!canTow(tow) && canTow(towed)) {
            Entity swap = tow;
            tow = towed;
            towed = swap;
        }
        this.towed = towed;
        this.tow = tow;
        this.towedId = towed.getUUID();
        this.towId = tow.getUUID();
        entityData.set(DATA_TOWED, towed.getId());
        entityData.set(DATA_TOW, tow.getId());
        searchTicks = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TOW, -1);
        builder.define(DATA_TOWED, -1);
    }

    /** The end that pulls; on a client, as far as it has loaded. */
    public @Nullable Entity tow() {
        return level().isClientSide() ? level().getEntity(entityData.get(DATA_TOW)) : tow;
    }

    /** The end that is pulled. */
    public @Nullable Entity towed() {
        return level().isClientSide() ? level().getEntity(entityData.get(DATA_TOWED)) : towed;
    }

    /** Whether {@code entity} is either end of this chain. */
    public boolean holds(Entity entity) {
        return entity == tow() || entity == towed();
    }

    /** Takes the chain off, with the sound of it coming loose. Server only. */
    public void disconnect() {
        ModSounds.playAt(this, "chain_ct", 1.0F, 1.0F);
        discard();
    }

    /** The chain on {@code entity}, at either end, if there is one within reach. */
    public static @Nullable ChainEntity on(Entity entity) {
        for (ChainEntity chain : entity.level().getEntitiesOfClass(ChainEntity.class, entity.getBoundingBox().inflate(25.0))) {
            if (chain.holds(entity)) {
                return chain;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (searchTicks > 0) {
            searchTicks--;
            Entity foundTowed = towedId != null ? server.getEntity(towedId) : null;
            Entity foundTow = towId != null ? server.getEntity(towId) : null;
            if (foundTowed != null && foundTow != null) {
                attach(foundTowed, foundTow);
            }
            return;
        }
        if (tow == null || towed == null || tow.isRemoved() || towed.isRemoved() || tow == towed) {
            disconnect();
            return;
        }
        towed.resetFallDistance();
        setPos(tow.getX(), tow.getY() + 2.0, tow.getZ());
        pull(tow, towed);
    }

    /** {@code updateTowingEntityPosRot}: turns the towed end to face along the chain and drags it in. */
    private void pull(Entity tow, Entity towed) {
        Vec3 d = towed.position().subtract(tow.position());
        double dist = d.length();
        if (dist <= length) {
            return;
        }
        towed.setYRot((float) Math.toDegrees(Math.atan2(d.z, d.x)) + 90.0F);
        double slack = dist - length;
        towed.setDeltaMovement(towed.getDeltaMovement().subtract(d.scale(0.001 / slack)));
        double max = length + 2.0;
        if (dist > max) {
            Vec3 at = tow.position().add(d.scale(max / dist));
            towed.setPos(at.x, at.y, at.z);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        towId = input.hasUUID("Tow") ? input.getUUID("Tow") : null;
        towedId = input.hasUUID("Towed") ? input.getUUID("Towed") : null;
        length = Mth.clamp((input.contains("Length") ? input.getInt("Length") : 4), MIN_LENGTH, MAX_LENGTH);
        searchTicks = SEARCH_TICKS;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        if (towId != null && towedId != null) {
            output.putUUID("Tow", towId);
            output.putUUID("Towed", towedId);
        }
        output.putInt("Length", length);
    }
}
