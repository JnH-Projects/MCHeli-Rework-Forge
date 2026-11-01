package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.registry.ModItems;
import com.jnhprojects.mcheli.weapon.CasStrike;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The ground laser target designator, {@code MCH_EntityGLTD}: a tripod a player sits behind. Its
 * sight looks up to 70 degrees either side of where it was set up, zooms and has night vision,
 * and marking a spot calls in an A-10 for three gun runs over it.
 */
public class GltdEntity extends Entity {
    /** How far the sight turns from the tripod's heading, either way, and up or down. */
    public static final float VIEW_LIMIT = 70.0F;
    /** The CAS weapon the A-10s fire; the original hard coded it too. */
    private static final String WEAPON = "a10gau8";
    /** Added to the weapon's own {@code Delay} between two calls. */
    private static final int EXTRA_WAIT = 150;
    private static final double SIGHT_RANGE = 300.0;

    private static final EntityDataAccessor<Integer> DATA_WAIT = SynchedEntityData.defineId(GltdEntity.class, EntityDataSerializers.INT);

    private final CasStrike strike = new CasStrike(WEAPON);
    private boolean nightVision;

    public GltdEntity(EntityType<? extends GltdEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WAIT, 0);
    }

    /** Ticks until the next A-10 can be called; the sight shows it. */
    public int waitTicks() {
        return entityData.get(DATA_WAIT);
    }

    /** The eye of the sight, a little in front of and above the tripod. */
    public Vec3 sightPosition(float partialTicks) {
        float yaw = getYRot(partialTicks) * Mth.DEG_TO_RAD;
        Vec3 pos = getPosition(partialTicks);
        return new Vec3(pos.x - Mth.sin(yaw) * 0.6, pos.y + 0.7, pos.z + Mth.cos(yaw) * 0.6);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        Vec3 m = getDeltaMovement().add(0.0, -0.04, 0.0);
        if (onGround()) {
            m = m.scale(0.5);
        }
        move(MoverType.SELF, m);
        setDeltaMovement(m.x * 0.99, m.y * 0.95, m.z * 0.99);

        if (waitTicks() > 0) {
            entityData.set(DATA_WAIT, waitTicks() - 1);
        }
        WeaponDefinition def = MCHeli.content().weapon(WEAPON);
        if (def != null && level() instanceof ServerLevel server) {
            strike.tick(server, def);
        }
        Entity viewer = getFirstPassenger();
        if (viewer == null) {
            nightVision = false;
        } else if (nightVision) {
            NightVision.topUp(viewer);
        }
    }

    /**
     * {@code useCurrentWeapon}: marks the block {@code player} looks at through the sight and calls
     * the A-10 on it. Server only.
     *
     * @return whether the strike was called; the sight may be waiting, or see nothing in range
     */
    public boolean callStrike(ServerPlayer player) {
        WeaponDefinition def = MCHeli.content().weapon(WEAPON);
        if (getFirstPassenger() != player || waitTicks() > 0 || def == null || !(level() instanceof ServerLevel server)) {
            return false;
        }
        Vec3 from = sightPosition(1.0F);
        Vec3 to = from.add(player.getLookAngle().scale(SIGHT_RANGE));
        BlockHitResult hit = server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        strike.start(hit.getLocation(), player.getYRot(), this, player);
        entityData.set(DATA_WAIT, def.delay + EXTRA_WAIT);
        ModSounds.playAt(server, getX(), getY(), getZ(), "gltd", 0.5F, 1.0F);
        if (def.sound != null) {
            ModSounds.playTo(player, def.sound, 1.0F, 1.0F);
        }
        return true;
    }

    public void setNightVision(Player player, boolean on) {
        if (getFirstPassenger() != player) {
            return;
        }
        nightVision = on;
        if (!on) {
            NightVision.end(player);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive() || isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide()) {
            player.setYRot(getYRot());
            player.setXRot(0.0F);
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && nightVision) {
            NightVision.end(passenger);
            nightVision = false;
        }
    }

    /** The viewer sits behind the tripod. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, -0.3, -0.5).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    /** The original clamped the viewer's head the same way every tick. */
    @Override
    public void onPassengerTurned(Entity passenger) {
        float yaw = getYRot() + Mth.clamp(Mth.wrapDegrees(passenger.getYRot() - getYRot()), -VIEW_LIMIT, VIEW_LIMIT);
        passenger.setYRot(yaw);
        passenger.setYHeadRot(yaw);
        passenger.setXRot(Mth.clamp(passenger.getXRot(), -VIEW_LIMIT, VIEW_LIMIT));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isInvulnerableToBase(source) || isRemoved()) {
            return false;
        }
        boolean creative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
        ejectPassengers();
        if (!creative) {
            spawnAtLocation(level, ModItems.GLTD.get());
        }
        ModSounds.playAt(level, getX(), getY(), getZ(), "hit", 1.0F, 1.0F);
        discard();
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(ModItems.GLTD.get());
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
