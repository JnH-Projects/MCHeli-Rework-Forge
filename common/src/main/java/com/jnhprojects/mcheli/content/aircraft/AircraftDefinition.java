package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Definition;
import com.jnhprojects.mcheli.content.InvalidDefinitionException;
import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.part.CameraPart;
import com.jnhprojects.mcheli.content.aircraft.part.CrawlerTrack;
import com.jnhprojects.mcheli.content.aircraft.part.LandingGearPart;
import com.jnhprojects.mcheli.content.aircraft.part.MovingPart;
import com.jnhprojects.mcheli.content.aircraft.part.RotatingPart;
import com.jnhprojects.mcheli.content.aircraft.part.ThrottlePart;
import com.jnhprojects.mcheli.content.aircraft.part.TrackRoller;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponBayPart;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponPart;
import com.jnhprojects.mcheli.content.aircraft.part.WheelPart;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.jnhprojects.mcheli.content.DefinitionValues.lower;
import static com.jnhprojects.mcheli.content.DefinitionValues.split;
import static com.jnhprojects.mcheli.content.DefinitionValues.splitSlash;
import static com.jnhprojects.mcheli.content.DefinitionValues.toBool;
import static com.jnhprojects.mcheli.content.DefinitionValues.toDouble;
import static com.jnhprojects.mcheli.content.DefinitionValues.toFloat;
import static com.jnhprojects.mcheli.content.DefinitionValues.toInt;
import static com.jnhprojects.mcheli.content.DefinitionValues.toVec;
import static com.jnhprojects.mcheli.content.DefinitionValues.wrapDegrees;

/**
 * Keys shared by every vehicle kind (helicopters, planes, tanks and ground vehicles).
 *
 * <p>Ported from {@code MCH_AircraftInfo}. Default values and clamping ranges are kept identical
 * so the original content behaves the same, and so are the generated part names
 * ({@code hatch0}, {@code lg2}, {@code weapon5}, ...), because the models refer to them.
 */
public abstract class AircraftDefinition extends Definition {
    public static final int MAX_SEATS = 30;
    private static final Vector3 UP = new Vector3(0, 1, 0);

    // Identity and presentation
    public String displayName;
    public final Map<String, String> localizedNames = new LinkedHashMap<>();
    public String category = "zzz";
    public final List<String> textures = new ArrayList<>();
    public boolean smoothShading = true;
    public boolean hideEntity;
    public float particlesScale = 1.0F;

    // Durability
    public int maxHp = 50;
    public float damageFactor = 0.2F;
    public float armorDamageFactor = 1.0F;
    public float armorMinDamage = 0.0F;
    public float armorMaxDamage = 100000.0F;
    public float submergedDamageHeight = 0.0F;
    public boolean regeneration;

    // Fuel and supply
    public int maxFuel;
    public float fuelConsumption = 1.0F;
    public float fuelSupplyRange;
    public float ammoSupplyRange;
    /** {@code RepairOtherVehicles}: how far this vehicle repairs others, and by how much a second. */
    public float repairOtherVehiclesRange;
    public int repairOtherVehiclesValue = 10;
    public int inventorySize;

    // Body and physics
    public float bodyWidth = 2.0F;
    public float bodyHeight = 0.7F;
    public boolean floats;
    public float floatOffset;
    public float gravity = -0.04F;
    public float gravityInWater = -0.04F;
    public float speed = 0.1F;
    public float motionFactor = 0.96F;
    public float throttleUpDown = 1.0F;
    public float throttleUpDownOnEntity = 2.0F;
    public float stepHeight;
    public float onGroundPitch;
    public boolean canMoveOnGround = true;
    public boolean canRotateOnGround = true;
    public boolean enableBack;
    public final List<BoundingBoxDefinition> boundingBoxes = new ArrayList<>();
    /**
     * Not from the file: boxes for the parts of the model the {@code BoundingBox} lines leave out,
     * made by {@link com.jnhprojects.mcheli.model.HullFill} when the content loads. Empty without a
     * model.
     */
    private List<BoundingBoxDefinition> generatedBoxes = List.of();
    private @Nullable List<BoundingBoxDefinition> hull;
    public float markerWidth = 2.0F;
    public float markerHeight = 1.0F;

    // Handling
    public float mobilityYaw = 1.0F;
    public float mobilityPitch = 1.0F;
    public float mobilityRoll = 1.0F;
    public float mobilityYawOnGround = 1.0F;
    public boolean limitRotation;
    public float minRotationPitch;
    public float maxRotationPitch;
    public float minRotationRoll;
    public float maxRotationRoll;
    public float autoPilotRot = -0.6F;
    public float rotorSpeed;

    // Crew and cameras
    public boolean canRide = true;
    public final List<SeatDefinition> seats = new ArrayList<>();
    public final List<String> huds = new ArrayList<>();
    public final List<CameraPosition> cameraPositions = new ArrayList<>();
    public boolean alwaysCameraView;
    public float cameraRotationSpeed = 1000.0F;
    public int cameraZoom;
    public boolean defaultFreelook;
    public Vector3 unmountPosition;
    public Vector3 turretPosition = Vector3.ZERO;
    public boolean gunnerMode;
    public boolean concurrentGunnerMode;
    public boolean nightVision;
    public boolean entityRadar;
    public boolean ejectionSeat;
    public boolean parachuting;
    public float entityWidth = 1.0F;
    public float entityHeight = 1.0F;
    public float entityPitch;
    public float entityRoll;

