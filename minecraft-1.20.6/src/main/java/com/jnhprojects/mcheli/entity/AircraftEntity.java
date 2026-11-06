package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.ServerConfig;
import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.BoundingBoxDefinition;
import com.jnhprojects.mcheli.content.aircraft.RackDefinition;
import com.jnhprojects.mcheli.content.aircraft.RideRack;
import com.jnhprojects.mcheli.content.aircraft.SeatDefinition;
import com.jnhprojects.mcheli.entity.projectile.ProjectileEntity;
import com.jnhprojects.mcheli.entity.projectile.TvMissileEntity;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.jnhprojects.mcheli.item.ChainItem;
import com.jnhprojects.mcheli.item.FuelItem;
import com.jnhprojects.mcheli.item.WrenchItem;
import com.jnhprojects.mcheli.menu.VehicleMenu;
import com.jnhprojects.mcheli.particle.GlowParticleOptions;
import com.jnhprojects.mcheli.physics.AircraftRotation;
import com.jnhprojects.mcheli.physics.CrashSpin;
import com.jnhprojects.mcheli.physics.GroundImpact;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.registry.ModItems;
import com.jnhprojects.mcheli.supply.SupplyRules;
import com.jnhprojects.mcheli.weapon.Explosions;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Behaviour shared by every MCHeli vehicle: which definition it uses, hit points, seats, moving
 * parts and the pilot authority model.
 *
 * <p>Movement is simulated wherever {@link #isControlledByLocalInstance()} is true. With a player
 * at the controls that is the pilot's client, exactly like vanilla boats, and the server checks
 * the result through the regular vehicle move packet. Without a pilot the server simulates the
 * vehicle on its own. Roll, throttle and key state are not part of the vanilla packet, so the
 * pilot sends them separately and the server republishes them through synched data.
 *
 * <p>Seats are explicit. Vanilla only knows an ordered passenger list, so the seat each passenger
 * occupies is kept in a synched seat map of entity ids. Seat zero is the pilot and decides who
 * controls the vehicle; crew seats follow, then cargo racks.
 */
public abstract class AircraftEntity extends Entity {
    // VehicleEntity's hurt wobble. Vanilla 1.20.6 keeps that class package private where it matters,
    // so vehicles extend Entity and carry their own.
    private static final EntityDataAccessor<Integer> DATA_HURT_TIME = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HURT_DIR = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_TYPE = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TEXTURE = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_HP = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_THROTTLE = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_REVERSE = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROLL = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_HOVERING = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GUNNER_MODE = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_INPUT = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_PARTS = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_SEATS = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_WEAPON_SELECTION = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_WEAPON_USE = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TV_MISSILE = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FLARE = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SINKING = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FUEL = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    /** The UAV station a drone is flown from, by entity id, or 0. */
    private static final EntityDataAccessor<Integer> DATA_STATION = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_REPELLING = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SEARCH_LIGHT = SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.BOOLEAN);

    /** Marks a player flying a drone from a station, so a stale hide can be undone at login. */
    public static final String REMOTE_PILOT_TAG = "mcheli_remote_pilot";

    // Bits of DATA_PARTS, the same assignment the original used for its part status word.
    protected static final int PART_CANOPY = 0;
    protected static final int PART_NOZZLE = 1;
    protected static final int PART_LANDING_GEAR = 2;
    protected static final int PART_WING = 3;
    protected static final int PART_HATCH = 4;

    private static final int EMPTY = -1;
    /** How far above its hit box the original drew a vehicle's model; see {@link #waterDepth}. */
    private static final float LEGACY_MODEL_LIFT = 0.35F;
    /** How fast a cooked-off boat goes under, in blocks per tick (about 0.4 blocks a second). */
    private static final double SINK_SPEED = 0.02;
    /** The bow-up pitch and the list a sinking boat settles into, in degrees. */
    private static final float SINK_PITCH = 14.0F;
    private static final float SINK_LIST = 12.0F;

    protected final ControlInput input = new ControlInput();
    public final AnimationState animation = new AnimationState();

    /** Throttle in [0, 1] as seen by the simulating side. Other sides read {@link #DATA_THROTTLE}. */
    protected double throttle;
    /** Reverse throttle in [0, 0.6] for vehicles with {@code EnableBack}. */
    protected float reverse;
    protected float roll;
    public float rollO;
    protected int modeSwitchCooldown;
    private int ticksAlive;

    private @Nullable AircraftDefinition cachedDefinition;
    private String cachedName = "";
    private int cachedGeneration;

    protected @Nullable PartState canopy;
    protected @Nullable PartState landingGear;
    protected @Nullable PartState hatch;
    private final List<PartState> parts = new ArrayList<>();

    private int[] seatIds = new int[0];
    private int pendingSeat = EMPTY;
    private final Map<UUID, Integer> savedSeats = new HashMap<>();
    private final Map<Integer, Integer> lastSeats = new HashMap<>();
    /** The station a drone is flown from, and where its pilot goes back to. Server only. */
    private @Nullable UUID station;
    private @Nullable Vec3 stationReturn;
    /** Remote pilots leaving this tick, by entity id, with where they come back down. */
    private final Map<Integer, Vec3> remoteReturns = new HashMap<>();
    /** Where a workshop test flight started, or {@code null} for an ordinary vehicle. Server only. */
    private @Nullable Vec3 testFlightStart;
    private boolean testFlightOver;
    /** How far the ropes hang below their hooks, as a negative number; both sides let them out. */
    private float ropeLength;
    private int repellingTicks;
    private int lastRope;
    /** Passengers are leaving one by one under parachutes. Server only. */
    private boolean droppingTroops;

    private @Nullable VehicleWeapons weapons;
    private int @Nullable [] savedAmmo;
    private @Nullable TvMissileEntity tvMissile;

    // Damage model
    /** Damage factor of the extra hit box the last projectile ray struck, {@code lastBBDamageFactor}. */
    private float hitBoxFactor = 1.0F;
    private int timeSinceHit;
    /** The fuel load from which an aircraft counts as heavy: the H8K flying boat and up. */
    private static final int HEAVY_FUEL = 3000;
    /** How long a wreck stays, burning, before it disappears: 30 seconds. */
    private static final int WRECK_TICKS = 600;
    /** Ticks a server position update is eased in over. */
    private static final int LERP_STEPS = 3;
    /** The wreck fire dies down over the last five seconds. */
    private static final int WRECK_FADE_TICKS = 100;
    /** Ticks until a destroyed wreck disappears; above zero means destroyed. Server only. */
    private int despawnCount;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    /** Ticks the client has seen this vehicle as a wreck, for the fire to die down on time. */
    private int wreckTicks;
    /** Ticks since this aircraft was shot down, for the crash spin. Counted on both sides. */
    private int spinTicks;
    private boolean exploded;
    /** Blasts still to come while a wrecked boat cooks off, and ticks until the next one. */
    private int cookOffBlasts;
    private int cookOffDelay;
    private @Nullable Entity cookOffCause;
    private int alertCount;

    /** Fuel burnt but not yet taken from the tank, which only holds whole units. Server only. */
    private double fuelBurnt;
    /** Ticks a fuel truck nearby keeps this vehicle from burning fuel. Server only. */
    private int fuelSuppliedCount;
    /** One bit per seat whose occupant has night vision on. Server only, not saved. */
    private int nightVisionSeats;
    /** Fuel and parachute slots and the cargo chest. Only the server's copy holds anything. */
    private final VehicleInventory inventory = new VehicleInventory(this);

    // Crashes, measured from position so they work whichever side simulates the vehicle.
    /** Blocks per tick into terrain, sideways, that count as a crash: about 25 m/s. */
    private static final double TERRAIN_CRASH_SPEED = 1.25;
    /**
     * How far a hull reaches past its main box, at most, when looking for neighbours: other
     * vehicles are found by their main box, and the longest hulls stretch about this far from it.
     */
    private static final double HULL_REACH = 24.0;
    /** Closing speed between two vehicles that counts as a crash: about 10 m/s. */
    private static final double VEHICLE_CRASH_SPEED = 0.5;
    /** Highest point since a ground vehicle left the ground, or NaN while it is on it. */
    private double airborneFromY = Double.NaN;
    private @Nullable Vec3 lastTickPos;
    private Vec3 lastVelocity = Vec3.ZERO;
    private int collisionCooldown;

    protected AircraftEntity(EntityType<? extends AircraftEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HURT_TIME, 0);
        builder.define(DATA_HURT_DIR, 1);
        builder.define(DATA_TYPE, "");
        builder.define(DATA_TEXTURE, "");
        builder.define(DATA_HP, 0);
        builder.define(DATA_THROTTLE, 0.0F);
        builder.define(DATA_REVERSE, 0.0F);
        builder.define(DATA_ROLL, 0.0F);
        builder.define(DATA_HOVERING, false);
        builder.define(DATA_GUNNER_MODE, false);
        builder.define(DATA_INPUT, (byte) 0);
        builder.define(DATA_PARTS, 0);
        builder.define(DATA_SEATS, "");
        builder.define(DATA_WEAPON_SELECTION, "");
        builder.define(DATA_WEAPON_USE, 0);
        builder.define(DATA_TV_MISSILE, 0);
        builder.define(DATA_FLARE, false);
        builder.define(DATA_SINKING, false);
        builder.define(DATA_FUEL, 0);
        builder.define(DATA_STATION, 0);
        builder.define(DATA_REPELLING, false);
        builder.define(DATA_SEARCH_LIGHT, false);
    }

    // Definition

    /**
     * Looks the definition up by name. Returns {@code null} for vehicles whose content was removed.
     * After a content reload the next call picks up the new definition object.
     */
    public @Nullable AircraftDefinition definition() {
        String name = typeName();
        int generation = MCHeli.content().generation();
        if (!name.equals(cachedName) || generation != cachedGeneration) {
            boolean reloaded = name.equals(cachedName);
            cachedName = name;
            cachedGeneration = generation;
            AircraftDefinition previous = cachedDefinition;
            cachedDefinition = name.isEmpty() ? null : lookup(name);
            if (cachedDefinition != null && cachedDefinition != previous) {
                if (reloaded && weapons != null) {
                    // The same vehicle with new numbers: keep the rounds it carries.
                    savedAmmo = weapons.saveAmmo();
                }
                onDefinitionChanged(cachedDefinition);
            }
        }
        return cachedDefinition;
    }

    /** Finds a definition of this entity's kind. */
    protected abstract @Nullable AircraftDefinition lookup(String name);

    /** Builds the per vehicle state that depends on the definition: seats, parts, animation arrays. */
    protected void onDefinitionChanged(AircraftDefinition def) {
        if (seatIds.length != def.seatAndRackCount()) {
            int[] resized = new int[def.seatAndRackCount()];
            Arrays.fill(resized, EMPTY);
            System.arraycopy(seatIds, 0, resized, 0, Math.min(seatIds.length, resized.length));
            seatIds = resized;
        }
        parts.clear();
        canopy = def.canopies.isEmpty() ? null : addPart(PART_CANOPY, 3.5F, PartState.Sounds.CANOPY);
        landingGear = def.landingGear.isEmpty() ? null : addPart(PART_LANDING_GEAR, 2.5F, PartState.Sounds.LANDING_GEAR);
        hatch = def.hatches.isEmpty() ? null : addPart(PART_HATCH, 1.5F, PartState.Sounds.HATCH);
        animation.resize(def);
        weapons = new VehicleWeapons(this, def);
        if (savedAmmo != null) {
            weapons.loadAmmo(savedAmmo);
            savedAmmo = null;
        }
    }

    protected PartState addPart(int bit, float speed, PartState.Sounds sounds) {
        PartState state = new PartState(this, bit, speed, sounds);
        // A vehicle that comes into view with a part already switched should not animate it.
        state.force(state.status());
        parts.add(state);
        return state;
    }

    /** Sets which vehicle this is. Call once, right after construction, before adding to the level. */
    public void setType(AircraftDefinition definition) {
        entityData.set(DATA_TYPE, definition.name());
        entityData.set(DATA_TEXTURE, definition.textures().getFirst());
        entityData.set(DATA_HP, definition.maxHp);
        entityData.set(DATA_FUEL, definition.maxFuel);
        refreshDimensions();
        definition();
    }

    public String typeName() {
        return entityData.get(DATA_TYPE);
    }

    public String texture() {
        String texture = entityData.get(DATA_TEXTURE);
        return texture.isEmpty() ? typeName() : texture;
    }

    public int hp() {
        return entityData.get(DATA_HP);
    }

    /**
     * {@code repair}: adds hit points up to the maximum. A wreck stays a wreck. Server only.
     *
     * @return whether anything was repaired
     */
    public boolean repair(int amount) {
        AircraftDefinition def = definition();
        if (def == null || isDestroyed() || hp() >= def.maxHp) {
            return false;
        }
        entityData.set(DATA_HP, Math.min(hp() + amount, def.maxHp));
        return true;
    }

    /**
     * {@code switchNextTextureName}: the next paint scheme the definition lists. Server only.
     *
     * @return false when the vehicle has only one
     */
    public boolean nextTexture() {
        AircraftDefinition def = definition();
        if (def == null || def.textures().size() < 2) {
            return false;
        }
        List<String> textures = def.textures();
        int next = (textures.indexOf(texture()) + 1) % textures.size();
        entityData.set(DATA_TEXTURE, textures.get(next));
        return true;
    }

    // Fuel

    public int fuel() {
        return entityData.get(DATA_FUEL);
    }

    public int maxFuel() {
        AircraftDefinition def = definition();
        return def != null ? def.maxFuel : 0;
    }

    /** {@code getFuelP}: how full the tank is, 0 to 1. Vehicles without a tank read as full. */
    public float fuelRatio() {
        int max = maxFuel();
        return max <= 0 ? 1.0F : (float) fuel() / max;
    }

    /** {@code canUseFuel}: a vehicle without a tank never runs dry. */
    public boolean hasFuel() {
        return maxFuel() <= 0 || fuel() > 1;
    }

    /** Sets the tank outright; a drone leaves its station with 5%. Server only. */
    public void setFuelLevel(int fuel) {
        entityData.set(DATA_FUEL, Math.clamp(fuel, 0, Math.max(maxFuel(), 0)));
    }

    /**
     * Pours up to {@code amount} into the tank. Server only.
     *
     * @return how much went in
     */
    public int refuel(int amount) {
        int added = Math.clamp(maxFuel() - fuel(), 0, Math.max(amount, 0));
        if (added > 0) {
            entityData.set(DATA_FUEL, fuel() + added);
        }
        return added;
    }

    // Night vision

    /** A crew member switched night vision on or off. Server only. */
    public void setNightVision(Player player, boolean on) {
        AircraftDefinition def = definition();
        int seat = seatOf(player);
        if (seat < 0 || seat >= 31) {
            return;
        }
        if (on && def != null && def.nightVision && !isDestroyed()) {
            nightVisionSeats |= 1 << seat;
        } else if (hasNightVision(seat)) {
            nightVisionSeats &= ~(1 << seat);
            NightVision.end(player);
        }
    }

    /** Keeps night vision on for everyone in the crew who switched it on, see {@link NightVision}. */
    private void tickNightVision() {
        if (nightVisionSeats == 0) {
            return;
        }
        if (isDestroyed()) {
            for (int seat = 0; seat < seatIds.length; seat++) {
                Entity crew = occupant(seat);
                if (crew != null) {
                    NightVision.end(crew);
                }
            }
            nightVisionSeats = 0;
            return;
        }
        for (int seat = 0; seat < seatIds.length; seat++) {
            Entity crew = occupant(seat);
            if (hasNightVision(seat) && crew != null) {
                NightVision.topUp(crew);
            }
        }
    }

    /** Only the first 31 seats can switch it on; racks and the like come after them anyway. */
    private boolean hasNightVision(int seat) {
        return seat >= 0 && seat < 31 && (nightVisionSeats & 1 << seat) != 0;
    }

    /**
     * {@code updateFuel}: once a second the engine burns its {@code FuelConsumption}, scaled by the
     * throttle and topping out at 70% throttle. Nothing burns next to a fuel truck or with the
     * server's infinite fuel on.
     */
    private void burnFuel(AircraftDefinition def) {
        if (def.maxFuel <= 0 || isDestroyed()) {
            return;
        }
        if (fuelSuppliedCount > 0) {
            fuelSuppliedCount--;
            return;
        }
        if (ticksAlive % 20 != 0 || fuel() <= 1 || throttle <= 0.0 || ServerConfig.infiniteFuel()) {
            return;
        }
        fuelBurnt += Math.min(throttle * 1.4, 1.0) * def.fuelConsumption;
        if (fuelBurnt > 1.0) {
            int whole = (int) fuelBurnt;
            fuelBurnt -= whole;
            entityData.set(DATA_FUEL, Math.max(fuel() - whole, 0));
        }
    }

    /**
     * {@code supplyFuel}: a fuel truck ({@code FuelSupplyRange}) tops up every vehicle within range,
     * 30 units every half second. While it is on the ground it only reaches vehicles that are on
     * the ground too.
     */
    private void supplyFuel(AircraftDefinition def) {
        float range = def.fuelSupplyRange;
        if (range <= 0.0F || ticksAlive % 10 != 0 || isDestroyed()) {
            return;
        }
        for (AircraftEntity other : level().getEntitiesOfClass(AircraftEntity.class, getBoundingBox().inflate(range))) {
            if (other == this) {
                continue;
            }
            if (!onGround() || other.onGround()) {
                other.refuel(30);
            }
            other.fuelSuppliedCount = 40;
        }
    }

    // Inventory and supply

    public VehicleInventory inventory() {
        return inventory;
    }

    /**
     * {@code canSupply}: fuel cans and resupply only work while the vehicle stands on something,
     * within three blocks; a vehicle that cannot float must not be in the water either.
     */
    public boolean canSupply() {
        AircraftDefinition def = definition();
        return def != null && hasBlockBelow(1, 3, false) && (def.floats || !isInWater());
    }

    /** {@code openGui}: shows a crew member the vehicle screen. Server only. */
    public void openScreen(ServerPlayer player) {
        AircraftDefinition def = definition();
        if (def == null || isDestroyed() || player.getVehicle() != this) {
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, playerInventory, p) -> new VehicleMenu(id, playerInventory, this), displayName(def)));
    }

    /**
     * {@code openInventory}: shows the cargo chest of a vehicle with an {@code InventorySize}.
     * Server only.
     *
     * @return false when the vehicle has none
     */
    public boolean openCargo(ServerPlayer player) {
        AircraftDefinition def = definition();
        if (def == null || def.cargoRows() <= 0 || isDestroyed() || !inventory.stillValid(player)) {
            return false;
        }
        int rows = Math.min(def.cargoRows(), 6);
        MenuType<ChestMenu> type = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        player.openMenu(new SimpleMenuProvider((id, playerInventory, p) -> new ChestMenu(type, id, playerInventory, inventory.cargo(), rows),
                displayName(def)));
        return true;
    }

    /** The vehicle's name as its item shows it. */
    private static Component displayName(AircraftDefinition def) {
        return Component.translatableWithFallback("item.mcheli." + def.name(), def.displayName);
    }

    /**
     * {@code supplyAmmoToOtherAircraft}: an ammunition truck ({@code AmmoSupplyRange}) tops up the
     * weapons of every vehicle within range that stands on something, a tenth of each weapon's
     * maximum every two seconds.
     */
    private void supplyAmmo(AircraftDefinition def) {
        float range = def.ammoSupplyRange;
        if (range <= 0.0F || ticksAlive % SupplyRules.AMMO_TRUCK_INTERVAL != 0 || isDestroyed()) {
            return;
        }
        for (AircraftEntity other : level().getEntitiesOfClass(AircraftEntity.class, getBoundingBox().inflate(range))) {
            VehicleWeapons theirs = other.weapons();
            if (other == this || theirs == null || other.isDestroyed() || !other.canSupply()) {
                continue;
            }
            for (WeaponSet set : theirs.sets()) {
                int rounds = SupplyRules.roundsFromTruck(set.ammo() + set.restAmmo(), set.maxAmmo());
                if (rounds > 0) {
                    set.setRestAmmo(set.restAmmo() + rounds);
                    if (set.ammo() <= 0) {
                        set.reloadMagazine();
                    }
                }
            }
        }
    }

    /** {@code repairOtherAircraft}: a repair vehicle ({@code RepairOtherVehicles}) mends those in range. */
    private void repairOthers(AircraftDefinition def) {
        float range = def.repairOtherVehiclesRange;
        if (range <= 0.0F || ticksAlive % SupplyRules.REPAIR_INTERVAL != 0 || isDestroyed()) {
            return;
        }
        for (AircraftEntity other : level().getEntitiesOfClass(AircraftEntity.class, getBoundingBox().inflate(range))) {
            AircraftDefinition theirs = other.definition();
            if (other != this && theirs != null) {
                other.repair(SupplyRules.repair(other.hp(), theirs.maxHp, def.repairOtherVehiclesValue));
            }
        }
    }

    /** Whether the flight model holds a hover: in hover mode, or while the pilot mans the guns. */
    public boolean isHovering() {
        return isHoveringMode() || isGunnerMode();
    }

    /** The hover mode the helicopter pilot switches with jump. */
    public boolean isHoveringMode() {
        return entityData.get(DATA_HOVERING);
    }

    protected void setHovering(boolean hovering) {
        entityData.set(DATA_HOVERING, hovering);
    }

    // Pilot gunner mode

    /**
     * Pilot gunner mode ({@code EnableGunnerMode}): the pilot leaves the controls to man the
     * weapons from the camera position. Helicopters hold a hover; planes level off and circle.
     */
    public boolean isGunnerMode() {
        return entityData.get(DATA_GUNNER_MODE);
    }

    /** {@code canSwitchGunnerMode}: the kinds add their own attitude and speed limits. */
    public boolean canSwitchGunnerMode() {
        AircraftDefinition def = definition();
        if (def == null || !def.gunnerMode || !isCanopyClosed() || isDestroyed() || isHoveringMode()) {
            return false;
        }
        // Without ConcurrentGunnerMode a player in the gunner seat has the guns already.
        return def.concurrentGunnerMode || !(occupant(1) instanceof Player);
    }

    /** Server side. Leaving gunner mode is always allowed; entering it only when {@link #canSwitchGunnerMode}. */
    public void setGunnerMode(boolean on) {
        if (!on || canSwitchGunnerMode()) {
            entityData.set(DATA_GUNNER_MODE, on);
        }
    }

    /** Whether {@code entity} is the pilot and currently in gunner mode. */
    public boolean isGunnerMode(Entity entity) {
        return isGunnerMode() && seatOf(entity) == 0;
    }

    /** Drops gunner mode once its conditions no longer hold. Server side, every tick. */
    private void checkGunnerMode(AircraftDefinition def) {
        if (isGunnerMode() && (occupant(0) == null || isDestroyed()
                || (!def.concurrentGunnerMode && occupant(1) instanceof Player))) {
            entityData.set(DATA_GUNNER_MODE, false);
        }
    }

    // Throttle, roll and input

    /** Throttle for animation and sound, valid on every side. */
    public float visibleThrottle() {
        return isSimulatingHere() ? (float) throttle : entityData.get(DATA_THROTTLE);
    }

    public float visibleReverse() {
        return isSimulatingHere() ? reverse : entityData.get(DATA_REVERSE);
    }

    public float roll() {
        return isSimulatingHere() ? roll : entityData.get(DATA_ROLL);
    }

    public float roll(float partialTicks) {
        return Mth.rotLerp(partialTicks, rollO, roll());
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    protected boolean isSimulatingHere() {
        return isControlledByLocalInstance() && !isPassenger();
    }

    public ControlInput input() {
        return input;
    }

    /**
     * Applies the pilot's view change for one frame. Only called on the pilot's client, and only
     * for kinds that steer with the mouse.
     *
     * @param dYaw   change of the player's yaw since the last frame, in degrees
     * @param dPitch change of the player's pitch since the last frame, in degrees
     */
    public void applyPilotLook(float dYaw, float dPitch) {
    }

    /** Whether the pilot steers with the mouse, as in helicopters and planes. */
    public boolean steersWithMouse() {
        return false;
    }

    /** Server side entry point for the pilot's control packet. */
    public void acceptPilotState(float throttle, float reverse, float roll, boolean hovering, byte keys) {
        this.throttle = Mth.clamp(throttle, 0.0F, 1.0F);
        this.reverse = Mth.clamp(reverse, 0.0F, 1.0F);
        this.roll = AircraftRotation.wrap(roll);
        input.read(keys);
        entityData.set(DATA_THROTTLE, (float) this.throttle);
        entityData.set(DATA_REVERSE, this.reverse);
        entityData.set(DATA_ROLL, this.roll);
        entityData.set(DATA_HOVERING, hovering);
        entityData.set(DATA_INPUT, keys);
    }

    public float reverse() {
        return reverse;
    }

    // Tick

    /**
     * Makes this a workshop test flight: it takes no damage, is not saved with the world, and goes
     * away once its pilot gets out, putting them back where they started.
     */
    public void startTestFlight(Vec3 start) {
        testFlightStart = start;
    }

    @Override
    public boolean shouldBeSaved() {
        return testFlightStart == null && super.shouldBeSaved();
    }

    @Override
    public void tick() {
        if (testFlightOver) {
            discard();
            return;
        }
        rollO = roll();
        if (getHurtTime() > 0) {
            setHurtTime(getHurtTime() - 1);
        }
        super.tick();
        if (level().isClientSide()) {
            tickLerp();
        }
        ticksAlive++;
        lastSeats.clear();
        remoteReturns.clear();
        if (modeSwitchCooldown > 0) {
            modeSwitchCooldown--;
        }

        AircraftDefinition def = definition();
        if (def == null) {
            // Content for this vehicle no longer exists. Keep the entity inert rather than crash.
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (timeSinceHit > 0) {
            timeSinceHit--;
        }
        if (alertCount > 0) {
            alertCount--;
        }
        boolean groundedBefore = onGround();
        double fallBefore = getDeltaMovement().y;
        tickDestroyed();

        if (!level().isClientSide()) {
            supplyFuel(def);
            supplyAmmo(def);
            repairOthers(def);
            burnFuel(def);
            if (def.maxFuel > 0 && fuel() < def.maxFuel && !isDestroyed()) {
                inventory.pourFuelCans(ticksAlive);
            }
            tickNightVision();
            tickStation();
        }
        tickRopes(def);

        if (isSimulatingHere()) {
            if (!(getControllingPassenger() instanceof Player) || isDestroyed()) {
                input.clear();
            }
            if (isRepelling()) {
                // {@code MCH_AircraftClientTickHandler}: the pilot holds a hover while the ropes are out.
                input.throttleUp = false;
                input.throttleDown = false;
                input.left = false;
                input.right = false;
            }
            if (!hasFuel()) {
                // An empty tank: the throttle runs down by itself, as with the pilot's hand off it.
                input.throttleUp = false;
            }
            if (entityData.get(DATA_SINKING)) {
                sink();
            } else {
                simulate();
            }
            float[] kick = weapons != null ? weapons.recoilKick() : null;
            if (kick != null) {
                setXRot(getXRot() + kick[0]);
                roll += kick[1];
            }
            if (!level().isClientSide()) {
                entityData.set(DATA_THROTTLE, (float) throttle);
                entityData.set(DATA_REVERSE, reverse);
                entityData.set(DATA_ROLL, roll);
                entityData.set(DATA_INPUT, input.write());
            }
        } else {
            if (!isControlledByLocalInstance()) {
                input.read(entityData.get(DATA_INPUT));
            }
            setDeltaMovement(Vec3.ZERO);
        }
        for (PartState part : parts) {
            part.tick();
        }
        if (!level().isClientSide()) {
            updatePartsServer(def);
            checkGunnerMode(def);
        }
        animation.tick(this, def);
        tickAnimation();
        if (weapons != null) {
            weapons.tick();
            if (!level().isClientSide()) {
                entityData.set(DATA_FLARE, weapons.flares.isActive());
                warnIncomingMissiles();
            }
        }
        if (level().isClientSide()) {
            damageSmoke(def);
            VehicleParticles.tick(this, def);
        } else {
            tickImpacts(def, groundedBefore, fallBefore);
        }
    }

    /**
     * Server side crashes. A wreck explodes when it lands; an intact vehicle takes damage when it
     * meets the ground steeply tilted ({@code onUpdate_CollisionGroundDamage}), sits under water
     * without floats, flies into terrain, or rams another vehicle.
     */
    private void tickImpacts(AircraftDefinition def, boolean groundedBefore, double fallBefore) {
        Vec3 velocity = lastTickPos != null ? position().subtract(lastTickPos) : Vec3.ZERO;
        Vec3 previous = lastVelocity;
        lastTickPos = position();
        lastVelocity = velocity;
        if (collisionCooldown > 0) {
            collisionCooldown--;
        }
        double speed = Math.max(velocity.length(), previous.length());

        if (isDestroyed()) {
            boolean falling = fallBefore < -0.2 || previous.y < -0.2;
            boolean landed = onGround() && !groundedBefore;
            boolean smashed = horizontalCollision && previous.horizontalDistance() > 0.5;
            if (!exploded && (landed && falling || smashed)) {
                crashExplosion();
            }
            return;
        }

        if (isNearGround(3)) {
            float roll = Math.abs(Mth.wrapDegrees(roll()));
            float pitch = Math.abs(Mth.wrapDegrees(getXRot()));
            if (roll > crashTilt() || pitch > crashTilt()) {
                float damage = roll + pitch;
                damage *= damage < 90.0F ? 0.4F * (float) velocity.length() : 0.4F;
                if (damage > 1.0F && random.nextInt(4) == 0) {
                    hurt(damageSources().inWall(), damage);
                }
            }
        }
        if (ticksAlive % 30 == 0 && !def.floats && level().getFluidState(
                BlockPos.containing(getX() + 0.5, getY() + 1.5 + def.submergedDamageHeight, getZ() + 0.5)).is(FluidTags.WATER)) {
            hurt(damageSources().inWall(), Math.max(def.maxHp / 10, 1));
        }

        if (takesGroundImpacts()) {
            groundImpacts(def, previous);
        }

        // Not in the original: flying into terrain or another vehicle at speed is a crash.
        if (horizontalCollision && previous.horizontalDistance() > TERRAIN_CRASH_SPEED && collisionCooldown == 0) {
            collisionCooldown = 20;
            hurt(damageSources().flyIntoWall(), crashDamage(previous.horizontalDistance()));
        }
        if (speed > VEHICLE_CRASH_SPEED && collisionCooldown == 0) {
            // A blocked move can leave the hulls up to a tick of travel apart; when the vehicle
            // stopped short this tick, look that far for what stopped it.
            boolean stopped = velocity.length() < previous.length() * 0.5;
            double reach = stopped ? 0.5 + previous.length() : 0.5;
            for (AircraftEntity other : solidNeighbours(hullBounds().inflate(reach))) {
                if (!hullsTouch(other, reach)) {
                    continue;
                }
                double closing = velocity.subtract(other.lastVelocity).length();
                closing = Math.max(closing, previous.subtract(other.lastVelocity).length());
                if (closing > VEHICLE_CRASH_SPEED && other.collisionCooldown == 0) {
                    collisionCooldown = 20;
                    other.collisionCooldown = 20;
                    float damage = crashDamage(closing);
                    hurt(damageSources().flyIntoWall(), damage);
                    other.hurt(damageSources().flyIntoWall(), damage);
                    break;
                }
            }
        }
    }

    /** {@code getGiveDamageRot}: how far a vehicle may lean near the ground before it scrapes. */
    protected float crashTilt() {
        return 40.0F;
    }

    /** Crash damage grows with the square of the impact speed, like the energy involved. */
    private static float crashDamage(double speed) {
        return (float) (speed * speed * 60.0);
    }

    /** Whether hitting walls and falling hurt this vehicle. Tanks and cars, which live on the ground. */
    protected boolean takesGroundImpacts() {
        return false;
    }

    /**
     * A ground vehicle that hits a wall or lands after a long drop takes a share of its hit points
     * by the speed or the height, see {@link GroundImpact}. The same height costs a car and a tank
     * the same share before armour.
     */
    private void groundImpacts(AircraftDefinition def, Vec3 previous) {
        if (horizontalCollision && previous.horizontalDistance() > GroundImpact.WALL_SPEED && collisionCooldown == 0) {
            collisionCooldown = 20;
            hurt(damageSources().flyIntoWall(), GroundImpact.wall(previous.horizontalDistance()) * def.maxHp);
        }
        if (isInWater() || isPassenger()) {
            airborneFromY = Double.NaN;
        } else if (!onGround()) {
            airborneFromY = Double.isNaN(airborneFromY) ? getY() : Math.max(airborneFromY, getY());
        } else if (!Double.isNaN(airborneFromY)) {
            double drop = airborneFromY - getY();
            airborneFromY = Double.NaN;
            if (drop > GroundImpact.SAFE_DROP) {
                hurt(damageSources().fall(), GroundImpact.drop(drop) * def.maxHp);
            }
        }
    }

    /** {@code MCH_MissileDetector}: the crew hears a warning while a missile homes on them. */
    private void warnIncomingMissiles() {
        if (alertCount > 0 || isDestroyed() || weapons == null || !weapons.flares.isFitted() || ticksAlive % 5 != 0) {
            return;
        }
        boolean tracked = !level().getEntitiesOfClass(ProjectileEntity.class, getBoundingBox().inflate(60.0),
                p -> p.target() != null && (p.target() == this || hasPassenger(p.target()))).isEmpty();
        if (tracked) {
            alertCount = 20;
            ModSounds.playAt(this, "alert", 1.0F, 1.0F);
        }
    }

    /** One tick of control and physics on the simulating side. */
    protected abstract void simulate();

    /** Visual state that every side updates, for example rotor angles. */
    protected void tickAnimation() {
    }

    /** Ticks since this entity instance was created, used to delay controls right after spawning. */
    protected int ticksAlive() {
        return ticksAlive;
    }

    // Moving parts

    int partBits() {
        return entityData.get(DATA_PARTS);
    }

    void setPartBit(int bit, boolean on) {
        int bits = entityData.get(DATA_PARTS);
        entityData.set(DATA_PARTS, on ? bits | (1 << bit) : bits & ~(1 << bit));
    }

    public @Nullable PartState canopy() {
        return canopy;
    }

    public @Nullable PartState landingGear() {
        return landingGear;
    }

    public @Nullable PartState hatch() {
        return hatch;
    }

    public boolean isCanopyClosed() {
        return canopy == null || canopy.isOff();
    }

    protected void openCanopy() {
        if (canopy != null && modeSwitchCooldown <= 0) {
            canopy.set(true, true);
            modeSwitchCooldown = 20;
        }
    }

    protected void closeCanopy() {
        if (canopy != null && modeSwitchCooldown <= 0 && canopy.status()) {
            canopy.set(false, true);
            modeSwitchCooldown = 20;
        }
    }

    public boolean isLandingGearUp() {
        return landingGear != null && landingGear.status();
    }

    /** Gear can be raised once the vehicle is at least ten blocks above anything solid. */
    public boolean canRaiseLandingGear() {
        return landingGear != null && landingGear.angle() < 1.0F && !landingGear.status() && !hasBlockBelow(3, 10, true);
    }

    public boolean canLowerLandingGear() {
        return landingGear != null && landingGear.angle() >= 89.0F && landingGear.status();
    }

    public void toggleLandingGear() {
        if (landingGear == null || modeSwitchCooldown > 0) {
            return;
        }
        if (canRaiseLandingGear()) {
            landingGear.set(true, true);
            modeSwitchCooldown = 20;
        } else if (canLowerLandingGear()) {
            landingGear.set(false, true);
            modeSwitchCooldown = 20;
        }
    }

    public boolean canFoldHatch() {
        return hatch != null && modeSwitchCooldown <= 0 && hatch.isOff();
    }

    public boolean canUnfoldHatch() {
        return hatch != null && modeSwitchCooldown <= 0 && hatch.isOn();
    }

    /** The fold key: hatches by default, planes fold or sweep their wings instead. */
    public void toggleFold() {
        if (canFoldHatch()) {
            hatch.set(true, true);
            modeSwitchCooldown = 20;
        } else if (canUnfoldHatch()) {
            hatch.set(false, true);
            modeSwitchCooldown = 20;
        }
    }

    /** Throttle below which the landing gear comes down by itself near the ground. */
    protected float autoLowerGearThrottle() {
        return 0.8F;
    }

    /** {@code updateParts}: automatic landing gear, plus whatever a kind adds. Server only. */
    protected void updatePartsServer(AircraftDefinition def) {
        if (landingGear == null) {
            return;
        }
        if (!landingGear.status() && landingGear.factor() <= 0.1F) {
            // Amphibians retract the gear when they settle on water.
            boolean onWater = isInWater() || isWaterBelow(20);
            if (def.floats && onWater && (throttle <= 0.8 || onGround() || hasBlockBelow(3, 20, true))) {
                landingGear.set(true, true);
            }
        } else if (landingGear.status() && landingGear.factor() >= 0.9F) {
            boolean ground = hasBlockBelow(3, 10, true);
            if (throttle < autoLowerGearThrottle() && ground) {
                if (!def.floats || !isWaterBelow(150)) {
                    landingGear.set(false, true);
                }
            } else if (vtolMode() == 2 && ground) {
                landingGear.set(false, true);
            }
        }
    }

    /** 0 off, 1 tilting, 2 fully in VTOL mode. Only planes have one. */
    public int vtolMode() {
        return 0;
    }

    // Seats

    /** Seat (or rack) index of {@code entity}, or -1. */
    public int seatOf(Entity entity) {
        int id = entity.getId();
        for (int i = 0; i < seatIds.length; i++) {
            if (seatIds[i] == id) {
                return i;
            }
        }
        return EMPTY;
    }

    public @Nullable Entity occupant(int seat) {
        if (seat < 0 || seat >= seatIds.length || seatIds[seat] == EMPTY) {
            return null;
        }
        int id = seatIds[seat];
        for (Entity passenger : getPassengers()) {
            if (passenger.getId() == id) {
                return passenger;
            }
        }
        return null;
    }

    public @Nullable SeatDefinition seat(Entity entity) {
        AircraftDefinition def = definition();
        int seat = seatOf(entity);
        return def != null && seat >= 0 && seat < def.seatCount() ? def.seats.get(seat) : null;
    }

    private boolean isSeatFree(int seat) {
        if (seat < 0 || seat >= seatIds.length || seatIds[seat] != EMPTY) {
            return false;
        }
        AircraftDefinition def = definition();
        if (def != null) {
            for (int[] group : def.exclusiveSeats) {
                if (contains(group, seat)) {
                    for (int other : group) {
                        if (other != seat && other >= 0 && other < seatIds.length && seatIds[other] != EMPTY) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    /** First crew seat a player boarding by hand would take. Drones keep their pilot seat for the remote. */
    private int firstFreeCrewSeat() {
        AircraftDefinition def = definition();
        if (def == null) {
            return EMPTY;
        }
        for (int i = def.uav ? 1 : 0; i < def.seatCount(); i++) {
            if (isSeatFree(i)) {
                return i;
            }
        }
        return EMPTY;
    }

    private void assignSeat(Entity entity, int seat) {
        for (int i = 0; i < seatIds.length; i++) {
            if (seatIds[i] == entity.getId()) {
                seatIds[i] = EMPTY;
                if (weapons != null) {
                    weapons.onLeft(i);
                }
            }
        }
        if (seat >= 0 && seat < seatIds.length) {
            seatIds[seat] = entity.getId();
        }
        entityData.set(DATA_SEATS, encodeSeats());
        if (weapons != null) {
            weapons.onSeated(entity, seat);
            weapons.resendAmmo();
        }
    }

    private String encodeSeats() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seatIds.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(seatIds[i]);
        }
        return sb.toString();
    }

    private void decodeSeats(String encoded) {
        if (encoded.isEmpty()) {
            Arrays.fill(seatIds, EMPTY);
            return;
        }
        String[] parts = encoded.split(",");
        if (parts.length != seatIds.length) {
            seatIds = new int[parts.length];
        }
        for (int i = 0; i < parts.length; i++) {
            try {
                seatIds[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                seatIds[i] = EMPTY;
            }
        }
    }

    /** Moves {@code entity} to the next free crew seat, wrapping around. Server only. */
    public void switchSeat(Entity entity) {
        AircraftDefinition def = definition();
        int current = seatOf(entity);
        if (def == null || current < 0 || current >= def.seatCount() || current == 0 && station != null) {
            // A remote pilot stays at the controls; they are not really aboard.
            return;
        }
        int count = def.seatCount();
        seatIds[current] = EMPTY;
        for (int step = 1; step < count; step++) {
            int seat = (current + step) % count;
            if (def.uav && seat == 0) {
                continue;
            }
            if (isSeatFree(seat)) {
                assignSeat(entity, seat);
                if (seat == 0) {
                    closeCanopy();
                }
                return;
            }
        }
        assignSeat(entity, current);
    }

    // UAV stations

    /** Whether a player at a station could take this drone's controls now. */
    public boolean canTakeRemotePilot() {
        AircraftDefinition def = definition();
        return def != null && def.uav && !def.targetDrone && !isDestroyed() && isSeatFree(0);
    }

    /**
     * Seats {@code player}, who sits at {@code from}, at this drone's controls. The pilot is hidden
     * and takes no harm while flying, since in the original they never left the station. Server only.
     */
    public boolean takeRemotePilot(ServerPlayer player, UavStationEntity from) {
        if (!canTakeRemotePilot()) {
            return false;
        }
        pendingSeat = 0;
        boolean seated = player.startRiding(this, true);
        pendingSeat = EMPTY;
        if (!seated) {
            return false;
        }
        station = from.getUUID();
        stationReturn = from.returnPosition();
        entityData.set(DATA_STATION, from.getId());
        player.addTag(REMOTE_PILOT_TAG);
        player.setInvisible(true);
        player.setInvulnerable(true);
        return true;
    }

    /** Sends the remote pilot back to the station, if one is flying. Server only. */
    public void releaseRemotePilot() {
        Entity pilot = occupant(0);
        if (station != null && pilot != null) {
            pilot.stopRiding();
        }
    }

    /** The station this drone is flown from, as the client sees it. */
    public @Nullable UavStationEntity stationEntity() {
        return level().getEntity(entityData.get(DATA_STATION)) instanceof UavStationEntity s ? s : null;
    }

    /** Flat distance to the station this drone is flown from, or -1 without one. */
    public double stationDistance() {
        UavStationEntity from = stationEntity();
        if (from == null) {
            return -1.0;
        }
        double dx = getX() - from.getX();
        double dz = getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Whether {@code entity} flies this drone from a station rather than sitting in it. */
    public boolean isRemotePilot(Entity entity) {
        return (station != null || entityData.get(DATA_STATION) != 0) && occupant(0) == entity;
    }

    private void endRemoteControl(Entity pilot) {
        UavStationEntity from = level() instanceof ServerLevel server && station != null
                && server.getEntity(station) instanceof UavStationEntity s ? s : null;
        Vec3 home = from != null ? from.returnPosition() : stationReturn;
        if (home != null) {
            remoteReturns.put(pilot.getId(), home);
        }
        clearRemotePilot(pilot);
        if (from != null) {
            from.setLinked(false);
        }
        station = null;
        stationReturn = null;
        entityData.set(DATA_STATION, 0);
    }

    /** Undoes what {@link #takeRemotePilot} did to the player. */
    public static void clearRemotePilot(Entity pilot) {
        if (pilot.removeTag(REMOTE_PILOT_TAG)) {
            pilot.setInvisible(false);
            pilot.setInvulnerable(false);
        }
    }

    /**
     * {@code updateUAV}: the link breaks when the station is gone, and flying out of its range
     * loses the drone, as in the original.
     */
    private void tickStation() {
        if (station == null || !(level() instanceof ServerLevel server)) {
            return;
        }
        Entity pilot = occupant(0);
        if (pilot == null) {
            station = null;
            stationReturn = null;
            entityData.set(DATA_STATION, 0);
            return;
        }
        UavStationEntity from = server.getEntity(station) instanceof UavStationEntity s && s.isAlive() ? s : null;
        if (from == null) {
            pilot.stopRiding();
            return;
        }
        entityData.set(DATA_STATION, from.getId());
        if (!from.inRange(this) && !isDestroyed()) {
            if (pilot instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.mcheli.uav.lost"), true);
            }
            pilot.stopRiding();
            hurt(damageSources().fellOutOfWorld(), hp() + 10.0F);
        }
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        // A wreck is nobody's to fly; the server lets it fall, so its crash is seen the same way by everyone.
        return occupant(0) instanceof Player player && !isDestroyed() ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        AircraftDefinition def = definition();
        if (def == null || !def.canRide) {
            return false;
        }
        if (pendingSeat >= 0) {
            return isSeatFree(pendingSeat);
        }
        // Coming back aboard after a reload, a remote pilot included.
        Integer saved = savedSeats.get(passenger.getUUID());
        if (saved != null && isSeatFree(saved)) {
            return true;
        }
        // Cargo only boards through a rack, never into a crew seat.
        return !(passenger instanceof AircraftEntity || passenger instanceof ContainerEntity) && firstFreeCrewSeat() >= 0;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (level().isClientSide()) {
            return;
        }
        int seat = pendingSeat;
        pendingSeat = EMPTY;
        if (seat < 0) {
            Integer saved = savedSeats.remove(passenger.getUUID());
            seat = saved != null && isSeatFree(saved) ? saved : firstFreeCrewSeat();
        }
        assignSeat(passenger, seat);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (level().isClientSide()) {
            return;
        }
        int seat = seatOf(passenger);
        if (seat >= 0) {
            if (hasNightVision(seat)) {
                nightVisionSeats &= ~(1 << seat);
                NightVision.end(passenger);
            }
            lastSeats.put(passenger.getId(), seat);
            seatIds[seat] = EMPTY;
            entityData.set(DATA_SEATS, encodeSeats());
            if (weapons != null) {
                weapons.onLeft(seat);
            }
        }
        if (seat == 0) {
            input.clear();
            if (station != null) {
                endRemoteControl(passenger);
            }
            if (testFlightStart != null) {
                remoteReturns.put(passenger.getId(), testFlightStart);
                testFlightOver = true;
            }
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        AircraftDefinition def = definition();
        int seat = seatOf(passenger);
        if (def == null || seat < 0 || seat >= def.seatAndRackCount()) {
            return super.getPassengerAttachmentPoint(passenger, dimensions, scale);
        }
        Vector3f p = seatPosition(def, seat, 1.0F);
        return new Vec3(p.x, p.y, p.z);
    }

    /** World offset of a seat from the vehicle origin. Turret seats turn with the pilot's view. */
    public Vector3f seatPosition(AircraftDefinition def, int seat, float partialTicks) {
        Vector3 pos;
        boolean turret;
        if (seat < def.seatCount()) {
            SeatDefinition s = def.seats.get(seat);
            pos = s.position();
            turret = s.rotatingSeat();
        } else {
            RackDefinition rack = def.rack(seat);
            pos = rack.position();
            turret = rack.rotatingSeat();
        }
        return toWorldOffset(turret ? onTurret(def, pos, partialTicks) : pos);
    }

    /** Rotates a hull-space point around {@code TurretPosition} by the pilot's view yaw. */
    public Vector3 onTurret(AircraftDefinition def, Vector3 pos, float partialTicks) {
        Vector3 t = def.turretPosition;
        Vector3f v = AircraftRotation.toWorld(animation.seatYaw(0, partialTicks), 0.0F, 0.0F,
                pos.x() - t.x(), pos.y() - t.y(), pos.z() - t.z());
        return new Vector3(v.x + t.x(), v.y + t.y(), v.z + t.z());
    }

    /** Rotates a hull-space point into the world frame, relative to the vehicle origin. */
    public Vector3f toWorldOffset(Vector3 local) {
        return AircraftRotation.toWorld(getYRot(), getXRot(), roll(), local.x(), local.y(), local.z());
    }

    public Vec3 toWorld(Vector3 local) {
        Vector3f v = toWorldOffset(local);
        return position().add(v.x, v.y, v.z);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        AircraftDefinition def = definition();
        if (def != null && passenger instanceof AircraftEntity cargo) {
            RackDefinition rack = def.rack(seatOf(passenger));
            float yaw = rack != null ? rack.yaw() : 0.0F;
            float pitch = rack != null ? rack.pitch() : 0.0F;
            cargo.setYRot(getYRot() + yaw);
            cargo.setXRot(getXRot() + pitch);
            cargo.roll = roll();
            cargo.setDeltaMovement(Vec3.ZERO);
        } else if (def != null && passenger instanceof ContainerEntity container) {
            RackDefinition rack = def.rack(seatOf(passenger));
            container.setYRot(getYRot() + (rack != null ? rack.yaw() : 0.0F));
            container.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 home = remoteReturns.remove(passenger.getId());
        if (home != null) {
            return home;
        }
        AircraftDefinition def = definition();
        Integer seat = lastSeats.get(passenger.getId());
        if (def != null) {
            Vector3 local;
            if (def.unmountPosition != null) {
                local = def.unmountPosition;
            } else {
                Vector3 seatPos = seat != null && seat < def.seatCount() ? def.seats.get(seat).position() : def.seats.getFirst().position();
                double x = seatPos.x() >= 0.0 ? seatPos.x() + 3.0 : seatPos.x() - 3.0;
                local = new Vector3(x, 2.0, seatPos.z());
            }
            Vec3 at = toWorld(local);
            AABB box = passenger.getDimensions(Pose.STANDING).makeBoundingBox(at);
            if (level().noCollision(passenger, box)) {
                return at;
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    /**
     * 1.20.6 hands the click position to {@code interactAt}, which the client sends before the
     * plain {@code interact}; 26.x merged the two.
     */
    @Override
    public InteractionResult interactAt(Player player, Vec3 location, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ChainItem) {
            return ChainItem.useOn(this, player);
        }
        if (definition() != null && (held.getItem() instanceof FuelItem || held.getItem() instanceof WrenchItem)) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return held.getItem() instanceof FuelItem ? FuelItem.pour(held, this, player) : WrenchItem.useOn(held, this, player, hand);
        }
        InteractionResult result = super.interact(player, hand);
        AircraftDefinition def = definition();
        if (result == InteractionResult.PASS && player.isSecondaryUseActive() && def != null && def.cargoRows() > 0
                && !isDestroyed() && player.getVehicle() != this) {
            // Sneaking opens the cargo chest from outside, as in the original.
            if (player instanceof ServerPlayer server) {
                openCargo(server);
            }
            return InteractionResult.SUCCESS;
        }
        if (result != InteractionResult.PASS || player.isSecondaryUseActive()) {
            return result;
        }
        if (def == null || !def.canRide || player.getVehicle() == this) {
            return InteractionResult.PASS;
        }
        boolean boardingPilot = !def.uav && isSeatFree(0);
        if (boardingPilot && canopy != null && isCanopyClosed()) {
            // The first click opens the canopy, the next one climbs in.
            if (!level().isClientSide()) {
                openCanopy();
            }
            return InteractionResult.SUCCESS;
        }
        if (boardingPilot && modeSwitchCooldown > 0) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide()) {
            if (!player.startRiding(this)) {
                return InteractionResult.PASS;
            }
            if (seatOf(player) == 0) {
                closeCanopy();
                boardNearbyMobs();
            }
        }
        return InteractionResult.SUCCESS;
    }

    // Racks

    /** Loads {@code cargo} into the given rack seat. Server only. */
    private boolean loadCargo(Entity cargo, int seat) {
        if (!isSeatFree(seat) || cargo.isPassenger()) {
            return false;
        }
        pendingSeat = seat;
        boolean loaded = cargo.startRiding(this, true);
        pendingSeat = EMPTY;
        if (loaded) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        return loaded;
    }

    /** Whether {@code cargo} may ride rack {@code seat} of this vehicle. */
    public boolean acceptsCargo(AircraftEntity cargo, int seat) {
        AircraftDefinition def = definition();
        AircraftDefinition cargoDef = cargo.definition();
        RackDefinition rack = def != null ? def.rack(seat) : null;
        if (rack == null || cargoDef == null || cargo == this || isPassenger() || cargo.isPassenger()) {
            return false;
        }
        // A vehicle that carries cargo itself cannot be loaded.
        for (Entity p : cargo.getPassengers()) {
            if (p instanceof AircraftEntity) {
                return false;
            }
        }
        if (rack.accepts(cargoDef.name(), cargoDef.kind())) {
            return true;
        }
        for (RideRack rr : cargoDef.rideRacks) {
            if (rr.carrier().equalsIgnoreCase(def.name()) && def.seatCount() + rr.rack() - 1 == seat) {
                return true;
            }
        }
        return false;
    }

    /** Whether rack {@code seat} carries containers, {@code AddRack} naming {@code container}. */
    public boolean acceptsContainer(ContainerEntity container, int seat) {
        AircraftDefinition def = definition();
        RackDefinition rack = def != null ? def.rack(seat) : null;
        return rack != null && !isPassenger() && !container.isPassenger()
                && rack.accepts(ContainerEntity.CARGO_NAME, ContainerEntity.CARGO_NAME);
    }

    private boolean isNearRackEntry(AircraftEntity cargo, RackDefinition rack) {
        Vec3 entry = toWorld(rack.entry());
        float r = rack.range();
        return Math.abs(cargo.getX() - entry.x) <= r && Math.abs(cargo.getY() - entry.y) <= r && Math.abs(cargo.getZ() - entry.z) <= r;
    }

    /** Carrier pilot loads every nearby vehicle or container that fits a free rack. Server only. */
    public void loadNearbyCargo() {
        AircraftDefinition def = definition();
        if (def == null || def.racks.isEmpty() || throttle > 0.3 || !hasBlockBelow(1, 3, true)) {
            return;
        }
        for (int seat = def.seatCount(); seat < def.seatAndRackCount(); seat++) {
            if (!isSeatFree(seat)) {
                continue;
            }
            RackDefinition rack = def.rack(seat);
            Vec3 entry = toWorld(rack.entry());
            AABB area = new AABB(entry, entry).inflate(rack.range());
            boolean loaded = false;
            for (AircraftEntity cargo : level().getEntitiesOfClass(AircraftEntity.class, area)) {
                if (acceptsCargo(cargo, seat) && loadCargo(cargo, seat)) {
                    loaded = true;
                    break;
                }
            }
            if (!loaded) {
                for (ContainerEntity container : level().getEntitiesOfClass(ContainerEntity.class, area)) {
                    if (acceptsContainer(container, seat) && loadCargo(container, seat)) {
                        break;
                    }
                }
            }
        }
    }

    /** Drops the last loaded piece of cargo at its rack's entry point. Server only. */
    public void unloadCargo() {
        AircraftDefinition def = definition();
        if (def == null) {
            return;
        }
        for (int seat = def.seatAndRackCount() - 1; seat >= def.seatCount(); seat--) {
            Entity cargo = occupant(seat);
            if (cargo == null) {
                continue;
            }
            RackDefinition rack = def.rack(seat);
            Vector3 drop = rack.entry();
            if (cargo instanceof AircraftEntity) {
                // Aircraft roll out well clear of the carrier's tail or nose.
                drop = drop.add(0.0, 0.0, drop.z() >= centreZ(def) ? 12.0 : -12.0);
            }
            Vec3 at = toWorld(drop);
            cargo.stopRiding();
            cargo.moveTo(at.x, at.y, at.z, getYRot() + rack.yaw(), 0.0F);
            // As in the original, with no ground in the 20 blocks below it goes down under a canopy.
            if (!(cargo instanceof AircraftEntity) && !hasBlockBelow(3, 20, false)) {
                ParachuteEntity.open(level(), cargo, ParachuteEntity.Kind.DROP);
            }
            return;
        }
    }

    /** Leaves the rack this vehicle rides on, at the rack entry point. Server only. */
    public void leaveRack() {
        if (!(getVehicle() instanceof AircraftEntity carrier)) {
            return;
        }
        AircraftDefinition carrierDef = carrier.definition();
        RackDefinition rack = carrierDef != null ? carrierDef.rack(carrier.seatOf(this)) : null;
        Vec3 at = rack != null ? carrier.toWorld(rack.entry()) : position();
        stopRiding();
        moveTo(at.x, at.y, at.z, getYRot(), 0.0F);
    }

    /**
     * Pilot of a vehicle near a free rack of a carrier boards that rack. Server only.
     *
     * @return whether a rack was found
     */
    public boolean boardNearbyRack() {
        if (isPassenger()) {
            return false;
        }
        for (AircraftEntity carrier : level().getEntitiesOfClass(AircraftEntity.class, getBoundingBox().inflate(60.0))) {
            AircraftDefinition def = carrier.definition();
            if (carrier == this || def == null) {
                continue;
            }
            for (int seat = def.seatCount(); seat < def.seatAndRackCount(); seat++) {
                if (carrier.isSeatFree(seat) && carrier.acceptsCargo(this, seat) && carrier.isNearRackEntry(this, def.rack(seat))
                        && carrier.loadCargo(this, seat)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double centreZ(AircraftDefinition def) {
        if (def.boundingBoxes.isEmpty()) {
            return 0.0;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (var box : def.boundingBoxes) {
            min = Math.min(min, box.offset().z() - box.width() / 2.0);
            max = Math.max(max, box.offset().z() + box.width() / 2.0);
        }
        return (min + max) / 2.0;
    }

    // Ejection

    /** Seats zero and one of vehicles with {@code EnableEjectionSeat} can bail out. */
    public boolean canEject(Entity entity) {
        AircraftDefinition def = definition();
        int seat = seatOf(entity);
        return def != null && def.ejectionSeat && seat >= 0 && seat < 2 && !(seat == 0 && def.uav);
    }

    /**
     * {@code ejectSeat}: throws the occupant clear of the aircraft in an ejection seat, whose
     * parachute opens once it starts to fall. Each ejection takes a parachute from the parachute
     * slots of the vehicle screen; without one the seat stays put. Creative players need none.
     * Server only.
     */
    public void eject(Entity entity) {
        AircraftDefinition def = definition();
        int seat = seatOf(entity);
        if (def == null || !canEject(entity)) {
            return;
        }
        boolean creative = entity instanceof Player player && player.getAbilities().instabuild;
        if (!creative && !inventory.takeParachute()) {
            if (entity instanceof ServerPlayer player) {
                ModSounds.playTo(player, "ng", 1.0F, 1.0F);
                player.displayClientMessage(Component.translatable("message.mcheli.no_parachute"), true);
            }
            return;
        }
        Vector3 seatPos = def.seats.get(seat).position();
        Vec3 out = toWorld(seatPos.add(0.0, 2.0, 0.0));
        Vector3f push = toWorldOffset(new Vector3(0.0, 2.0, 0.0));
        entity.stopRiding();
        entity.teleportTo(out.x, out.y, out.z);
        entity.setDeltaMovement(getDeltaMovement().add(push.x + (random.nextFloat() - 0.5) * 0.1, push.y,
                push.z + (random.nextFloat() - 0.5) * 0.1));
        entity.hurtMarked = true;
        ParachuteEntity.open(level(), entity, ParachuteEntity.Kind.EJECTION_SEAT);
        if (canopy != null && !canopy.status()) {
            canopy.set(true, false);
            modeSwitchCooldown = 40;
        }
        ModSounds.playAt(entity, "eject_seat", 5.0F, 1.0F);
    }

    // Search lights

    public boolean isSearchLightOn() {
        AircraftDefinition def = definition();
        return def != null && !def.searchLights.isEmpty() && entityData.get(DATA_SEARCH_LIGHT);
    }

    /** {@code canSwitchSearchLight}: the pilot or the first crew seat, in vehicles that have one. */
    public boolean canSwitchSearchLight(Entity entity) {
        AircraftDefinition def = definition();
        int seat = seatOf(entity);
        return def != null && !def.searchLights.isEmpty() && seat >= 0 && seat <= 1;
    }

    /** Server only. */
    public void toggleSearchLight(Entity entity) {
        if (canSwitchSearchLight(entity)) {
            entityData.set(DATA_SEARCH_LIGHT, !entityData.get(DATA_SEARCH_LIGHT));
        }
    }

    /**
     * Where a turning search light points: the first crew seat's view, else the pilot's, as
     * {@code renderLight} took them. Null with nobody aboard to aim it.
     */
    public @Nullable Entity searchLightAimer() {
        Entity crew = occupant(1);
        return crew != null ? crew : occupant(0);
    }

    // Troops: parachute drops and ropes

    public boolean isRepelling() {
        return entityData.get(DATA_REPELLING);
    }

    /** How far the ropes hang below their hooks, as a negative number. */
    public float ropeLength() {
        return ropeLength;
    }

    /** Where rope {@code index} hangs from, in the world. */
    public Vec3 ropePosition(int index) {
        AircraftDefinition def = definition();
        return def != null && index >= 0 && index < def.repellingHooks.size() ? toWorld(def.repellingHooks.get(index).position()) : position();
    }

    /**
     * {@code onUpdate_Repelling} and {@code onUpdate_UnmountCrew}: the ropes run out towards the
     * ground, then the mobs aboard slide down them one per hook interval; a parachute drop lets one
     * passenger out per {@code MobDropOption} interval while the hatch is open.
     */
    private void tickRopes(AircraftDefinition def) {
        if (!def.repellingHooks.isEmpty()) {
            if (isRepelling()) {
                int clear = airBelow(50);
                if (ropeLength > -50.0F && ropeLength > -clear) {
                    ropeLength -= level().isClientSide() ? 0.3F : 0.25F;
                }
            } else {
                ropeLength = 0.0F;
            }
        }
        if (level().isClientSide()) {
            return;
        }
        if (isRepelling() && isDestroyed()) {
            entityData.set(DATA_REPELLING, false);
        }
        if (!isRepelling()) {
            repellingTicks = 0;
        } else if (repellingTicks < 60) {
            repellingTicks++;
        } else {
            for (int rope = 0; rope < def.repellingHooks.size(); rope++) {
                if (ticksAlive % def.repellingHooks.get(rope).interval() != 0) {
                    continue;
                }
                for (int seat = 1; seat < def.seatCount(); seat++) {
                    if (occupant(seat) instanceof Mob mob) {
                        rappel(mob, rope);
                        break;
                    }
                }
            }
        }
        if (droppingTroops) {
            if (hasBlockBelow(3, 10, false) || isDestroyed()) {
                droppingTroops = false;
            } else if ((hatch == null || hatch.isOn()) && ticksAlive % def.dropInterval == 0 && !dropNextTroop(def)) {
                droppingTroops = false;
            }
        }
    }

    /** Air blocks straight below, up to {@code max}. */
    private int airBelow(int max) {
        BlockPos.MutableBlockPos pos = blockPosition().mutable();
        int i = 0;
        while (i < max && pos.getY() - i > level().getMinBuildHeight() && level().getBlockState(pos.setY(blockPosition().getY() - i - 1)).isAir()) {
            i++;
        }
        return i;
    }

    /**
     * {@code canStartRepelling}: a helicopter in hover mode, level and holding still. The original
     * compared against where it was a few ticks before.
     */
    private boolean canStartRepelling() {
        return this instanceof HelicopterEntity && isHoveringMode() && Math.abs(getXRot()) < 3.0F && Math.abs(roll()) < 3.0F
                && lastTickPos != null && position().distanceTo(lastTickPos) < 0.05;
    }

    /**
     * The pilot's drop key, {@code unmountCrew}: with ropes, lets them down from a steady hover
     * or pulls them up again; with {@code EnableParachuting}, starts or stops dropping the mobs
     * aboard under parachutes; otherwise, or close to the ground, simply lets the mobs out.
     * Server only.
     *
     * @return false if nothing could be done, so the pilot hears it
     */
    public boolean dropCrew() {
        AircraftDefinition def = definition();
        if (def == null || isDestroyed()) {
            return false;
        }
        if (!def.repellingHooks.isEmpty()) {
            if (isRepelling()) {
                entityData.set(DATA_REPELLING, false);
                return true;
            }
            if (hasBlockBelow(3, 4, false)) {
                return unloadMobs();
            }
            if (!canStartRepelling()) {
                return false;
            }
            entityData.set(DATA_REPELLING, true);
            repellingTicks = 0;
            return true;
        }
        if (droppingTroops) {
            droppingTroops = false;
            return true;
        }
        if (def.parachuting && !hasBlockBelow(3, 10, false)) {
            droppingTroops = true;
            if (hatch != null && hatch.isOff()) {
                hatch.set(true, true);
                modeSwitchCooldown = 20;
            }
            return true;
        }
        return unloadMobs();
    }

    /** Lets every mob in a crew seat out, as in {@code unmountCrew(false)}. */
    private boolean unloadMobs() {
        AircraftDefinition def = definition();
        boolean any = false;
        for (int seat = 1; def != null && seat < def.seatCount(); seat++) {
            if (occupant(seat) instanceof Mob mob) {
                mob.stopRiding();
                any = true;
            }
        }
        return any;
    }

    /** The next mob behind the second seat leaves at the drop point under a parachute. */
    private boolean dropNextTroop(AircraftDefinition def) {
        for (int seat = 2; seat < def.seatCount(); seat++) {
            if (occupant(seat) instanceof Mob mob) {
                parachuteOut(mob, def);
                return true;
            }
        }
        return false;
    }

    private void parachuteOut(Entity entity, AircraftDefinition def) {
        Vec3 out = toWorld(def.dropPosition);
        entity.stopRiding();
        entity.teleportTo(out.x, out.y, out.z);
        entity.setDeltaMovement(getDeltaMovement());
        entity.hurtMarked = true;
        ParachuteEntity.open(level(), entity, ParachuteEntity.Kind.DROP);
    }

    /**
     * A passenger's own drop key, {@code MCH_ClientSeatTickHandler}: slide down a rope once the
     * ropes are out, or jump with a parachute from behind the second seat of a vehicle with
     * {@code EnableParachuting} and room below. Server only.
     *
     * @return false if neither is possible right now
     */
    public boolean jumpOut(Player player) {
        AircraftDefinition def = definition();
        int seat = seatOf(player);
        if (def == null || seat <= 0 || seat >= def.seatCount() || isDestroyed()) {
            return false;
        }
        if (isRepelling() && repellingTicks > 50 && !def.repellingHooks.isEmpty()) {
            lastRope = (lastRope + 1) % def.repellingHooks.size();
            rappel(player, lastRope);
            return true;
        }
        if (def.parachuting && seat >= 2 && !hasBlockBelow(3, 13, false)) {
            parachuteOut(player, def);
            return true;
        }
        return false;
    }

    /** {@code unmountEntityRepelling}: two blocks under the hook, onto a rope rider that lets them down. */
    private void rappel(Entity entity, int rope) {
        Vec3 at = ropePosition(rope).add(0.0, -2.0, 0.0);
        entity.stopRiding();
        entity.teleportTo(at.x, at.y, at.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.resetFallDistance();
        RappelEntity rider = new RappelEntity(ModEntities.RAPPEL.get(), level());
        rider.attach(this, rope);
        rider.setPos(at);
        level().addFreshEntity(rider);
        entity.startRiding(rider, true);
        lastRope = rope;
    }

    /**
     * {@code mountMobToSeats}: when the pilot climbs in, mobs standing close by take the free crew
     * seats, so villagers and the like can be flown somewhere. Server only.
     */
    private void boardNearbyMobs() {
        AircraftDefinition def = definition();
        if (def == null || def.uav) {
            return;
        }
        for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(3.0, 2.0, 3.0), m -> !m.isPassenger() && !m.isVehicle())) {
            for (int seat = 1; seat < def.seatCount(); seat++) {
                if (isSeatFree(seat)) {
                    pendingSeat = seat;
                    mob.startRiding(this, true);
                    pendingSeat = EMPTY;
                    break;
                }
            }
        }
    }

    // Terrain helpers

    /** True if any solid block is within {@code depth} blocks below the vehicle. */
    protected boolean isNearGround(int depth) {
        BlockPos.MutableBlockPos pos = blockPosition().mutable();
        for (int i = 0; i < depth; i++) {
            pos.move(0, -1, 0);
            if (!level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Port of {@code MCH_Lib.getBlockY}: scans a {@code size} x {@code size} column from the
     * vehicle's block down {@code depth} layers (the first layer being the vehicle's own).
     *
     * @param solidOnly ignore blocks without collision; fluids still count when this is false
     */
    public boolean hasBlockBelow(int size, int depth, boolean solidOnly) {
        int px = Mth.floor(getX() + 0.5);
        int py = Mth.floor(getY() + 0.5);
        int pz = Mth.floor(getZ() + 0.5);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < depth; y++) {
            for (int x = -size / 2; x <= size / 2; x++) {
                for (int z = -size / 2; z <= size / 2; z++) {
                    pos.set(px + x, py - y, pz + z);
                    BlockState state = level().getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (!solidOnly || !state.getCollisionShape(level(), pos).isEmpty() || !state.getFluidState().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** True if the first non-air block within {@code depth} below is water. */
    protected boolean isWaterBelow(int depth) {
        BlockPos.MutableBlockPos pos = blockPosition().mutable();
        for (int i = 0; i < depth; i++) {
            BlockState state = level().getBlockState(pos);
            if (!state.isAir()) {
                return !state.getFluidState().isEmpty();
            }
            pos.move(0, -1, 0);
        }
        return false;
    }

    /** {@code getBlockY(this, 3, -2, false)}: standing on something that is neither air nor water. */
    protected boolean isOnSolidGround() {
        int px = Mth.floor(getX() + 0.5);
        int py = Mth.floor(getY() + 0.5);
        int pz = Mth.floor(getZ() + 0.5);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockState state = level().getBlockState(pos.set(px + x, py - y, pz + z));
                    if (!state.isAir()) {
                        return state.getFluidState().isEmpty();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Port of {@code getWaterDepth}: the share of the hit box that is in water, from 0 to 1, sampled
     * in five horizontal slices. {@code FloatOffset} raises the slices, so a hull with an offset rides
     * higher. The float physics only pushes up once this reaches 1, which sets the waterline.
     *
     * <p>The original drew the model {@value #LEGACY_MODEL_LIFT} above the bottom of the hit box
     * (its {@code yOffset}, half the 0.7 default height); this port draws it at the bottom. The
     * slices are lowered by the same amount so boats sit in the water as they did in 1.0.3.
     */
    protected double waterDepth() {
        if (entityData.get(DATA_SINKING)) {
            return 0.0;
        }
        AircraftDefinition def = definition();
        float offset = (def != null ? def.floatOffset : 0.0F) - LEGACY_MODEL_LIFT;
        AABB box = getBoundingBox();
        double height = box.maxY - box.minY;
        double depth = 0.0;
        for (int i = 0; i < 5; i++) {
            double bottom = box.minY + height * i / 5.0 - 0.125 + offset;
            double top = box.minY + height * (i + 1) / 5.0 - 0.125 + offset;
            AABB slice = new AABB(box.minX, bottom, box.minZ, box.maxX, top, box.maxZ);
            if (level().getBlockStates(slice).anyMatch(state -> state.getFluidState().is(FluidTags.WATER))) {
                depth += 1.0 / 5.0;
            }
        }
        return depth;
    }

    /** {@code applyOnGroundPitch}: eases pitch towards {@code OnGroundPitch} and roll towards level. */
    protected void applyOnGroundPitch(float factor) {
        AircraftDefinition def = definition();
        if (def != null) {
            float ground = def.onGroundPitch;
            setXRot((getXRot() - ground) * factor + ground);
        }
        roll *= factor;
    }

    /** {@code StepHeight}: how tall a ledge the vehicle drives up without jumping. */
    @Override
    public float maxUpStep() {
        AircraftDefinition def = definition();
        return def != null ? def.stepHeight : 0.0F;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // Weapons

    public @Nullable VehicleWeapons weapons() {
        definition();
        return weapons;
    }

    public void publishWeaponSelection() {
        if (weapons != null && !level().isClientSide()) {
            entityData.set(DATA_WEAPON_SELECTION, weapons.encodeSelection());
        }
    }

    public void publishWeaponUse(int stat) {
        entityData.set(DATA_WEAPON_USE, stat);
    }

    public int syncedWeaponUse() {
        return entityData.get(DATA_WEAPON_USE);
    }

    /**
     * Whether {@code set} fires along the user's view instead of the hull: the shot direction and
     * the muzzle positions are worked out as if the vehicle faced where the user looks. Static
     * mounts do this; see {@link GroundVehicleEntity#aimsWithUser}.
     */
    public boolean aimsWithUser(WeaponSet set) {
        return false;
    }

    /** {@code isInfinityAmmo}: creative players never run dry, nor does anyone when the server config says so. */
    public boolean hasInfiniteAmmo(@Nullable Entity user) {
        return ServerConfig.infiniteAmmo() || user instanceof Player player && player.getAbilities().instabuild;
    }

    /** Where the pilot looks relative to the hull: the turret's traverse. */
    public float turretYaw() {
        return animation.seatYaw(0, 1.0F);
    }

    public float stealth() {
        AircraftDefinition def = definition();
        return def != null ? def.stealth : 0.0F;
    }

    /** The TV missile the crew is flying, if it still exists. */
    public @Nullable TvMissileEntity tvMissile() {
        if (level().isClientSide()) {
            return level().getEntity(entityData.get(DATA_TV_MISSILE)) instanceof TvMissileEntity m && !m.isRemoved() ? m : null;
        }
        return tvMissile != null && !tvMissile.isRemoved() ? tvMissile : null;
    }

    public void setTvMissile(@Nullable TvMissileEntity missile) {
        tvMissile = missile;
        entityData.set(DATA_TV_MISSILE, missile != null ? missile.getId() : 0);
    }

    /** Whether the flares are burning right now, for the crew's display. */
    public boolean isFlareActive() {
        return entityData.get(DATA_FLARE);
    }

    /** Someone is locking on to this vehicle; its crew hears the warning. Server only. */
    public void warnLocked() {
        if (alertCount == 0 && !isDestroyed() && weapons != null && weapons.flares.isFitted()) {
            alertCount = 10;
            for (int seat = 0; seat < 2; seat++) {
                if (occupant(seat) instanceof ServerPlayer player) {
                    ModSounds.playTo(player, "locked", 1.0F, 1.0F);
                }
            }
        }
    }

    // Hit boxes

    /**
     * Clips a ray against the main hit box and every {@code BoundingBox}, remembering the damage
     * factor of the box hit first, as the original's composite bounding box did.
     */
    public Optional<Vec3> clipHitBoxes(Vec3 from, Vec3 to, double inflate) {
        hitBoxFactor = 1.0F;
        Optional<Vec3> best = clip(getBoundingBox().inflate(inflate), from, to);
        double bestDistance = best.map(from::distanceToSqr).orElse(Double.MAX_VALUE);
        AircraftDefinition def = definition();
        if (def != null) {
            for (BoundingBoxDefinition box : def.hull()) {
                Optional<Vec3> hit = clip(hullBox(box).inflate(inflate), from, to);
                if (hit.isPresent() && from.distanceToSqr(hit.get()) < bestDistance) {
                    best = hit;
                    bestDistance = from.distanceToSqr(hit.get());
                    hitBoxFactor = box.damageFactor();
                }
            }
        }
        return best;
    }

    /**
     * One {@code BoundingBox} in world space. Like the original's, it stays axis aligned: the
     * centre turns with the hull, the box itself does not.
     */
    private AABB hullBox(BoundingBoxDefinition box) {
        Vec3 c = toWorld(box.offset());
        double hw = box.width() / 2.0;
        double hh = box.height() / 2.0;
        return new AABB(c.x - hw, c.y - hh, c.z - hw, c.x + hw, c.y + hh, c.z + hw);
    }

    /** The whole hull: the main hit box, every {@code BoundingBox} and the generated boxes, in world space. */
    public List<AABB> hullBoxes() {
        AircraftDefinition def = definition();
        if (def == null || def.hull().isEmpty()) {
            return List.of(getBoundingBox());
        }
        List<AABB> boxes = new ArrayList<>(def.hull().size() + 1);
        boxes.add(getBoundingBox());
        for (BoundingBoxDefinition box : def.hull()) {
            boxes.add(hullBox(box));
        }
        return boxes;
    }

    /** The smallest box around the whole hull. */
    private AABB hullBounds() {
        AABB bounds = getBoundingBox();
        AircraftDefinition def = definition();
        if (def != null) {
            for (BoundingBoxDefinition box : def.hull()) {
                bounds = bounds.minmax(hullBox(box));
            }
        }
        return bounds;
    }

    /** Whether any part of this hull touches any part of {@code other}'s, grown by {@code inflate}. */
    private boolean hullsTouch(AircraftEntity other, double inflate) {
        // Hulls generated from big models run to a thousand boxes, so skip ours that are nowhere near.
        AABB around = other.hullBounds().inflate(inflate);
        List<AABB> theirs = null;
        for (AABB mine : hullBoxes()) {
            AABB grown = mine.inflate(inflate);
            if (!grown.intersects(around)) {
                continue;
            }
            if (theirs == null) {
                theirs = other.hullBoxes();
            }
            for (AABB box : theirs) {
                if (grown.intersects(box)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Vehicles whose hulls block this one: everything nearby except its own carrier or cargo, and
     * vehicles either of the two could load onto a rack, which have to drive into each other.
     */
    private List<AircraftEntity> solidNeighbours(AABB area) {
        return level().getEntitiesOfClass(AircraftEntity.class, area.inflate(HULL_REACH), other -> other != this && !other.isRemoved()
                && other.definition() != null && !other.isPassenger() && !isPassengerOfSameVehicle(other)
                && other.getVehicle() != this && getVehicle() != other && !canCarry(other) && !other.canCarry(this));
    }

    /** Whether a free rack of this vehicle would take {@code cargo}. */
    private boolean canCarry(AircraftEntity cargo) {
        AircraftDefinition def = definition();
        if (def == null) {
            return false;
        }
        for (int seat = def.seatCount(); seat < def.seatAndRackCount(); seat++) {
            if (acceptsCargo(cargo, seat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Not in the original, where only the main box at a vehicle's feet collided: the whole hull
     * blocks other vehicles. A move that would push this hull into another vehicle's is undone,
     * sideways first and then completely, so a helicopter can also set down on a parked truck.
     * Hulls that already overlapped, say after two vehicles were placed into each other, may move
     * apart.
     */
    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type != MoverType.SELF || isPassenger() || definition() == null) {
            super.move(type, delta);
            return;
        }
        List<AircraftEntity> near = solidNeighbours(hullBounds().expandTowards(delta));
        if (near.isEmpty()) {
            super.move(type, delta);
            return;
        }
        List<AircraftEntity> overlapping = new ArrayList<>();
        for (AircraftEntity other : near) {
            if (hullsTouch(other, 0.0)) {
                overlapping.add(other);
            }
        }
        Vec3 before = position();
        super.move(type, delta);
        if (!entersHull(near, overlapping)) {
            return;
        }
        Vec3 m = getDeltaMovement();
        setPos(before.x, getY(), before.z);
        setDeltaMovement(0.0, m.y, 0.0);
        if (entersHull(near, overlapping)) {
            setPos(before);
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private boolean entersHull(List<AircraftEntity> near, List<AircraftEntity> overlapping) {
        for (AircraftEntity other : near) {
            if (!overlapping.contains(other) && hullsTouch(other, 0.0)) {
                return true;
            }
        }
        return false;
    }

    /** A ray that starts inside a box hits it where it starts, as it does for vanilla entity picking. */
    private static Optional<Vec3> clip(AABB box, Vec3 from, Vec3 to) {
        return box.contains(from) ? Optional.of(from) : box.clip(from, to);
    }

    public float consumeHitBoxFactor() {
        float f = hitBoxFactor;
        hitBoxFactor = 1.0F;
        return f;
    }

    // Damage

    public boolean isDestroyed() {
        return definition() != null && hp() <= 0;
    }

    /**
     * Port of the original's {@code attackEntityFrom}. Armour caps a hit at {@code ArmorMaxDamage},
     * scales it by {@code ArmorDamageFactor} and the struck box's factor, then subtracts
     * {@code ArmorMinDamage}. A player punching an empty vehicle packs it up instead.
     */
    @Override
    public boolean hurt(DamageSource source, float damage) {
        // 1.20.6 also calls this on the attacking client; only the server decides.
        if (!(level() instanceof ServerLevel level)) {
            return false;
        }
        float boxFactor = consumeHitBoxFactor();
        if (testFlightStart != null || isRemoved() || isInvulnerableTo(source) || timeSinceHit > 0) {
            return false;
        }
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.CACTUS)) {
            return false;
        }
        boolean lava = source.is(DamageTypes.LAVA);
        boolean onFire = source.is(DamageTypes.ON_FIRE);
        if (lava) {
            damage *= random.nextInt(8) + 2;
            timeSinceHit = 2;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            timeSinceHit = 1;
        } else if (source.getEntity() != null && hasPassenger(source.getEntity())) {
            return false;
        }
        if (onFire) {
            timeSinceHit = 10;
        }

        boolean creative = false;
        boolean sneaking = false;
        boolean packUp = false;
        if (source.getEntity() instanceof Player player) {
            creative = player.getAbilities().instabuild;
            sneaking = player.isShiftKeyDown();
            if (source.is(DamageTypes.PLAYER_ATTACK)) {
                packUp = creative || !hasPlayerPassenger();
            }
            ModSounds.playAt(this, "hit", 1.0F, 1.0F);
        } else {
            ModSounds.playAt(this, "helidmg", 1.0F, 0.9F + random.nextFloat() * 0.1F);
        }

        if (!isDestroyed()) {
            if (!packUp) {
                AircraftDefinition def = definition();
                if (def != null && !lava && !onFire) {
                    damage = Math.min(damage, def.armorMaxDamage);
                    if (boxFactor <= 1.0F) {
                        damage *= boxFactor;
                    }
                    damage *= def.armorDamageFactor;
                    damage -= def.armorMinDamage;
                    if (damage <= 0.0F) {
                        return false;
                    }
                    if (boxFactor > 1.0F) {
                        damage *= boxFactor;
                    }
                }
                damage *= ServerConfig.vehicleDamageFactor();
                entityData.set(DATA_HP, Math.max(hp() - (int) damage, 0));
            }
            setHurtDir(-getHurtDir());
            // No markHurt(): it sends the server's motion to every client, which snaps a vehicle
            // its pilot is flying back a step and looks like knockback on every hit.
            setHurtTime(10);
            if (hp() <= 0 || packUp) {
                if (!packUp) {
                    entityData.set(DATA_HP, 0);
                    destroyVehicle();
                    timeSinceHit = 20;
                    if (isAfloat() || isHeavy() && isNearGround(3)) {
                        startCookOff(source.getEntity());
                    } else if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.FALL)) {
                        crashExplosion();
                    } else {
                        Explosions.explode(level, getX(), getY(), getZ(),
                                new Explosions.Params(null, source.getEntity(), 2.0F, 2.0F, true, true, 5, false, null));
                    }
                } else {
                    // In creative the item only drops when sneaking, as with the original's defaults.
                    if (!creative || sneaking) {
                        spawnAtLocation(getPickResult() != null ? getPickResult() : ItemStack.EMPTY);
                    }
                    discard();
                }
            }
        } else if (packUp && creative) {
            discard();
        }
        return true;
    }

    private boolean hasPlayerPassenger() {
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof Player) {
                return true;
            }
        }
        return false;
    }

    /** {@code destroyAircraft}: the wreck tumbles, burns and disappears after 30 seconds. Server only. */
    private void destroyVehicle() {
        AircraftDefinition def = definition();
        despawnCount = def != null && def.targetDrone ? 50 : WRECK_TICKS;
        setHovering(false);
        if (def != null && def.uav) {
            Entity remote = occupant(0);
            if (remote != null) {
                remote.stopRiding();
            }
        }
        for (int seat = 0; seat < 2; seat++) {
            Entity crew = occupant(seat);
            if (crew != null && canEject(crew)) {
                eject(crew);
            }
        }
    }

    /** {@code explosionByCrash}: a wreck hitting the ground goes up in a fireball sized by its fuel. */
    private void crashExplosion() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (isHeavy()) {
            startCookOff(null);
            return;
        }
        exploded = true;
        AircraftDefinition def = definition();
        float size = Mth.clamp(def != null ? def.maxFuel / 400.0F : 2.0F, 1.0F, 15.0F);
        Vec3 at = def != null && def.fuelTankPosition != null ? toWorld(def.fuelTankPosition) : position();
        Explosions.explode(level, at.x, at.y, at.z,
                new Explosions.Params(null, null, size, size >= 2.0F ? size * 0.5F : 1.0F, true, true, 5, false, null));
        if (!isInWater()) {
            Explosions.crashFireball(level, at.x, at.y, at.z, size);
        }
    }

    /**
     * Airliners, bombers and transports ({@code MaxFuel} of {@value HEAVY_FUEL} or more): like a
     * boat, they go up in three blasts rather than one fireball.
     */
    private boolean isHeavy() {
        AircraftDefinition def = definition();
        return def != null && def.maxFuel >= HEAVY_FUEL;
    }

    /** A floating vehicle sitting on the water, such as a patrol boat or a float plane at rest. */
    private boolean isAfloat() {
        AircraftDefinition def = definition();
        return def != null && def.floats && waterDepth() > 0.0;
    }

    /**
     * A boat destroyed on the water, or a heavy aircraft wrecked on the ground, does not go up in
     * one crash fireball; its fuel and magazine cook off in three blasts over about five seconds,
     * then a hull in the water sinks. Not in the original, which only had the crash explosion.
     */
    private void startCookOff(@Nullable Entity cause) {
        exploded = true;
        cookOffBlasts = 3;
        cookOffDelay = 1;
        cookOffCause = cause;
    }

    private void tickCookOff(ServerLevel level) {
        if (cookOffBlasts <= 0 || --cookOffDelay > 0) {
            return;
        }
        AircraftDefinition def = definition();
        int blast = 4 - cookOffBlasts--;
        float fuel = Mth.clamp(def != null ? def.maxFuel / 400.0F : 2.0F, 1.0F, 15.0F);
        // The fuel tank first, then whatever the magazine still holds, then the hull goes up.
        float size = switch (blast) {
            case 1 -> fuel * 0.6F;
            case 2 -> Math.max(munitionSize(), fuel * 0.4F);
            default -> fuel;
        };
        size = Mth.clamp(size, 1.0F, 15.0F);
        Vec3 at = cookOffPoint(def);
        Explosions.explode(level, at.x, at.y, at.z,
                new Explosions.Params(null, cookOffCause, size, size >= 2.0F ? size * 0.5F : 1.0F, true, true, 5, false, null));
        // The blasts happen at the waterline, but the fire is above it, so show the full fireball.
        Explosions.crashFireball(level, at.x, at.y, at.z, size);
        if (cookOffBlasts > 0) {
            // Three blasts inside five seconds: about 1.5 to 2.5 seconds apart.
            cookOffDelay = 30 + random.nextInt(20);
        } else {
            // A boat or a flying boat goes under; a heavy aircraft on land stays where it burns.
            if (isInWater() || waterDepth() > 0.0) {
                entityData.set(DATA_SINKING, true);
            }
            cookOffCause = null;
        }
    }

    /**
     * A cooked-off wreck going under, in place of the vehicle's own physics: it settles slowly,
     * stern first with the bow raised and listing to one side, drifts to a stop and comes to rest
     * on the bottom. Out of the water it simply falls.
     */
    private void sink() {
        Vec3 m = getDeltaMovement();
        double my = isInWater() ? Math.max(m.y - 0.003, -SINK_SPEED) : m.y - 0.04;
        Vec3 motion = new Vec3(m.x * 0.9, my, m.z * 0.9);
        move(MoverType.SELF, motion);
        setDeltaMovement(onGround() ? motion.multiply(0.5, 0.0, 0.5) : motion);
        if (!onGround()) {
            // Which way it lists only has to be the same on every side, so take it from the id.
            float list = (getId() & 1) == 0 ? SINK_LIST : -SINK_LIST;
            setXRot(Mth.approach(getXRot(), -SINK_PITCH, 0.12F));
            roll = Mth.approach(roll, list, 0.15F);
        }
    }

    /**
     * How big the ammunition aboard goes up: each weapon's {@code Explosion} scaled by how full its
     * magazine and reserve still are. Guns and empty racks add nothing.
     */
    private float munitionSize() {
        if (weapons == null) {
            return 0.0F;
        }
        float size = 0.0F;
        for (WeaponSet set : weapons.sets()) {
            int rounds = set.ammo() + set.restAmmo();
            if (rounds > 0 && set.definition().explosion > 0) {
                size += set.definition().explosion * Math.min(1.0F, (float) rounds / Math.max(set.maxAmmo(), 1));
            }
        }
        return size;
    }

    /** A random spot on the hull: one of its hit boxes, or the middle of the body. */
    private Vec3 cookOffPoint(@Nullable AircraftDefinition def) {
        if (def == null || def.boundingBoxes.isEmpty() || random.nextInt(def.boundingBoxes.size() + 1) == 0) {
            return position().add(0.0, def != null ? def.bodyHeight / 2.0 : 0.5, 0.0);
        }
        return toWorld(def.boundingBoxes.get(random.nextInt(def.boundingBoxes.size())).offset());
    }

    /** Destroyed state each tick: the wreck spins, sets its crew alight and explodes on impact. */
    private void tickDestroyed() {
        if (!isDestroyed()) {
            return;
        }
        input.clear();
        throttle *= 0.98;
        crashSpin();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        tickCookOff(level);
        if (ticksAlive % 20 == 0) {
            AircraftDefinition def = definition();
            for (int seat = 0; seat < seatIds.length; seat++) {
                Entity crew = occupant(seat);
                if (crew != null && !(seat == 0 && def != null && def.uav)) {
                    crew.igniteForSeconds(5);
                }
            }
        }
        if (despawnCount > 0 && --despawnCount <= 1) {
            ejectPassengers();
            discard();
        }
    }

    /**
     * Not in the original: a shot down plane rolls into a nose-down spiral and a helicopter spins
     * with some wobble, until it is close to the ground. Tanks and cars just stop.
     */
    private void crashSpin() {
        if (!(this instanceof HelicopterEntity) && !(this instanceof PlaneEntity) || isNearGround(3) || isInWater()) {
            return;
        }
        int direction = CrashSpin.direction(getId());
        int ticks = spinTicks++;
        if (this instanceof HelicopterEntity) {
            setYRot(getYRot() + CrashSpin.helicopterYawRate(ticks, direction));
            setXRot(Mth.approach(getXRot(), CrashSpin.helicopterWobblePitch(ticks, getId()), 2.0F));
            roll = Mth.approach(roll, CrashSpin.helicopterWobbleRoll(ticks, getId()), 3.0F);
        } else {
            setYRot(getYRot() + CrashSpin.planeYawRate(ticks, direction));
            setXRot(CrashSpin.planePitch(getXRot()));
            roll = AircraftRotation.wrap(roll + CrashSpin.planeRollRate(ticks, direction));
        }
    }

    /** {@code onUpdate_Particle2}: smoke from a damaged hull, thicker the lower its hit points. */
    private void damageSmoke(AircraftDefinition def) {
        float ratio = (float) hp() / def.maxHp;
        if (ratio > 0.5F) {
            return;
        }
        int sources = 1 + def.boundingBoxes.size();
        int pick = random.nextInt(sources);
        Vec3 at = pick == 0 ? position().add(0.0, def.bodyHeight / 2.0, 0.0) : toWorld(def.boundingBoxes.get(pick - 1).offset());
        if (ratio < random.nextFloat() / 2.0F) {
            level().addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, at.x, at.y, at.z,
                    (random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.3);
        }
        if (isDestroyed()) {
            level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, at.x, at.y, at.z, 0.0, 0.07, 0.0);
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.FLAME, at.x + random.nextGaussian() * 0.5, at.y, at.z + random.nextGaussian() * 0.5, 0.0, 0.05, 0.0);
            }
            wreckFire(def);
        }
    }

    /**
     * Not in the original: the wreck keeps burning until it disappears, with a fire glow that
     * lights up the smoke at night and dies down over the last few seconds. Nothing burns under
     * water. Client only.
     */
    private void wreckFire(AircraftDefinition def) {
        wreckTicks++;
        if (isInWater()) {
            return;
        }
        float left = Math.clamp((float) (WRECK_TICKS - wreckTicks) / WRECK_FADE_TICKS, 0.15F, 1.0F);
        Vec3 centre = def.enginePosition != null ? toWorld(def.enginePosition) : position().add(0.0, def.bodyHeight / 2.0, 0.0);
        if (wreckTicks % 8 == 0) {
            // Each glow outlives the gap to the next, so the fire never goes dark in between.
            float size = Math.clamp(def.bodyWidth * 0.8F, 2.0F, 8.0F) * left;
            level().addAlwaysVisibleParticle(GlowParticleOptions.fire(GlowParticleOptions.FIRE, size, 24), true,
                    centre.x, centre.y, centre.z, 0.0, 0.02, 0.0);
        }
        if (random.nextFloat() < left) {
            double spread = def.bodyWidth * 0.4;
            level().addParticle(ParticleTypes.FLAME, centre.x + random.nextGaussian() * spread, centre.y,
                    centre.z + random.nextGaussian() * spread, 0.0, 0.04, 0.0);
        }
        if (random.nextInt(10) == 0) {
            level().addAlwaysVisibleParticle(GlowParticleOptions.fire(GlowParticleOptions.EMBER, 0.3F + random.nextFloat() * 0.3F,
                    20 + random.nextInt(20)), true, centre.x, centre.y, centre.z,
                    (random.nextDouble() - 0.5) * 0.15, 0.1 + random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.15);
        }
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        AircraftDefinition def = definition();
        return def != null ? AircraftItem.stackOf(def) : null;
    }

    // Dimensions and syncing

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        AircraftDefinition def = definition();
        return def != null ? EntityDimensions.scalable(def.bodyWidth, def.bodyHeight) : super.getDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_TYPE.equals(accessor)) {
            refreshDimensions();
            definition();
        } else if (DATA_SEATS.equals(accessor) && level().isClientSide()) {
            decodeSeats(entityData.get(DATA_SEATS));
        } else if (DATA_WEAPON_SELECTION.equals(accessor) && level().isClientSide() && weapons() != null) {
            weapons.decodeSelection(entityData.get(DATA_WEAPON_SELECTION), ClientAccess.localPlayer());
        }
    }

    /**
     * Server position updates are eased in over three ticks, as 26.x's linear interpolation does,
     * with the step vanilla's boat uses on 1.20.6.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = LERP_STEPS;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return lerpSteps > 0 ? (float) lerpXRot : getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return lerpSteps > 0 ? (float) lerpYRot : getYRot();
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, lerpXRot);
            lerpSteps--;
        }
    }

    /** A vehicle that is destroyed, packed up or killed spills what it carried; one that unloads keeps it. */
    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !level().isClientSide() && !isRemoved()) {
            inventory.dropAll();
        }
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Type", typeName());
        tag.putString("Texture", texture());
        tag.putInt("Hp", hp());
        tag.putInt("Fuel", fuel());
        tag.putFloat("Roll", roll);
        tag.putBoolean("Hovering", isHoveringMode());
        tag.putBoolean("GunnerMode", isGunnerMode());
        tag.putInt("Parts", partBits());
        tag.putInt("DespawnCount", despawnCount);
        tag.putInt("CookOff", cookOffBlasts);
        tag.putBoolean("Sinking", entityData.get(DATA_SINKING));
        tag.putBoolean("SearchLight", entityData.get(DATA_SEARCH_LIGHT));
        if (weapons != null) {
            tag.putIntArray("WeaponsAmmo", weapons.saveAmmo());
        }
        inventory.save(tag);
        if (station != null && stationReturn != null) {
            tag.putUUID("Station", station);
            tag.putDouble("StationReturnX", stationReturn.x);
            tag.putDouble("StationReturnY", stationReturn.y);
            tag.putDouble("StationReturnZ", stationReturn.z);
        }
        ListTag seats = new ListTag();
        for (int i = 0; i < seatIds.length; i++) {
            Entity e = occupant(i);
            seats.add(StringTag.valueOf(e != null ? e.getUUID().toString() : ""));
        }
        tag.put("Seats", seats);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_TYPE, tag.getString("Type"));
        entityData.set(DATA_TEXTURE, tag.getString("Texture"));
        entityData.set(DATA_PARTS, tag.getInt("Parts"));
        AircraftDefinition def = definition();
        entityData.set(DATA_HP, tag.contains("Hp") ? tag.getInt("Hp") : def != null ? def.maxHp : 1);
        // 1.0.0 and the earlier betas had no fuel; their vehicles come back with a full tank.
        entityData.set(DATA_FUEL, tag.contains("Fuel") ? tag.getInt("Fuel") : def != null ? def.maxFuel : 0);
        roll = tag.getFloat("Roll");
        setHovering(tag.getBoolean("Hovering"));
        entityData.set(DATA_GUNNER_MODE, tag.getBoolean("GunnerMode"));
        despawnCount = tag.getInt("DespawnCount");
        if (hp() <= 0 && despawnCount <= 0) {
            despawnCount = WRECK_TICKS;
        }
        cookOffBlasts = tag.getInt("CookOff");
        cookOffDelay = cookOffBlasts > 0 ? 20 : 0;
        entityData.set(DATA_SINKING, tag.getBoolean("Sinking"));
        entityData.set(DATA_SEARCH_LIGHT, tag.getBoolean("SearchLight"));
        exploded |= cookOffBlasts > 0 || entityData.get(DATA_SINKING);
        if (tag.contains("WeaponsAmmo", Tag.TAG_INT_ARRAY)) {
            int[] ammo = tag.getIntArray("WeaponsAmmo");
            if (weapons != null) {
                weapons.loadAmmo(ammo);
            } else {
                savedAmmo = ammo;
            }
        }
        inventory.load(tag);
        station = tag.hasUUID("Station") ? tag.getUUID("Station") : null;
        stationReturn = station != null ? new Vec3(tag.getDouble("StationReturnX"), tag.getDouble("StationReturnY"), tag.getDouble("StationReturnZ")) : null;
        savedSeats.clear();
        ListTag seats = tag.getList("Seats", Tag.TAG_STRING);
        for (int i = 0; i < seats.size(); i++) {
            if (!seats.getString(i).isEmpty()) {
                try {
                    savedSeats.put(UUID.fromString(seats.getString(i)), i);
                } catch (IllegalArgumentException ignored) {
                    // A hand edited save; that occupant simply takes the first free seat.
                }
            }
        }
        for (PartState part : parts) {
            part.force(part.status());
        }
        refreshDimensions();
    }

    /** Raw pilot input, filled in by the client controller every tick. */
    public static final class ControlInput {
        public boolean throttleUp;
        public boolean throttleDown;
        public boolean left;
        public boolean right;
        public boolean brake;
        /** The pilot is looking around instead of steering; planes and tanks yaw with A/D then. */
        public boolean freeLook;

        public void clear() {
            throttleUp = false;
            throttleDown = false;
            left = false;
            right = false;
            brake = false;
            freeLook = false;
        }

        public byte write() {
            return (byte) ((throttleUp ? 1 : 0) | (throttleDown ? 2 : 0) | (left ? 4 : 0) | (right ? 8 : 0)
                    | (brake ? 16 : 0) | (freeLook ? 32 : 0));
        }

        public void read(byte bits) {
            throttleUp = (bits & 1) != 0;
            throttleDown = (bits & 2) != 0;
            left = (bits & 4) != 0;
            right = (bits & 8) != 0;
            brake = (bits & 16) != 0;
            freeLook = (bits & 32) != 0;
        }
    }

    public int getHurtTime() {
        return entityData.get(DATA_HURT_TIME);
    }

    public void setHurtTime(int ticks) {
        entityData.set(DATA_HURT_TIME, ticks);
    }

    public int getHurtDir() {
        return entityData.get(DATA_HURT_DIR);
    }

    public void setHurtDir(int dir) {
        entityData.set(DATA_HURT_DIR, dir);
    }

    /** The yaw for this frame, as 26.x's {@code Entity#getYRot(float)} gives it. */
    public float getYRot(float partialTicks) {
        return partialTicks == 1.0F ? getYRot() : Mth.rotLerp(partialTicks, yRotO, getYRot());
    }

    /** The pitch for this frame, as 26.x's {@code Entity#getXRot(float)} gives it. */
    public float getXRot(float partialTicks) {
        return partialTicks == 1.0F ? getXRot() : Mth.lerp(partialTicks, xRotO, getXRot());
    }
}
