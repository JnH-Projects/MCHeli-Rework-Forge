package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.ServerConfig;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.jnhprojects.mcheli.menu.UavStationMenu;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The UAV station and the portable UAV controller, {@code MCH_EntityUavStation}. A player sits at
 * it and puts a UAV in its slot: the drone appears at the station's launch offset and the player
 * flies it from there. The portable controller only launches small UAVs and its lid opens and
 * closes with Shift and right-click.
 *
 * <p>The original kept the player at the station and had the drone read its pilot from there.
 * Here the pilot takes the drone's reserved seat for as long as they fly it, hidden and unhurt,
 * so the drone uses the same controls, cameras, HUD and weapons as every other vehicle. Leaving
 * the drone, or losing it, puts the pilot back beside the station.
 */
public class UavStationEntity extends Entity {
    public static final int STATION = 1;
    public static final int PORTABLE = 2;
    /** Further out than this, measured flat, the link breaks and the drone is lost, as in the original. */
    public static final double RANGE = 123.0;
    /** The launch offset goes up to this far from the station along each axis. */
    public static final int MAX_OFFSET = 50;

    private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(UavStationEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_OPEN = SynchedEntityData.defineId(UavStationEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> DATA_OFFSET = SynchedEntityData.defineId(UavStationEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> DATA_LINKED = SynchedEntityData.defineId(UavStationEntity.class, EntityDataSerializers.BOOLEAN);

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private final SimpleContainer slot = new SimpleContainer(1);
    private @Nullable UUID lastUav;
    /** The lid of the portable controller, 0 shut to 1 open. */
    private float cover;
    private float previousCover;

    public UavStationEntity(EntityType<? extends UavStationEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, (byte) STATION);
        builder.define(DATA_OPEN, true);
        builder.define(DATA_OFFSET, new BlockPos(12, 2, 12));
        builder.define(DATA_LINKED, false);
    }

    public int kind() {
        return entityData.get(DATA_KIND);
    }

    public void setKind(int kind) {
        entityData.set(DATA_KIND, (byte) (kind == PORTABLE ? PORTABLE : STATION));
    }

    public boolean isOpen() {
        return entityData.get(DATA_OPEN);
    }

    /** Where a launched drone appears, relative to the station. */
    public BlockPos offset() {
        return entityData.get(DATA_OFFSET);
    }

    public void setOffset(BlockPos offset) {
        entityData.set(DATA_OFFSET, new BlockPos(Mth.clamp(offset.getX(), -MAX_OFFSET, MAX_OFFSET),
                Mth.clamp(offset.getY(), -MAX_OFFSET, MAX_OFFSET), Mth.clamp(offset.getZ(), -MAX_OFFSET, MAX_OFFSET)));
    }

    /** Whether a drone is flown from here right now; the screen lights up. */
    public boolean isLinked() {
        return entityData.get(DATA_LINKED);
    }

    public void setLinked(boolean linked) {
        entityData.set(DATA_LINKED, linked);
    }

    public SimpleContainer slot() {
        return slot;
    }

    public float cover(float partialTicks) {
        return Mth.lerp(partialTicks, previousCover, cover);
    }

    /**
     * {@code initUavPostion}: the first launch offset is twelve blocks out on the diagonal the
     * station faces, two up.
     */
    public void initOffset() {
        int quarter = (int) (Mth.positiveModulo(getYRot() + 45.0F, 360.0F) / 90.0F) % 4;
        int x = quarter == 0 || quarter == 3 ? 12 : -12;
        int z = quarter == 0 || quarter == 1 ? 12 : -12;
        setOffset(new BlockPos(x, 2, z));
    }

    @Override
    public void tick() {
        super.tick();
        interpolation.interpolate();
        previousCover = cover;
        cover = Mth.clamp(cover + (isOpen() ? 0.1F : -0.1F), 0.0F, 1.0F);
        if (level().isClientSide()) {
            return;
        }
        Vec3 m = getDeltaMovement().add(0.0, -0.03, 0.0);
        move(MoverType.SELF, new Vec3(0.0, m.y, 0.0));
        setDeltaMovement(0.0, onGround() ? 0.0 : m.y * 0.96, 0.0);

        Entity user = getFirstPassenger();
        if (user instanceof ServerPlayer player && !slot.getItem(0).isEmpty()) {
            launch(player, slot.getItem(0));
        }
    }

    /** What the slot holds, if it is a vehicle at all. */
    public static @Nullable AircraftDefinition definitionOf(ItemStack stack) {
        String name = AircraftItem.vehicleOf(stack);
        return name != null ? MCHeli.content().any(name) : null;
    }

    /** Whether this station can put up the vehicle in {@code stack}. */
    public boolean accepts(@Nullable AircraftDefinition def) {
        return def != null && def.uav && (kind() != PORTABLE || def.smallUav);
    }

    /** {@code handleItem}: puts up the drone in the slot and hands it to {@code player}. */
    private void launch(ServerPlayer player, ItemStack stack) {
        AircraftDefinition def = definitionOf(stack);
        if (!accepts(def) || !(level() instanceof ServerLevel server)) {
            return;
        }
        if (ServerConfig.isDisabled(def.name())) {
            player.sendOverlayMessage(Component.translatable("message.mcheli.vehicle.disabled", stack.getHoverName()));
            return;
        }
        AircraftEntity uav = ModEntities.typeFor(def).create(server, EntitySpawnReason.TRIGGERED);
        if (uav == null) {
            return;
        }
        BlockPos o = offset();
        double y = Math.max(getY() + o.getY(), server.getMinY() + 2.0);
        uav.setType(def);
        uav.snapTo(getX() + o.getX(), y, getZ() + o.getZ(), getYRot() - 180.0F, 0.0F);
        if (!server.noCollision(uav, uav.getBoundingBox().deflate(0.1))) {
            player.sendOverlayMessage(Component.translatable("message.mcheli.uav.blocked"));
            slot.setItem(0, ItemStack.EMPTY);
            Containers.dropItemStack(server, player.getX(), player.getY(), player.getZ(), stack);
            return;
        }
        slot.setItem(0, ItemStack.EMPTY);
        uav.setFuelLevel(def.targetDrone ? uav.maxFuel() : (int) (uav.maxFuel() * 0.05F));
        server.addFreshEntity(uav);
        if (!def.targetDrone) {
            player.closeContainer();
            control(player, uav);
        }
    }

    /** {@code controlLastAircraft}: takes the last drone flown from here again. */
    public boolean continueControl(ServerPlayer player) {
        AircraftEntity uav = lastUav();
        if (uav == null || getFirstPassenger() != player || !uav.canTakeRemotePilot()) {
            return false;
        }
        player.closeContainer();
        return control(player, uav);
    }

    public @Nullable AircraftEntity lastUav() {
        if (lastUav == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        Entity e = server.getEntity(lastUav);
        return e instanceof AircraftEntity uav && uav.isAlive() && !uav.isDestroyed() && inRange(uav) ? uav : null;
    }

    public boolean inRange(Entity uav) {
        double dx = uav.getX() - getX();
        double dz = uav.getZ() - getZ();
        return dx * dx + dz * dz <= RANGE * RANGE;
    }

    private boolean control(ServerPlayer player, AircraftEntity uav) {
        player.stopRiding();
        if (!uav.takeRemotePilot(player, this)) {
            return false;
        }
        lastUav = uav.getUUID();
        setLinked(true);
        player.setYRot(uav.getYRot());
        return true;
    }

    /** Where a pilot comes back to when the drone is left or lost: the seat in front of the station. */
    public Vec3 returnPosition() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(getX() - Mth.sin(yaw) * 0.9, getY(), getZ() + Mth.cos(yaw) * 0.9);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (isVehicle()) {
            return InteractionResult.PASS;
        }
        if (kind() == PORTABLE && player.isSecondaryUseActive()) {
            if (!level().isClientSide()) {
                entityData.set(DATA_OPEN, !isOpen());
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() || kind() == PORTABLE && !isOpen()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            player.setYRot(getYRot() - 180.0F);
            if (player.startRiding(this)) {
                server.openMenu(new SimpleMenuProvider((id, inventory, p) -> new UavStationMenu(id, inventory, this),
                        Component.translatable(kind() == PORTABLE ? "item.mcheli.uav_station2" : "item.mcheli.uav_station")));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The original sat the player down in front of the console, lower for the portable one. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double y = kind() == PORTABLE ? -0.6 : 0.35;
        return new Vec3(0.0, y, 0.9).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isInvulnerableToBase(source) || isRemoved() || damage <= 0.0F) {
            return false;
        }
        boolean creative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
        ejectPassengers();
        AircraftEntity uav = lastUav();
        if (uav != null) {
            uav.releaseRemotePilot();
        }
        Containers.dropContents(level, blockPosition(), slot);
        if (!creative) {
            spawnAtLocation(level, item());
        }
        ModSounds.playAt(level, getX(), getY(), getZ(), source.getEntity() instanceof Player ? "hit" : "helidmg", 1.0F, 1.0F);
        discard();
        return true;
    }

    private Item item() {
        return kind() == PORTABLE ? ModItems.UAV_CONTROLLER.get() : ModItems.UAV_STATION.get();
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(item());
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setKind(input.getIntOr("Kind", STATION));
        entityData.set(DATA_OPEN, input.getBooleanOr("Open", true));
        setOffset(new BlockPos(input.getIntOr("UavX", 12), input.getIntOr("UavY", 2), input.getIntOr("UavZ", 12)));
        lastUav = input.read("LastUav", UUIDUtil.CODEC).orElse(null);
        slot.setItem(0, input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Kind", kind());
        output.putBoolean("Open", isOpen());
        BlockPos o = offset();
        output.putInt("UavX", o.getX());
        output.putInt("UavY", o.getY());
        output.putInt("UavZ", o.getZ());
        if (lastUav != null) {
            output.store("LastUav", UUIDUtil.CODEC, lastUav);
        }
        if (!slot.getItem(0).isEmpty()) {
            output.store("Item", ItemStack.CODEC, slot.getItem(0));
        }
    }
}