    // Drones
    public boolean uav;
    public boolean smallUav;
    public boolean targetDrone;

    // Armament
    public final List<WeaponGroup> weaponGroups = new ArrayList<>();
    public final List<Integer> flareTypes = new ArrayList<>();
    public Vector3 flarePosition = Vector3.ZERO;
    public float stealth;

    // Sound
    public String sound = "";
    public float soundRange;
    public float soundVolume = 1.0F;
    public float soundPitch = 1.0F;

    // Cargo racks
    public final List<RackDefinition> racks = new ArrayList<>();
    public final List<RideRack> rideRacks = new ArrayList<>();
    /** Groups of seat indices (crew seats first, then racks) of which only one may be occupied. */
    public final List<int[]> exclusiveSeats = new ArrayList<>();

    // Ground handling
    /** Terrain probe positions from {@code SetWheelPos}, positive X only; the mirror side is implied. */
    public final List<Vector3> groundWheels = new ArrayList<>();
    public float onGroundPitchFactor;
    public float onGroundRollFactor;
    public float pivotTurnThrottle;
    public float trackRollerRot = 30.0F;
    public float partWheelRot = 30.0F;

    // Animated parts
    public final List<MovingPart> hatches = new ArrayList<>();
    public final List<MovingPart> canopies = new ArrayList<>();
    public final List<WeaponBayPart> weaponBays = new ArrayList<>();
    public final List<LandingGearPart> landingGear = new ArrayList<>();
    public final List<ThrottlePart> throttleParts = new ArrayList<>();
    public final List<RotatingPart> rotatingParts = new ArrayList<>();
    public final List<CameraPart> cameraParts = new ArrayList<>();
    public final List<WheelPart> wheels = new ArrayList<>();
    public final List<WheelPart> steeringWheels = new ArrayList<>();
    public final List<TrackRoller> trackRollers = new ArrayList<>();
    public final List<CrawlerTrack> crawlerTracks = new ArrayList<>();
    public final List<WeaponPart> weaponParts = new ArrayList<>();

    // Crafting, kept verbatim until the drafting table lands
    public final List<String> recipes = new ArrayList<>();
    public boolean shapedRecipe = true;

    protected AircraftDefinition(String name) {
        super(name);
        this.displayName = name;
        this.textures.add(name);
        this.groundWheels.addAll(defaultGroundWheels());
        this.minRotationPitch = defaultMinRotationPitch();
        this.maxRotationPitch = defaultMaxRotationPitch();
        this.minRotationRoll = defaultMinRotationPitch();
        this.maxRotationRoll = defaultMaxRotationPitch();
        this.rotorSpeed = defaultRotorSpeed();
        this.soundRange = defaultSoundRange();
        this.cameraZoom = defaultMaxZoom();
    }

    /** Asset folder for this kind, for example {@code helicopters}. */
    public abstract String folder();

    public List<BoundingBoxDefinition> generatedBoxes() {
        return generatedBoxes;
    }

    public void setGeneratedBoxes(List<BoundingBoxDefinition> boxes) {
        generatedBoxes = List.copyOf(boxes);
        hull = null;
    }

    /** Every hit box besides the main one: the {@code BoundingBox} lines, then the generated boxes. */
    public List<BoundingBoxDefinition> hull() {
        List<BoundingBoxDefinition> h = hull;
        if (h == null) {
            List<BoundingBoxDefinition> all = new ArrayList<>(boundingBoxes);
            all.addAll(generatedBoxes);
            h = List.copyOf(all);
            hull = h;
        }
        return h;
    }

    /** HUD used for a seat that the definition did not configure. */
    protected abstract String defaultHud(int seatIndex);

    protected float maxSpeed() {
        return 0.8F;
    }

    protected float defaultMinRotationPitch() {
        return -89.9F;
    }

    protected float defaultMaxRotationPitch() {
        return 80.0F;
    }

    protected float defaultRotorSpeed() {
        return 0.0F;
    }

    protected float defaultSoundRange() {
        return 100.0F;
    }

    protected int defaultMaxZoom() {
        return 1;
    }

    public String defaultSound() {
        return "";
    }

    /** Terrain probes used when the file has no {@code SetWheelPos}. */
    protected List<Vector3> defaultGroundWheels() {
        return List.of();
    }

    /** Kind name as racks refer to it, for example {@code plane}. */
    public abstract String kind();

    public String soundOrDefault() {
        return sound.isEmpty() ? defaultSound() : sound;
    }

    /** Crew seats, without racks. Seat zero is the pilot. */
    public int seatCount() {
        return seats.size();
    }

    /**
     * Rows of the cargo chest: {@code InventorySize} rounded up to whole rows of nine, the only
     * sizes a chest screen comes in. Zero means the vehicle has no chest.
     */
    public int cargoRows() {
        return (inventorySize + 8) / 9;
    }

