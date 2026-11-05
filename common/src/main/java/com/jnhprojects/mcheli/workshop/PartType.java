package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.DefinitionValues;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static com.jnhprojects.mcheli.workshop.PartField.bool;
import static com.jnhprojects.mcheli.workshop.PartField.number;
import static com.jnhprojects.mcheli.workshop.PartField.position;
import static com.jnhprojects.mcheli.workshop.PartField.rest;
import static com.jnhprojects.mcheli.workshop.PartField.vector;
import static com.jnhprojects.mcheli.workshop.PartField.weapon;
import static com.jnhprojects.mcheli.workshop.PartField.whole;

/**
 * Every line the workshop can place, with the layout of its values. Each entry mirrors a parser in
 * {@code AircraftDefinition} or its subclasses: the field indices are the ones that parser reads,
 * the lengths are the value counts it accepts, and the first values of a new line are its fallback.
 */
public enum PartType {
    SEAT(Group.SEATS, "AddSeat", all(), lengths(3, 4), position(0), bool("rotating_seat", 3, "false")),
    GUNNER_SEAT(Group.SEATS, "AddGunnerSeat", all(), lengths(3, 6, 7, 9, 10),
            position(0), position("camera", 3), bool("switch_gunner", 6, "false"),
            number("min_pitch", 7, "-30"), number("max_pitch", 8, "70"), bool("rotating_seat", 9, "false")),
    FIXED_SEAT(Group.SEATS, "AddFixRotSeat", all(), lengths(3, 6, 7, 9, 10),
            position(0), position("camera", 3), bool("switch_gunner", 6, "false"),
            number("yaw", 7, "0"), number("pitch", 8, "0"), bool("rotating_seat", 9, "false")),

    CAMERA(Group.CAMERAS, "CameraPosition", all(), lengths(3, 4, 5, 6),
            position(0), bool("always", 3, "false"), number("yaw", 4, "0"), number("pitch", 5, "0")),
    CAMERA_TURRET(Group.CAMERAS, "AddPartCamera", all(), lengths(3, 4, 5),
            position(0), bool("yaw_sync", 3, "true"), bool("pitch_sync", 4, "false")),

    ROTOR(Group.ROTORS, "AddRotor", only(WorkshopKind.HELICOPTER), lengths(8, 9),
            whole("blades", 0, "2"), whole("blade_step", 1, "180"), position(2), vector("axis", 5, "0, 1, 0"), bool("foldable", 8, "false")),
    PROPELLER(Group.ROTORS, "AddPartRotor", only(WorkshopKind.PLANE), lengths(6, 7),
            position(0), vector("axis", 3, "0, 0, 1"), number("max_angle", 6, "90")),
    BLADE(Group.ROTORS, "AddBlade", only(WorkshopKind.PLANE), PROPELLER, lengths(8),
            whole("blades", 0, "2"), whole("blade_step", 1, "180"), position(2), vector("axis", 5, "0, 0, 1")),

    WING(Group.WINGS, "AddPartWing", only(WorkshopKind.PLANE), lengths(7),
            position(0), vector("axis", 3, "0, 1, 0"), number("max_angle", 6, "90")),
    PYLON(Group.WINGS, "AddPartPylon", only(WorkshopKind.PLANE), WING, lengths(7),
            position(0), vector("axis", 3, "0, 1, 0"), number("max_angle", 6, "90")),
    /** The format has no flaps of its own. A flap is a part that turns with the throttle. */
    FLAP(Group.WINGS, "AddPartThrottle", all(), lengths(7, 10),
            position(0), vector("axis", 3, "1, 0, 0"), number("angle", 6, "-30"), vector("slide", 7, "0, 0, 0")),
    NOZZLE(Group.WINGS, "AddPartNozzle", only(WorkshopKind.PLANE), lengths(6), position(0), vector("axis", 3, "1, 0, 0")),

