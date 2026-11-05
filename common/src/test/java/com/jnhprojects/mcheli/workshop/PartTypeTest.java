package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.DefinitionReader;
import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.TankDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The part table repeats what the parsers in {@code AircraftDefinition} read, so it is checked against them. */
class PartTypeTest {
    private static WorkshopKind kindFor(PartType type) {
        for (WorkshopKind kind : WorkshopKind.values()) {
            if (type.appliesTo(kind)) {
                return kind;
            }
        }
        throw new AssertionError(type + " applies to nothing");
    }

    private static AircraftDefinition read(WorkshopKind kind, String lines) throws IOException {
        DefinitionReader.Result<AircraftDefinition> result = DefinitionReader.read("AddSeat = 0, 1, 0\n" + lines, kind.create("test"));
        assertEquals(java.util.List.of(), result.problems());
        assertEquals(Set.of(), result.unhandledKeys());
        assertTrue(result.isValid());
        return result.definition();
    }

    @ParameterizedTest
    @EnumSource(PartType.class)
    void everyKeyIsKnownToItsParser(PartType type) throws IOException {
        StringBuilder lines = new StringBuilder();
        if (type.parent() != null) {
            lines.append(type.parent().key()).append(" = ").append(type.parent().defaultValue()).append('\n');
        }
        lines.append(type.key()).append(" = ").append(type.defaultValue()).append('\n');
        read(kindFor(type), lines.toString());
    }

    @ParameterizedTest
    @EnumSource(PartType.class)
    void everyKeyFindsItsType(PartType type) {
        assertSame(type, PartType.byKey(type.key().toUpperCase(java.util.Locale.ROOT)));
    }

    @ParameterizedTest
    @EnumSource(PartType.class)
    void fieldsDoNotOverlap(PartType type) {
        int next = 0;
        for (PartField field : type.fields()) {
            assertEquals(next, field.index(), type + " " + field.id());
            next = field.index() + field.kind().width();
        }
    }

    @Test
    void aNewSeatLoads() throws IOException {
        AircraftDefinition def = read(WorkshopKind.PLANE, PartType.GUNNER_SEAT.key() + " = " + PartType.GUNNER_SEAT.defaultValue());
        assertEquals(2, def.seats.size());
        assertTrue(def.seats.get(1).gunner());
    }

    @Test
    void newRotorsKeepTheirEightValues() throws IOException {
        AircraftDefinition def = read(WorkshopKind.HELICOPTER, "AddRotor = " + PartType.ROTOR.defaultValue());
        assertEquals(1, ((HelicopterDefinition) def).rotors.size());
    }

    @Test
    void childPartsAttachToTheirParent() throws IOException {
        PlaneDefinition def = (PlaneDefinition) read(WorkshopKind.PLANE,
                "AddPartWing = " + PartType.WING.defaultValue() + "\nAddPartPylon = " + PartType.PYLON.defaultValue());
        assertEquals(1, def.wings.getFirst().pylons().size());
    }

    @Test
    void movingAPositionOnlyTouchesItsValues() {
        WorkshopPart part = new WorkshopPart(PartType.WEAPON, 0, "AddWeapon", "m230, 1, 2, 3, 45, 5");
        String changed = part.with(PartType.WEAPON.mainPosition(), new Vector3(4, 5.5, -6.25));
        assertEquals("m230, 4, 5.5, -6.25, 45, 5", changed);
    }

    @Test
    void settingALaterFieldFillsTheOnesBefore() throws IOException {
        WorkshopPart part = new WorkshopPart(PartType.GUNNER_SEAT, 0, "AddGunnerSeat", "0, 1, 0");
        PartField maxPitch = PartType.GUNNER_SEAT.fields().get(4);
        assertEquals("max_pitch", maxPitch.id());
        String changed = part.with(maxPitch, 0, "50");
        assertEquals("0, 1, 0, 0, 0, 0, false, -30, 50", changed);

        AircraftDefinition def = read(WorkshopKind.TANK, "AddGunnerSeat = " + changed);
        assertEquals(50.0F, def.seats.get(1).maxPitch());
        assertEquals(-30.0F, def.seats.get(1).minPitch());
    }

    @Test
    void anOptionalFieldLeftAtItsFallbackIsNotWritten() {
        WorkshopPart part = new WorkshopPart(PartType.SEAT, 0, "AddSeat", "0, 1, 0");
        PartField rotating = PartType.SEAT.fields().get(1);
        assertEquals("0, 1, 0", part.with(rotating, 0, "false"));
        assertEquals("0, 1, 0, true", part.with(rotating, 0, "true"));
    }

    @Test
    void aLineIsNeverShortenedBelowWhatItHad() {
        // Five values make a camera fixed even at yaw zero, which rewriting must not undo.
        WorkshopPart part = new WorkshopPart(PartType.CAMERA, 0, "CameraPosition", "0, 1, 0, false, 0");
        assertEquals("1, 1, 0, false, 0", part.with(PartType.CAMERA.mainPosition(), new Vector3(1, 1, 0)));
    }

    @Test
    void theRestOfALineStaysTogether() {
        WorkshopPart part = new WorkshopPart(PartType.GROUND_WHEELS, 0, "SetWheelPos", "1.5, -0.24, 2, 0, -2");
        PartField rest = PartType.GROUND_WHEELS.fields().get(1);
        assertEquals("0, -2", part.get(rest, 0));
        assertEquals("1.5, -0.24, 2, -3", part.with(rest, 0, "-3"));
    }

    @Test
    void numbersAreWrittenShort() {
        assertEquals("1.5", WorkshopPart.number(1.5));
        assertEquals("2", WorkshopPart.number(2.0));
        assertEquals("0", WorkshopPart.number(-0.00001));
        assertEquals("0.0625", WorkshopPart.number(0.0625));
        assertEquals("100", WorkshopPart.number(100.0));
    }

    @Test
    void tuningDefaultsComeFromABlankDefinition() {
        TuningKey speed = TuningKey.ALL.stream().filter(k -> k.key().equals("Speed")).findFirst().orElseThrow();
        assertEquals("0.1", speed.defaultFor(WorkshopKind.PLANE));
        assertTrue(speed.accepts("1.8"));
        assertFalse(speed.accepts("9"));
        assertFalse(speed.accepts("fast"));
    }

    @Test
    void everyTuningKeyIsKnownToItsParser() throws IOException {
        for (WorkshopKind kind : WorkshopKind.values()) {
            AircraftDefinition blank = kind.create("test");
            for (TuningKey key : TuningKey.ALL) {
                if (key.applies().test(blank)) {
                    read(kind, key.key() + " = " + key.defaultFor(kind));
                }
            }
        }
    }

    @Test
    void tuningOnlyShowsKeysOfTheKind() {
        TuningKey weight = TuningKey.ALL.stream().filter(k -> k.key().equals("WeightType")).findFirst().orElseThrow();
        assertTrue(weight.applies().test(new TankDefinition("test")));
        assertFalse(weight.applies().test(new PlaneDefinition("test")));
        assertNotNull(weight.defaultFor(WorkshopKind.TANK));
    }
}
