package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.InvalidDefinitionException;
import com.jnhprojects.mcheli.content.aircraft.part.MovingPart;
import com.jnhprojects.mcheli.content.aircraft.part.PropellerPart;
import com.jnhprojects.mcheli.content.aircraft.part.WingPart;

import java.util.ArrayList;
import java.util.List;

import static com.jnhprojects.mcheli.content.DefinitionValues.split;
import static com.jnhprojects.mcheli.content.DefinitionValues.toBool;
import static com.jnhprojects.mcheli.content.DefinitionValues.toFloat;
import static com.jnhprojects.mcheli.content.DefinitionValues.toInt;
import static com.jnhprojects.mcheli.content.DefinitionValues.toVec;

/** Ported from {@code MCP_PlaneInfo}. */
public final class PlaneDefinition extends AircraftDefinition {
    public static final String FOLDER = "planes";

    public final List<MovingPart> nozzles = new ArrayList<>();
    public final List<PropellerPart> propellers = new ArrayList<>();
    public final List<WingPart> wings = new ArrayList<>();
    public boolean vtol;
    public boolean defaultVtol;
    public float vtolYaw = 0.3F;
    public float vtolPitch = 0.2F;
    public boolean variableSweepWing;
    /**
     * Top speed with the wings fully swept. The original initialised it from {@code speed} before
     * any line was read, so a file that never sets it gets 0.1, not its own speed.
     */
    public float sweepWingSpeed = speed;

    public PlaneDefinition(String name) {
        super(name);
    }

    @Override
    public String folder() {
        return FOLDER;
    }

    @Override
    public String kind() {
        return "plane";
    }

    @Override
    protected float maxSpeed() {
        return 1.8F;
    }

    @Override
    protected float defaultRotorSpeed() {
        return 47.94F;
    }

    @Override
    protected int defaultMaxZoom() {
        return 8;
    }

    @Override
    public String defaultSound() {
        return "plane";
    }

    @Override
    protected String defaultHud(int seatIndex) {
        return seatIndex <= 1 ? "plane" : "gunner";
    }

    /** VTOL capable planes, and planes with nozzles or tilting rotors, get a nozzle part state. */
    public boolean hasNozzleState() {
        return !nozzles.isEmpty() || !propellers.isEmpty() || vtol;
    }

    @Override
    protected void registerKeys(KeyTable keys) {
        super.registerKeys(keys);
        keys.put("AddPartRotor", v -> {
            String[] s = split(v);
            if (s.length >= 6) {
                float factor = s.length >= 7 ? toFloat(s[6], -180.0F, 180.0F) / 90.0F : 1.0F;
                propellers.add(new PropellerPart("rotor" + propellers.size(), toVec(s[0], s[1], s[2]), toVec(s[3], s[4], s[5]),
                        factor, List.of()));
            }
        });
        keys.put("AddBlade", v -> {
            String[] s = split(v);
            if (s.length == 8 && !propellers.isEmpty()) {
                // Every blade of a rotor shares one part name, numbered after the rotor.
                int rotor = propellers.size() - 1;
                propellers.getLast().blades().add(new PropellerPart.Blade("blade" + rotor, toInt(s[0]), toInt(s[1]),
                        toVec(s[2], s[3], s[4]), toVec(s[5], s[6], s[7])));
            }
        });
        keys.put("AddPartWing", v -> {
            String[] s = split(v);
            if (s.length == 7) {
                wings.add(new WingPart("wing" + wings.size(), toVec(s[0], s[1], s[2]), toVec(s[3], s[4], s[5]),
                        toFloat(s[6]) / 90.0F, List.of()));
            }
        });
        keys.put("AddPartPylon", v -> {
            String[] s = split(v);
            if (s.length >= 7 && !wings.isEmpty()) {
                WingPart wing = wings.getLast();
                wing.pylons().add(new MovingPart(wing.part() + "_pylon" + wing.pylons().size(), toVec(s[0], s[1], s[2]),
                        toVec(s[3], s[4], s[5]), toFloat(s[6]) / 90.0F, false));
            }
        });
        keys.put("AddPartNozzle", v -> {
            String[] s = split(v);
            if (s.length == 6) {
                nozzles.add(new MovingPart("nozzle" + nozzles.size(), toVec(s[0], s[1], s[2]), toVec(s[3], s[4], s[5]), 1.0F, false));
            }
        });
        keys.put("VariableSweepWing", v -> variableSweepWing = toBool(v));
        keys.put("SweepWingSpeed", v -> sweepWingSpeed = toFloat(v, 0.0F, 5.0F));
        keys.put("EnableVtol", v -> vtol = toBool(v));
        keys.put("DefaultVtol", v -> defaultVtol = toBool(v));
        keys.put("VtolYaw", v -> vtolYaw = toFloat(v, 0.0F, 1.0F));
        keys.put("VtolPitch", v -> vtolPitch = toFloat(v, 0.01F, 1.0F));
    }

    @Override
    protected void validate() throws InvalidDefinitionException {
        // Hatches and wings share the fold key and the fold state, so the original refused both.
        if (!hatches.isEmpty() && !wings.isEmpty()) {
            hatches.clear();
            wings.clear();
        }
        super.validate();
    }
}