    /**
     * Seats that can bail out, one parachute slot each in the vehicle screen: seats zero and one
     * with {@code EnableEjectionSeat}, except the pilot seat of a drone, whose pilot is not aboard.
     */
    public int ejectionSeats() {
        if (!ejectionSeat) {
            return 0;
        }
        int seats = Math.min(seatCount(), 2);
        return uav && seats > 0 ? seats - 1 : seats;
    }

    /** Crew seats followed by racks, the index space used by {@code ExclusionSeat} and the seat map. */
    public int seatAndRackCount() {
        return seats.size() + racks.size();
    }

    /** The rack behind a combined seat index, or {@code null} for a crew seat. */
    public RackDefinition rack(int seatIndex) {
        int i = seatIndex - seats.size();
        return i >= 0 && i < racks.size() ? racks.get(i) : null;
    }

    public boolean hasLandingGear() {
        return !landingGear.isEmpty();
    }

    @Override
    protected void registerKeys(KeyTable keys) {
        keys.put("DisplayName", v -> displayName = v);
        keys.put("AddDisplayName", v -> {
            String[] s = split(v);
            if (s.length == 2) {
                localizedNames.put(lower(s[0]), s[1]);
            }
        });
        keys.put("Category", v -> category = v.toUpperCase(Locale.ROOT).replaceAll("[,;:]", ".").replaceAll("[ \t]", ""));
        keys.put("AddTexture", v -> textures.add(lower(v)));
        keys.put("SmoothShading", v -> smoothShading = toBool(v));
        keys.put("HideEntity", v -> hideEntity = toBool(v));
        keys.put("ParticlesScale", v -> particlesScale = toFloat(v, 0.0F, 50.0F));
        // ItemID only mattered before 1.7 removed numeric ids; accept it so it is not reported.
        keys.put("ItemID", v -> { });

        keys.put("MaxHp", v -> maxHp = toInt(v, 1, 100000));
        keys.put("DamageFactor", v -> damageFactor = toFloat(v, 0.0F, 1.0F));
        keys.put("ArmorDamageFactor", v -> armorDamageFactor = toFloat(v, 0.0F, 10000.0F));
        keys.put("ArmorMinDamage", v -> armorMinDamage = toFloat(v, 0.0F, 1000000.0F));
        keys.put("ArmorMaxDamage", v -> armorMaxDamage = toFloat(v, 0.0F, 1000000.0F));
        keys.put("SubmergedDamageHeight", v -> submergedDamageHeight = toFloat(v, -1000.0F, 1000.0F));
        keys.put("Regeneration", v -> regeneration = toBool(v));

        keys.put("MaxFuel", v -> maxFuel = toInt(v, 0, 100000000));
        keys.put("FuelConsumption", v -> fuelConsumption = toFloat(v, 0.0F, 10000.0F));
        keys.put("FuelSupplyRange", v -> fuelSupplyRange = toFloat(v, 0.0F, 1000.0F));
        keys.put("AmmoSupplyRange", v -> ammoSupplyRange = toFloat(v, 0.0F, 1000.0F));
        keys.put("RepairOtherVehicles", v -> {
            String[] s = split(v);
            repairOtherVehiclesRange = toFloat(s[0], 0.0F, 1000.0F);
            if (s.length >= 2) {
                repairOtherVehiclesValue = toInt(s[1], 0, 10000000);
            }
        });
        keys.put("InventorySize", v -> inventorySize = toInt(v, 0, 54));

        keys.put("Width", v -> bodyWidth = toFloat(v, 0.1F, 1000.0F));
        keys.put("Height", v -> bodyHeight = toFloat(v, 0.1F, 1000.0F));
        keys.put("Float", v -> floats = toBool(v));
        keys.put("FloatOffset", v -> floatOffset = -toFloat(v));
        keys.put("Gravity", v -> gravity = toFloat(v, -50.0F, 50.0F));
        keys.put("GravityInWater", v -> gravityInWater = toFloat(v, -50.0F, 50.0F));
        keys.put("Speed", v -> speed = toFloat(v, 0.0F, maxSpeed()));
        keys.put("MotionFactor", v -> motionFactor = toFloat(v, 0.0F, 1.0F));
        keys.put("ThrottleUpDown", v -> throttleUpDown = toFloat(v, 0.0F, 3.0F));
        keys.put("ThrottleUpDownOnEntity", v -> throttleUpDownOnEntity = toFloat(v, 0.0F, 100000.0F));
        keys.put("StepHeight", v -> stepHeight = toFloat(v, 0.0F, 1000.0F));
        keys.put("OnGroundPitch", v -> onGroundPitch = -toFloat(v, -90.0F, 90.0F));
        keys.put("CanMoveOnGround", v -> canMoveOnGround = toBool(v));
        keys.put("CanRotOnGround", v -> canRotateOnGround = toBool(v));
        keys.put("EnableBack", v -> enableBack = toBool(v));
        keys.put("BoundingBox", this::parseBoundingBox);

        keys.put("MobilityYaw", v -> mobilityYaw = toFloat(v, 0.0F, 100.0F));
        keys.put("MobilityPitch", v -> mobilityPitch = toFloat(v, 0.0F, 100.0F));
        keys.put("MobilityRoll", v -> mobilityRoll = toFloat(v, 0.0F, 100.0F));
        keys.put("MobilityYawOnGround", v -> mobilityYawOnGround = toFloat(v, 0.0F, 100.0F));
        keys.put("MinRotationPitch", v -> {
            limitRotation = true;
            minRotationPitch = toFloat(v, defaultMinRotationPitch(), 0.0F);
        });
        keys.put("MaxRotationPitch", v -> {
            limitRotation = true;
            maxRotationPitch = toFloat(v, 0.0F, defaultMaxRotationPitch());
        });
        // The original clamps roll against fixed bounds rather than the overridable pitch ones.
        keys.put("MinRotationRoll", v -> {
            limitRotation = true;
            minRotationRoll = toFloat(v, -80.0F, 0.0F);
        });
        keys.put("MaxRotationRoll", v -> {
            limitRotation = true;
            maxRotationRoll = toFloat(v, 0.0F, 80.0F);
        });
        keys.put("AutoPilotRot", v -> autoPilotRot = toFloat(v, -5.0F, 5.0F));
        keys.put("RotorSpeed", v -> {
            float s = toFloat(v, -10000.0F, 10000.0F);
            // Small offset inherited from the original, it stops the blades from looking frozen
            // when the speed divides 360 evenly.
            if (s > 0.01F) {
                s -= 0.01F;
            } else if (s < -0.01F) {
                s += 0.01F;
            }
            rotorSpeed = s;
        });

        keys.put("CanRide", v -> canRide = toBool(v, true));
        keys.putAliased("AddSeat", this::parseSeat);
        keys.putAliased("AddGunnerSeat", this::parseSeat);
        keys.putAliased("AddFixRotSeat", this::parseSeat);
        keys.put("HUD", v -> {
            huds.clear();
            for (String hud : split(v)) {
                huds.add(lower(hud));
            }
        });
        keys.put("CameraPosition", v -> {
            String[] s = split(v);
            if (s.length >= 3) {
                alwaysCameraView = s.length >= 4 && toBool(s[3]);
                boolean fixed = s.length >= 5;
                float yaw = s.length >= 5 ? toFloat(s[4]) : 0.0F;
                float pitch = s.length >= 6 ? toFloat(s[5]) : 0.0F;
                cameraPositions.add(new CameraPosition(toVec(s[0], s[1], s[2]), fixed, yaw, pitch));
            }
        });
        keys.put("CameraRotationSpeed", v -> cameraRotationSpeed = toFloat(v, 0.0F, 10000.0F));
        keys.put("CameraZoom", v -> cameraZoom = toInt(v, 1, 10));
        keys.put("DefaultFreelook", v -> defaultFreelook = toBool(v));
        keys.put("UnmountPosition", v -> {
            String[] s = split(v);
            if (s.length >= 3) {
                unmountPosition = toVec(s[0], s[1], s[2]);
            }
        });
        keys.put("TurretPosition", v -> {
            String[] s = split(v);
            if (s.length >= 3) {
                turretPosition = toVec(s[0], s[1], s[2]);
            }
        });
        keys.put("EnableGunnerMode", v -> gunnerMode = toBool(v));
        keys.put("ConcurrentGunnerMode", v -> concurrentGunnerMode = toBool(v));
        keys.put("EnableNightVision", v -> nightVision = toBool(v));
        keys.put("EnableEntityRadar", v -> entityRadar = toBool(v));
        keys.put("EnableEjectionSeat", v -> ejectionSeat = toBool(v));
        keys.put("EnableParachuting", v -> parachuting = toBool(v));
        keys.put("EntityWidth", v -> entityWidth = toFloat(v, -100.0F, 100.0F));
        keys.put("EntityHeight", v -> entityHeight = toFloat(v, -100.0F, 100.0F));
        keys.put("EntityPitch", v -> entityPitch = toFloat(v, -360.0F, 360.0F));
        keys.put("EntityRoll", v -> entityRoll = toFloat(v, -360.0F, 360.0F));

        keys.put("UAV", v -> {
            uav = toBool(v);
            smallUav = false;
        });
        keys.put("SmallUAV", v -> {
            uav = toBool(v);
            smallUav = true;
        });
        keys.put("TargetDrone", v -> targetDrone = toBool(v));

        keys.putAliased("AddWeapon", this::parseWeapon);
        keys.putAliased("AddTurretWeapon", this::parseWeapon);
        keys.put("FlareType", v -> {
            flareTypes.clear();
            for (String s : split(v)) {
                flareTypes.add(toInt(s, 1, 10));
            }
        });
        keys.put("FlareOption", v -> {
            String[] s = split(v);
            if (s.length >= 3) {
                flarePosition = toVec(s[0], s[1], s[2]);
            }
        });
        keys.put("Stealth", v -> stealth = toFloat(v, 0.0F, 1.0F));

        keys.put("Sound", v -> sound = lower(v));
        keys.put("SoundRange", v -> soundRange = toFloat(v, 1.0F, 1000.0F));
        keys.put("SoundVolume", v -> soundVolume = toFloat(v, 0.0F, 10.0F));
        keys.put("SoundPitch", v -> soundPitch = toFloat(v, 0.0F, 10.0F));

        keys.put("AddRecipe", v -> {
            shapedRecipe = true;
            recipes.add(v.toUpperCase(Locale.ROOT));
        });
        keys.put("AddShapelessRecipe", v -> {
            shapedRecipe = false;
            recipes.add(v.toUpperCase(Locale.ROOT));
        });

        keys.put("AddRack", this::parseRack);
        keys.put("RideRack", v -> {
            String[] s = split(v);
            if (s.length >= 2) {
                rideRacks.add(new RideRack(lower(s[0]), toInt(s[1], 1, 10000)));
            }
        });
        keys.put("ExclusionSeat", v -> {
            String[] s = split(v);
            if (s.length >= 2) {
                int[] group = new int[s.length];
                for (int i = 0; i < s.length; i++) {
                    group[i] = toInt(s[i], 1, 10000) - 1;
                }
                exclusiveSeats.add(group);
            }
        });

        keys.put("SetWheelPos", this::parseWheelPositions);
        keys.put("OnGroundPitchFactor", v -> onGroundPitchFactor = toFloat(v, 0.0F, 180.0F));
        keys.put("OnGroundRollFactor", v -> onGroundRollFactor = toFloat(v, 0.0F, 180.0F));
        keys.put("PivotTurnThrottle", v -> pivotTurnThrottle = toFloat(v, 0.0F, 1.0F));
        keys.put("TrackRollerRot", v -> trackRollerRot = toFloat(v, -10000.0F, 10000.0F));
        keys.put("PartWheelRot", v -> partWheelRot = toFloat(v, -10000.0F, 10000.0F));

        keys.putAliased("AddPartHatch", this::parseHatch);
        keys.putAliased("AddPartSlideHatch", this::parseHatch);
        keys.putAliased("AddPartCanopy", this::parseCanopy);
        keys.putAliased("AddPartSlideCanopy", this::parseCanopy);
        keys.putAliased("AddPartWeaponBay", this::parseWeaponBay);
        keys.putAliased("AddPartSlideWeaponBay", this::parseWeaponBay);
        keys.putAliased("AddPartLG", this::parseLandingGear);
        keys.putAliased("AddPartLGRev", this::parseLandingGear);
        keys.putAliased("AddPartLGHatch", this::parseLandingGear);
        keys.putAliased("AddPartSlideRotLG", this::parseLandingGear);
        keys.put("AddPartThrottle", this::parseThrottle);
        keys.put("AddPartRotation", this::parseRotation);
        keys.put("AddPartCamera", this::parseCamera);
        keys.put("AddPartWheel", this::parseWheel);
        keys.put("AddPartSteeringWheel", this::parseSteeringWheel);
        keys.put("AddTrackRoller", v -> {
            String[] s = split(v);
            if (s.length >= 3) {
                Vector3 pos = toVec(s[0], s[1], s[2]);
                trackRollers.add(new TrackRoller("track_roller" + trackRollers.size(), pos, pos.x() >= 0.0 ? 1 : 0));
            }
        });
        keys.put("AddCrawlerTrack", this::parseCrawlerTrack);
        keys.putAliased("AddPartWeapon", this::parseWeaponPart);
        keys.putAliased("AddPartRotWeapon", this::parseWeaponPart);
        keys.putAliased("AddPartTurretWeapon", this::parseWeaponPart);
        keys.putAliased("AddPartTurretRotWeapon", this::parseWeaponPart);
        keys.putAliased("AddPartWeaponMissile", this::parseWeaponPart);
        keys.put("AddPartWeaponChild", this::parseWeaponChild);
    }

