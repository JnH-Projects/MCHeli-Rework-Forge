package com.jnhprojects.mcheli.content.aircraft;

import java.util.ArrayList;
import java.util.List;

import static com.jnhprojects.mcheli.content.DefinitionValues.split;
import static com.jnhprojects.mcheli.content.DefinitionValues.toBool;
import static com.jnhprojects.mcheli.content.DefinitionValues.toFloat;
import static com.jnhprojects.mcheli.content.DefinitionValues.toInt;
import static com.jnhprojects.mcheli.content.DefinitionValues.toVec;

/** Ported from {@code MCH_HeliInfo}. */
public final class HelicopterDefinition extends AircraftDefinition {
    public static final String FOLDER = "helicopters";

    public final List<RotorDefinition> rotors = new ArrayList<>();
    public boolean foldableBlades;

    public HelicopterDefinition(String name) {
        super(name);
    }

    @Override
    public String folder() {
        return FOLDER;
    }

    @Override
    public String kind() {
        return "helicopter";
    }

    @Override
    protected float defaultMinRotationPitch() {
        return -20.0F;
    }

    @Override
    protected float defaultMaxRotationPitch() {
        return 20.0F;
    }

    @Override
    protected float defaultRotorSpeed() {
        return 79.99F;
    }

    @Override
    protected float defaultSoundRange() {
        return 80.0F;
    }

    @Override
    protected int defaultMaxZoom() {
        return 8;
    }

    @Override
    public String defaultSound() {
        return "heli";
    }

    @Override
    protected String defaultHud(int seatIndex) {
        return switch (seatIndex) {
            case 0 -> "heli";
            case 1 -> "heli_gnr";
            default -> "gunner";
        };
    }

    @Override
    protected void registerKeys(KeyTable keys) {
        super.registerKeys(keys);
        keys.put("EnableFoldBlade", v -> foldableBlades = toBool(v));
        keys.putAliased("AddRotor", this::parseRotor);
        keys.putAliased("AddRotorOld", this::parseRotor);
    }

    private void parseRotor(String key, String data) {
        String[] s = split(data);
        if (s.length != 8 && s.length != 9) {
            return;
        }
        rotors.add(new RotorDefinition(
                toInt(s[0]),
                toInt(s[1]),
                toVec(s[2], s[3], s[4]),
                toVec(s[5], s[6], s[7]),
                "blade" + rotors.size(),
                s.length == 9 && toBool(s[8]),
                key.equals("addrotorold")));
    }
}