    WHEEL(Group.WHEELS, "AddPartWheel", all(), lengths(3, 4, 7, 10),
            position(0), number("steer", 3, "0"), vector("axis", 4, "0, 1, 0"), position("pivot", 7)),
    STEERING_WHEEL(Group.WHEELS, "AddPartSteeringWheel", all(), lengths(7),
            position(0), vector("axis", 3, "0, 0, 1"), number("angle", 6, "90")),
    /** One line naming every probe, so its first position stands for all of them. */
    GROUND_WHEELS(Group.WHEELS, "SetWheelPos", only(WorkshopKind.TANK), null, true, null,
            new PartField("position", PartField.Kind.POSITION, 0, "1.5, -0.24, 2"), rest("more_z", 3, "-2")),

    TRACK_ROLLER(Group.TRACKS, "AddTrackRoller", only(WorkshopKind.TANK), lengths(3), position(0)),
    CRAWLER_TRACK(Group.TRACKS, "AddCrawlerTrack", only(WorkshopKind.TANK), null,
            bool("reverse", 0, "false"), number("link_spacing", 1, "0.5"), number("x", 2, "1.2"),
            rest("outline", 3, "0.2/2, 1/2, 1/-2, 0.2/-2")),

    LANDING_GEAR(Group.LANDING_GEAR, "AddPartLG", all(), lengths(6, 7, 10, 11),
            position(0), vector("axis", 3, "1, 0, 0"), number("max_angle", 6, "90"),
            vector("second_axis", 7, "0, 0, 0"), number("second_angle", 10, "90")),
    LANDING_GEAR_REVERSED(Group.LANDING_GEAR, "AddPartLGRev", all(), lengths(6, 7, 10, 11),
            position(0), vector("axis", 3, "1, 0, 0"), number("max_angle", 6, "90"),
            vector("second_axis", 7, "0, 0, 0"), number("second_angle", 10, "90")),
    LANDING_GEAR_HATCH(Group.LANDING_GEAR, "AddPartLGHatch", all(), lengths(6, 7, 10, 11),
            position(0), vector("axis", 3, "1, 0, 0"), number("max_angle", 6, "90"),
            vector("second_axis", 7, "0, 0, 0"), number("second_angle", 10, "90")),
    LANDING_GEAR_SLIDING(Group.LANDING_GEAR, "AddPartSlideRotLG", all(), lengths(9, 10),
            vector("slide", 0, "0, 0, 0"), position(3), vector("axis", 6, "1, 0, 0"), number("max_angle", 9, "90")),

    CANOPY(Group.CANOPIES, "AddPartCanopy", all(), lengths(6, 7),
            position(0), vector("axis", 3, "1, 0, 0"), number("max_angle", 6, "90")),
    /** Sliding parts have no pivot: their three numbers are how far they travel. */
    CANOPY_SLIDING(Group.CANOPIES, "AddPartSlideCanopy", all(), lengths(3), vector("slide", 0, "0, 0, -1")),
    HATCH(Group.CANOPIES, "AddPartHatch", all(), lengths(6, 7),
            position(0), vector("axis", 3, "1, 0, 0"), number("max_angle", 6, "90")),
    HATCH_SLIDING(Group.CANOPIES, "AddPartSlideHatch", all(), lengths(3), vector("slide", 0, "0, 0, -1")),