    private void parseRack(String data) {
        String[] s = split(lower(data));
        if (s.length < 7) {
            return;
        }
        List<String> accepts = List.of(splitSlash(s[0]));
        float range = s.length >= 8 ? toFloat(s[7]) : 6.0F;
        float parachute = s.length >= 9 ? toFloat(s[8], 0.0F, 1000000.0F) : 20.0F;
        float yaw = s.length >= 10 ? toFloat(s[9]) : 0.0F;
        float pitch = s.length >= 11 ? toFloat(s[10]) : 0.0F;
        boolean rotating = s.length >= 12 && toBool(s[11]);
        racks.add(new RackDefinition(accepts, toVec(s[1], s[2], s[3]), toVec(s[4], s[5], s[6]).add(0.0, 1.5, 0.0),
                range, parachute, yaw, pitch, rotating));
    }

    private void parseWheelPositions(String data) {
        String[] s = split(data);
        if (s.length < 4) {
            return;
        }
        double x = Math.abs(toFloat(s[0]));
        double y = toFloat(s[1]);
        groundWheels.clear();
        for (int i = 2; i < s.length; i++) {
            groundWheels.add(new Vector3(x, y, toFloat(s[i])));
        }
        // Front to back, which the terrain follower relies on.
        groundWheels.sort(Comparator.comparingDouble(Vector3::z).reversed());
    }

