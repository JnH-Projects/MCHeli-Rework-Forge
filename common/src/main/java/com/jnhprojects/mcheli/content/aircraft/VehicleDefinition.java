package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.aircraft.part.VehiclePart;

import java.util.ArrayList;
import java.util.List;

import static com.jnhprojects.mcheli.content.DefinitionValues.split;
import static com.jnhprojects.mcheli.content.DefinitionValues.toBool;
import static com.jnhprojects.mcheli.content.DefinitionValues.toFloat;
import static com.jnhprojects.mcheli.content.DefinitionValues.toInt;
import static com.jnhprojects.mcheli.content.DefinitionValues.toVec;

/**
 * Ported from {@code MCH_VehicleInfo}: static guns and launchers. The gunner aims with the view
 * and the {@code AddPart} tree turns to follow.
 */
public final class VehicleDefinition extends AircraftDefinition {
    public static final String FOLDER = "vehicles";

    public boolean canMove;
    public boolean canRotate;
    public final List<VehiclePart> parts = new ArrayList<>();

    public VehicleDefinition(String name) {
        super(name);
    }

    @Override
    public String folder() {
        return FOLDER;
    }

    @Override
    public String kind() {
        return "vehicle";
    }

    @Override
    protected float defaultMinRotationPitch() {
        return -90.0F;
    }

    @Override
    protected float defaultMaxRotationPitch() {
        return 90.0F;
    }

    @Override
    protected String defaultHud(int seatIndex) {
        return "vehicle";
    }

    @Override
    protected void registerKeys(KeyTable keys) {
        super.registerKeys(keys);
        keys.put("CanMove", v -> canMove = toBool(v));
        keys.put("CanRotation", v -> canRotate = toBool(v));
        keys.put("RotationPitchMin", v -> accept("minrotationpitch", v));
        keys.put("RotationPitchMax", v -> accept("maxrotationpitch", v));
        keys.put("AddPart", v -> {
            VehiclePart part = parsePart(v, "part" + parts.size());
            if (part != null) {
                parts.add(part);
            }
        });
        keys.put("AddChildPart", v -> {
            if (parts.isEmpty()) {
                return;
            }
            // Children attach to the last top level part, never to another child.
            VehiclePart parent = parts.getLast();
            VehiclePart child = parsePart(v, parent.part() + "_" + parent.children().size());
            if (child != null) {
                parent.children().add(child);
            }
        });
    }

    private static VehiclePart parsePart(String data, String name) {
        String[] s = split(data);
        if (s.length < 7) {
            return null;
        }
        float recoil = s.length >= 8 ? toFloat(s[7]) : 0.0F;
        return new VehiclePart(name, toVec(s[4], s[5], s[6]), toBool(s[0]), toBool(s[1]), toBool(s[2]), toInt(s[3]), recoil, List.of());
    }
}