    WEAPON(Group.WEAPONS, "AddWeapon", all(), lengths(4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
            weapon(0), position(1), number("yaw", 4, "0"), number("pitch", 5, "0"),
            bool("pilot", 6, "true"), whole("seat", 7, "1"), number("default_yaw", 8, "0"),
            number("min_yaw", 9, "0"), number("max_yaw", 10, "0"), number("min_pitch", 11, "0"), number("max_pitch", 12, "0")),
    TURRET_WEAPON(Group.WEAPONS, "AddTurretWeapon", all(), lengths(4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
            weapon(0), position(1), number("yaw", 4, "0"), number("pitch", 5, "0"),
            bool("pilot", 6, "true"), whole("seat", 7, "1"), number("default_yaw", 8, "0"),
            number("min_yaw", 9, "0"), number("max_yaw", 10, "0"), number("min_pitch", 11, "0"), number("max_pitch", 12, "0")),
    TURRET(Group.WEAPONS, "TurretPosition", all(), null, true, lengths(3), position(0)),

    HIT_BOX(Group.HIT_BOXES, "BoundingBox", all(), lengths(5, 6),
            position(0), number("width", 3, "2"), number("height", 4, "2"), number("damage_factor", 5, "1")),

    ENGINE(Group.ENGINE, "EnginePosition", all(), null, true, lengths(3), position(0)),
    FUEL_TANK(Group.ENGINE, "FuelTankPosition", all(), null, true, lengths(3), position(0));

    /** The headings of the parts list. */
    public enum Group {
        SEATS, CAMERAS, ROTORS, WINGS, WHEELS, TRACKS, LANDING_GEAR, CANOPIES, WEAPONS, HIT_BOXES, ENGINE
    }

    private static final Map<String, PartType> BY_KEY = new HashMap<>();

    static {
        for (PartType type : values()) {
            BY_KEY.put(DefinitionValues.lower(type.key), type);
        }
    }

    private final Group group;
    private final String key;
    private final Set<WorkshopKind> kinds;
    private final @Nullable PartType parent;
    private final boolean single;
    private final int @Nullable [] lengths;
    private final List<PartField> fields;

    PartType(Group group, String key, Set<WorkshopKind> kinds, int @Nullable [] lengths, PartField... fields) {
        this(group, key, kinds, null, false, lengths, fields);
    }

    PartType(Group group, String key, Set<WorkshopKind> kinds, @Nullable PartType parent, int @Nullable [] lengths, PartField... fields) {
        this(group, key, kinds, parent, false, lengths, fields);
    }

    PartType(Group group, String key, Set<WorkshopKind> kinds, @Nullable PartType parent, boolean single, int @Nullable [] lengths, PartField... fields) {
        this.group = group;
        this.key = key;
        this.kinds = kinds;
        this.parent = parent;
        this.single = single;
        this.lengths = lengths;
        this.fields = List.of(fields);
    }

    private static Set<WorkshopKind> all() {
        return EnumSet.allOf(WorkshopKind.class);
    }

    private static Set<WorkshopKind> only(WorkshopKind kind) {
        return EnumSet.of(kind);
    }

    private static int[] lengths(int... lengths) {
        return lengths;
    }

    public Group group() {
        return group;
    }

    public String key() {
        return key;
    }

    /** Names the part in the language files, {@code gui.mcheli.workshop.part.<id>}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean appliesTo(WorkshopKind kind) {
        return kinds.contains(kind);
    }

    /** The part a line of this type belongs to, which it has to follow in the file. */
    public @Nullable PartType parent() {
        return parent;
    }

    /** Whether a definition has at most one such line. */
    public boolean isSingle() {
        return single;
    }

    public List<PartField> fields() {
        return fields;
    }

    /** The field a gizmo moves first and the list goes to, or {@code null} for a part with no spot of its own. */
    public @Nullable PartField mainPosition() {
        for (PartField field : fields) {
            if (field.kind() == PartField.Kind.POSITION) {
                return field;
            }
        }
        return null;
    }

    /** The values of a new line, with only the ones its parser requires. */
    public String defaultValue() {
        StringJoiner out = new StringJoiner(", ");
        for (PartField field : fields) {
            if (lengths == null || field.index() < lengths[0]) {
                out.add(field.fallback());
            }
        }
        return out.toString();
    }

    /**
     * The shortest value count that keeps every value differing from its fallback, never shorter
     * than {@code current}, since a parser can read meaning into a line's length alone. A parser
     * only takes certain lengths: those are in {@link #lengths}.
     */
    int lengthFor(String[] tokens, int current) {
        if (lengths == null) {
            return tokens.length;
        }
        int needed = current;
        for (PartField field : fields) {
            for (int i = 0; i < field.kind().width(); i++) {
                int at = field.index() + i;
                if (at < tokens.length && tokens[at] != null && at >= needed && !tokens[at].equals(fallback(field, i))) {
                    needed = at + 1;
                }
            }
        }
        for (int length : lengths) {
            if (length >= needed) {
                return length;
            }
        }
        return lengths[lengths.length - 1];
    }

    /** One value of a field's fallback. */
    static String fallback(PartField field, int component) {
        String[] values = DefinitionValues.split(field.fallback());
        return component < values.length ? values[component] : "";
    }

    /** The type of a key in a definition file, or {@code null} if the workshop does not place it. */
    public static @Nullable PartType byKey(String key) {
        return BY_KEY.get(DefinitionValues.lower(key));
    }
}