    private void parseHatch(String key, String data) {
        String[] s = split(data);
        String part = "hatch" + hatches.size();
        if (key.equals("addpartslidehatch")) {
            if (s.length >= 3) {
                hatches.add(new MovingPart(part, toVec(s[0], s[1], s[2]), Vector3.ZERO, 1.0F, true));
            }
        } else if (s.length >= 6) {
            float max = s.length >= 7 ? toFloat(s[6], -180.0F, 180.0F) : 90.0F;
            hatches.add(new MovingPart(part, toVec(s[0], s[1], s[2]), toVec(s[3], s[4], s[5]), max / 90.0F, false));
        }
    }

    private void parseCanopy(String key, String data) {
        String[] s = split(data);
        boolean slide = key.equals("addpartslidecanopy");
        // The first canopy is registered twice, as canopy0 and canopy, because models use both names.
        int number = Math.max(canopies.size() - 1, 0);
        MovingPart canopy;
        if (slide) {
            if (s.length < 3) {
                return;
            }
            canopy = new MovingPart("canopy" + number, toVec(s[0], s[1], s[2]), Vector3.ZERO, 1.0F, true);
        } else {
            if (s.length < 6) {
                return;
            }
            float max = s.length >= 7 ? toFloat(s[6], -180.0F, 180.0F) : 90.0F;
            canopy = new MovingPart("canopy" + number, toVec(s[0], s[1], s[2]), toVec(s[3], s[4], s[5]), max / 90.0F, false);
        }
        canopies.add(canopy);
        if (number == 0) {
            canopies.add(new MovingPart("canopy", canopy.position(), canopy.axis(), canopy.factor(), slide));
        }
    }

