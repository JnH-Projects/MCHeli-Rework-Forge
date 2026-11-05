package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.TankDefinition;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A flight, drive or durability value on the workshop's tuning page: a single value key of the
 * definition. Until the file sets it, the page shows what the game would use, read from a blank
 * definition of the kind, so the defaults here cannot drift from the real ones.
 *
 * @param choices the values a choice key takes, or empty for a number or a switch
 * @param min     the lowest number the parser accepts
 * @param max     the highest number the parser accepts
 */
public record TuningKey(String key, Type type, List<String> choices, double min, double max,
                        Predicate<AircraftDefinition> applies, Function<AircraftDefinition, String> current) {
    public enum Type {
        NUMBER, SWITCH, CHOICE
    }

    public static final List<TuningKey> ALL = List.of(
            number("MaxHp", 1, 100000, d -> d.maxHp),
            number("Speed", 0, 1.8, d -> d.speed),
            number("MotionFactor", 0, 1, d -> d.motionFactor),
            number("Gravity", -50, 50, d -> d.gravity),
            number("GravityInWater", -50, 50, d -> d.gravityInWater),
            number("ThrottleUpDown", 0, 3, d -> d.throttleUpDown),
            number("MobilityYaw", 0, 100, d -> d.mobilityYaw),
            number("MobilityPitch", 0, 100, d -> d.mobilityPitch),
            number("MobilityRoll", 0, 100, d -> d.mobilityRoll),
            number("MobilityYawOnGround", 0, 100, d -> d.mobilityYawOnGround),
            number("RotorSpeed", -10000, 10000, d -> d.rotorSpeed),
            number("MaxFuel", 0, 100000000, d -> d.maxFuel),
            number("FuelConsumption", 0, 10000, d -> d.fuelConsumption),
            number("DamageFactor", 0, 1, d -> d.damageFactor),
            number("ArmorDamageFactor", 0, 10000, d -> d.armorDamageFactor),
            number("ArmorMinDamage", 0, 1000000, d -> d.armorMinDamage),
            number("Width", 0.1, 1000, d -> d.bodyWidth),
            number("Height", 0.1, 1000, d -> d.bodyHeight),
            number("StepHeight", 0, 1000, d -> d.stepHeight),
            number("OnGroundPitch", -90, 90, d -> -d.onGroundPitch),
            number("InventorySize", 0, 54, d -> d.inventorySize),
            bool("Float", d -> d.floats),
            bool("EnableBack", d -> d.enableBack),
            bool("Regeneration", d -> d.regeneration),
            bool("EnableGunnerMode", d -> d.gunnerMode),
            bool("EnableNightVision", d -> d.nightVision),
            bool("EnableEjectionSeat", d -> d.ejectionSeat),
            kindBool("EnableFoldBlade", HelicopterDefinition.class, d -> ((HelicopterDefinition) d).foldableBlades),
            kindBool("EnableVtol", PlaneDefinition.class, d -> ((PlaneDefinition) d).vtol),
            kindBool("VariableSweepWing", PlaneDefinition.class, d -> ((PlaneDefinition) d).variableSweepWing),
            kindNumber("VtolYaw", 0, 1, PlaneDefinition.class, d -> ((PlaneDefinition) d).vtolYaw),
            kindNumber("VtolPitch", 0.01, 1, PlaneDefinition.class, d -> ((PlaneDefinition) d).vtolPitch),
            kindNumber("SweepWingSpeed", 0, 5, PlaneDefinition.class, d -> ((PlaneDefinition) d).sweepWingSpeed),
            kindNumber("PivotTurnThrottle", 0, 1, TankDefinition.class, d -> d.pivotTurnThrottle),
            kindNumber("OnGroundPitchFactor", 0, 180, TankDefinition.class, d -> d.onGroundPitchFactor),
            kindNumber("OnGroundRollFactor", 0, 180, TankDefinition.class, d -> d.onGroundRollFactor),
            kindNumber("WeightedCenterZ", -1000, 1000, TankDefinition.class, d -> ((TankDefinition) d).weightedCenterZ),
            new TuningKey("WeightType", Type.CHOICE, List.of("unknown", "car", "tank"), 0, 0, TankDefinition.class::isInstance,
                    d -> ((TankDefinition) d).weightType.name().toLowerCase(Locale.ROOT)));

    private static TuningKey number(String key, double min, double max, Function<AircraftDefinition, Number> read) {
        return new TuningKey(key, Type.NUMBER, List.of(), min, max, d -> true, d -> WorkshopPart.number(read.apply(d).doubleValue()));
    }

    private static TuningKey kindNumber(String key, double min, double max, Class<? extends AircraftDefinition> kind,
                                        Function<AircraftDefinition, Number> read) {
        return new TuningKey(key, Type.NUMBER, List.of(), min, max, kind::isInstance, d -> WorkshopPart.number(read.apply(d).doubleValue()));
    }

    private static TuningKey bool(String key, Predicate<AircraftDefinition> read) {
        return new TuningKey(key, Type.SWITCH, List.of(), 0, 0, d -> true, d -> String.valueOf(read.test(d)));
    }

    private static TuningKey kindBool(String key, Class<? extends AircraftDefinition> kind, Predicate<AircraftDefinition> read) {
        return new TuningKey(key, Type.SWITCH, List.of(), 0, 0, kind::isInstance, d -> String.valueOf(read.test(d)));
    }

    /** Names the value in the language files, {@code gui.mcheli.workshop.tuning.<id>}. */
    public String id() {
        return key.toLowerCase(Locale.ROOT);
    }

    /** The default of this key for a vehicle kind. */
    public String defaultFor(WorkshopKind kind) {
        return current.apply(kind.create("default"));
    }

    /** Whether a number typed in is one the definition keeps as it is, not clamped or rejected. */
    public boolean accepts(@Nullable String text) {
        if (text == null || type != Type.NUMBER) {
            return false;
        }
        try {
            double value = Double.parseDouble(text.trim());
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