    private void parseWeaponBay(String key, String data) {
        String[] s = split(data);
        String part = "wb" + weaponBays.size();
        MovingPart door;
        if (key.equals("addpartslideweaponbay")) {
            if (s.length < 4) {
                return;
            }
            door = new MovingPart(part, toVec(s[1], s[2], s[3]), Vector3.ZERO, 1.0F, true);
        } else {
            if (s.length < 7) {
                return;
            }
            float max = s.length >= 8 ? toFloat(s[7], -180.0F, 180.0F) : 90.0F;
            door = new MovingPart(part, toVec(s[1], s[2], s[3]), toVec(s[4], s[5], s[6]), max / 90.0F, false);
        }
        weaponBays.add(new WeaponBayPart(door, List.of(splitSlash(lower(s[0])))));
    }

    private void parseLandingGear(String key, String data) {
        String[] s = split(data);
        String part = "lg" + landingGear.size();
        if (key.equals("addpartsliderotlg")) {
            if (s.length >= 9) {
                float max = s.length >= 10 ? toFloat(s[9], -180.0F, 180.0F) : 90.0F;
                landingGear.add(new LandingGearPart(part, toVec(s[3], s[4], s[5]), toVec(s[6], s[7], s[8]), max / 90.0F,
                        false, false, null, 0.0F, toVec(s[0], s[1], s[2])));
            }
            return;
        }
        if (s.length < 6) {
            return;
        }
        float max = s.length >= 7 ? toFloat(s[6], -180.0F, 180.0F) : 90.0F;
        Vector3 secondAxis = null;
        float secondFactor = 0.0F;
        if (s.length >= 10) {
            secondAxis = toVec(s[7], s[8], s[9]);
            secondFactor = (s.length >= 11 ? toFloat(s[10], -180.0F, 180.0F) : 90.0F) / 90.0F;
        }
        landingGear.add(new LandingGearPart(part, toVec(s[0], s[1], s[2]), toVec(s[3], s[4], s[5]), max / 90.0F,
                key.equals("addpartlgrev"), key.equals("addpartlghatch"), secondAxis, secondFactor, null));
    }

    private void parseThrottle(String data) {
        String[] s = split(data);
        if (s.length < 7) {
            return;
        }
        Vector3 slide = new Vector3(
                s.length >= 8 ? toFloat(s[7]) : 0.0F,
                s.length >= 9 ? toFloat(s[8]) : 0.0F,
                s.length >= 10 ? toFloat(s[9]) : 0.0F);
        throttleParts.add(new ThrottlePart("throttle" + throttleParts.size(), toVec(s[0], s[1], s[2]), toVec(s[3], s[4], s[5]),
                toFloat(s[6]), slide));
    }

    private void parseRotation(String data) {
        String[] s = split(data);
        if (s.length < 7) {
            return;
        }
        boolean always = s.length < 8 || toBool(s[7]);
        // The original numbered these after the throttle list, and the models were made to match.
        rotatingParts.add(new RotatingPart("rotpart" + throttleParts.size(), toVec(s[0], s[1], s[2]), toVec(s[3], s[4], s[5]),
                toFloat(s[6]), always));
    }

    private void parseCamera(String data) {
        String[] s = split(data);
        if (s.length < 3) {
            return;
        }
        boolean yawSync = s.length < 4 || toBool(s[3]);
        boolean pitchSync = s.length >= 5 && toBool(s[4]);
        cameraParts.add(new CameraPart("camera" + cameraParts.size(), toVec(s[0], s[1], s[2]), yawSync, pitchSync));
    }

    private void parseWheel(String data) {
        String[] s = split(data);
        if (s.length < 3) {
            return;
        }
        Vector3 pos = toVec(s[0], s[1], s[2]);
        float steer = s.length >= 4 ? toFloat(s[3], -1800.0F, 1800.0F) : 0.0F;
        Vector3 axis = s.length >= 7 ? toVec(s[4], s[5], s[6]) : UP;
        Vector3 pivot = s.length >= 10 ? toVec(s[7], s[8], s[9]) : pos;
        wheels.add(new WheelPart("wheel" + wheels.size(), pos, axis, steer, pivot));
    }

    private void parseSteeringWheel(String data) {
        String[] s = split(data);
        if (s.length < 7) {
            return;
        }
        Vector3 pos = toVec(s[0], s[1], s[2]);
        steeringWheels.add(new WheelPart("steering_wheel" + steeringWheels.size(), pos, toVec(s[3], s[4], s[5]), toFloat(s[6]), pos));
    }

    private void parseCrawlerTrack(String data) {
        String[] s = split(data);
        if (s.length < 7) {
            return;
        }
        int count = s.length - 3;
        float[] ys = new float[count];
        float[] zs = new float[count];
        for (int i = 0; i < count; i++) {
            String[] yz = splitSlash(s[3 + i]);
            ys[i] = toFloat(yz[0]);
            zs[i] = toFloat(yz[1]);
        }
        CrawlerTrack track = CrawlerTrack.create("crawler_track" + crawlerTracks.size(), toBool(s[0]), toFloat(s[1]), toFloat(s[2]), ys, zs);
        if (track != null) {
            crawlerTracks.add(track);
        }
    }

    private void parseWeaponPart(String key, String data) {
        String[] s = split(data);
        if (s.length < 7) {
            return;
        }
        boolean rotating = key.equals("addpartrotweapon") || key.equals("addpartturretrotweapon");
        boolean turret = key.equals("addpartturretweapon") || key.equals("addpartturretrotweapon");
        Vector3 axis = Vector3.ZERO;
        float recoil = 0.0F;
        if (rotating) {
            axis = s.length >= 10 ? toVec(s[7], s[8], s[9]) : new Vector3(0, 0, -1);
        } else if (s.length >= 8) {
            recoil = toFloat(s[7]);
        }
        weaponParts.add(new WeaponPart("weapon" + weaponParts.size(), List.of(splitSlash(lower(s[0]))), rotating,
                key.equals("addpartweaponmissile"), toBool(s[1]), toBool(s[2]), toBool(s[3]),
                toVec(s[4], s[5], s[6]), axis, recoil, turret, List.of()));
    }

    private void parseWeaponChild(String data) {
        String[] s = split(data);
        if (s.length < 5 || weaponParts.isEmpty()) {
            return;
        }
        WeaponPart parent = weaponParts.getLast();
        float recoil = s.length >= 6 ? toFloat(s[5]) : 0.0F;
        parent.children().add(new WeaponPart.Child(parent.part() + "_" + parent.children().size(),
                toBool(s[0]), toBool(s[1]), toVec(s[2], s[3], s[4]), recoil));
    }

    private void parseSeat(String key, String data) {
        if (seats.size() >= MAX_SEATS) {
            return;
        }
        String[] s = split(data);
        if (s.length < 3) {
            return;
        }
        Vector3 pos = toVec(s[0], s[1], s[2]);

        if (key.equals("addseat")) {
            seats.add(SeatDefinition.passenger(pos, s.length >= 4 && toBool(s[3])));
            return;
        }
        if (s.length < 6) {
            seats.add(new SeatDefinition(pos, true, CameraPosition.DEFAULT, false, false, 0, 0,
                    SeatDefinition.DEFAULT_MIN_PITCH, SeatDefinition.DEFAULT_MAX_PITCH, false));
            return;
        }

        CameraPosition camera = new CameraPosition(toVec(s[3], s[4], s[5]));
        boolean switchGunner = s.length >= 7 && toBool(s[6]);
        if (key.equals("addgunnerseat")) {
            float minPitch = SeatDefinition.DEFAULT_MIN_PITCH;
            float maxPitch = SeatDefinition.DEFAULT_MAX_PITCH;
            boolean rotating = false;
            if (s.length >= 9) {
                minPitch = toFloat(s[7], -90.0F, 90.0F);
                maxPitch = toFloat(s[8], -90.0F, 90.0F);
                if (minPitch > maxPitch) {
                    float t = minPitch;
                    minPitch = maxPitch;
                    maxPitch = t;
                }
                rotating = s.length >= 10 && toBool(s[9]);
            }
            seats.add(new SeatDefinition(pos, true, camera, switchGunner, false, 0, 0, minPitch, maxPitch, rotating));
        } else {
            boolean fixed = s.length >= 9;
            float yaw = fixed ? toFloat(s[7]) : 0.0F;
            float pitch = fixed ? toFloat(s[8]) : 0.0F;
            boolean rotating = s.length >= 10 && toBool(s[9]);
            seats.add(new SeatDefinition(pos, true, camera, switchGunner, fixed, yaw, pitch,
                    SeatDefinition.DEFAULT_MIN_PITCH, SeatDefinition.DEFAULT_MAX_PITCH, rotating));
        }
    }

    private void parseWeapon(String key, String data) {
        String[] s = split(data);
        if (s.length < 4) {
            return;
        }
        String type = lower(s[0]);
        float yaw = s.length >= 5 ? toFloat(s[4]) : 0.0F;
        float pitch = s.length >= 6 ? toFloat(s[5]) : 0.0F;
        boolean pilot = s.length < 7 || toBool(s[6]);
        int seat = s.length >= 8 ? toInt(s[7], 1, MAX_SEATS) - 1 : 0;
        if (seat <= 0) {
            pilot = true;
        }
        float defaultYaw = s.length >= 9 ? wrapDegrees(toFloat(s[8])) : 0.0F;
        float minYaw = s.length >= 10 ? toFloat(s[9]) : 0.0F;
        float maxYaw = s.length >= 11 ? toFloat(s[10]) : 0.0F;
        float minPitch = s.length >= 12 ? toFloat(s[11]) : 0.0F;
        float maxPitch = s.length >= 13 ? toFloat(s[12]) : 0.0F;

        WeaponMount mount = new WeaponMount(toVec(s[1], s[2], s[3]), yaw, pitch, pilot, seat,
                defaultYaw, minYaw, maxYaw, minPitch, maxPitch, key.equals("addturretweapon"));

        WeaponGroup last = weaponGroups.isEmpty() ? null : weaponGroups.getLast();
        if (last == null || !last.weaponType().equals(type)) {
            last = new WeaponGroup(type, new ArrayList<>());
            weaponGroups.add(last);
        }
        last.mounts().add(mount);
    }

    private void parseBoundingBox(String data) {
        String[] s = split(data);
        if (s.length < 5) {
            return;
        }
        float factor = s.length >= 6 ? toFloat(s[5]) : 1.0F;
        Vector3 offset = new Vector3(toDouble(s[0]), toDouble(s[1]), toDouble(s[2]));
        float width = toFloat(s[3]);
        float height = toFloat(s[4]);
        boundingBoxes.add(new BoundingBoxDefinition(offset, width, height, factor));

        markerHeight = Math.max(markerHeight, (float) (offset.y() + height / 2.0));
        double halfWidth = width / 2.0;
        markerWidth = (float) Math.max(markerWidth, Math.abs(offset.x() + halfWidth) / 2.0);
        markerWidth = (float) Math.max(markerWidth, Math.abs(offset.x() - halfWidth) / 2.0);
        markerWidth = (float) Math.max(markerWidth, Math.abs(offset.z() + halfWidth) / 2.0);
        markerWidth = (float) Math.max(markerWidth, Math.abs(offset.z() - halfWidth) / 2.0);
    }

    @Override
    protected void validate() throws InvalidDefinitionException {
        if (cameraPositions.isEmpty()) {
            cameraPositions.add(CameraPosition.DEFAULT);
        }
        if (targetDrone) {
            uav = true;
        }
        if (uav) {
            alwaysCameraView = true;
            if (seats.isEmpty()) {
                seats.add(SeatDefinition.passenger(Vector3.ZERO, false));
            }
        }
        if (seats.isEmpty()) {
            throw new InvalidDefinitionException("no seats defined");
        }
        for (int i = huds.size(); i < seats.size(); i++) {
            huds.add(defaultHud(i));
        }
        if (seats.size() == 1 && huds.size() == 1) {
            huds.add(defaultHud(1));
        }
        // A bay that opens for no weapon this vehicle carries would never move.
        weaponBays.removeIf(bay -> bay.weapons().stream().noneMatch(w -> weaponGroup(w) >= 0));
    }

    /**
     * Drops weapon lines naming a weapon that is not loaded. The original skipped those lines while
     * reading, so the groups either side of one merge when they fire the same weapon.
     *
     * @return the names that were dropped
     */
    public List<String> retainWeapons(Set<String> known) {
        List<String> dropped = new ArrayList<>();
        List<WeaponGroup> kept = new ArrayList<>();
        for (WeaponGroup group : weaponGroups) {
            if (!known.contains(group.weaponType())) {
                dropped.add(group.weaponType());
            } else if (!kept.isEmpty() && kept.getLast().weaponType().equals(group.weaponType())) {
                kept.getLast().mounts().addAll(group.mounts());
            } else {
                kept.add(group);
            }
        }
        weaponGroups.clear();
        weaponGroups.addAll(kept);
        weaponBays.removeIf(bay -> bay.weapons().stream().noneMatch(w -> weaponGroup(w) >= 0));
        return dropped;
    }

    /** Index of the weapon group firing {@code weaponType}, or -1. */
    public int weaponGroup(String weaponType) {
        for (int i = 0; i < weaponGroups.size(); i++) {
            if (weaponGroups.get(i).weaponType().equalsIgnoreCase(weaponType)) {
                return i;
            }
        }
        return -1;
    }

    /** Texture names in declaration order. The first one is what a freshly placed vehicle uses. */
    public List<String> textures() {
        return Collections.unmodifiableList(textures);
    }

    /** Texture that follows {@code current} when the player cycles paint jobs. */
    public String nextTexture(String current) {
        int i = textures.indexOf(current);
        return i < 0 ? textures.getFirst() : textures.get((i + 1) % textures.size());
    }
}
