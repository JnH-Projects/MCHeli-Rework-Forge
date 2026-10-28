package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

import java.util.List;

import static com.jnhprojects.mcheli.content.DefinitionValues.lower;
import static com.jnhprojects.mcheli.content.DefinitionValues.toFloat;

/** Ported from {@code MCH_TankInfo}. Covers tracked tanks as well as wheeled cars. */
public final class TankDefinition extends AircraftDefinition {
    public static final String FOLDER = "tanks";

    public enum WeightType {
        /** Neither car nor tank; collides like a car but does not crush other tanks. */
        UNKNOWN,
        CAR,
        TANK
    }

    public WeightType weightType = WeightType.UNKNOWN;
    /** Z offset of the centre of gravity used by the terrain follower. */
    public float weightedCenterZ;

    public TankDefinition(String name) {
        super(name);
    }

    @Override
    public String folder() {
        return FOLDER;
    }

    @Override
    public String kind() {
        return "tank";
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
    protected float defaultSoundRange() {
        return 50.0F;
    }

    @Override
    protected int defaultMaxZoom() {
        return 8;
    }

    @Override
    public String defaultSound() {
        return "prop";
    }

    @Override
    protected String defaultHud(int seatIndex) {
        return seatIndex <= 1 ? "tank" : "gunner";
    }

    @Override
    protected List<Vector3> defaultGroundWheels() {
        return List.of(new Vector3(1.5, -0.24, 2.0), new Vector3(1.5, -0.24, -2.0));
    }

    @Override
    protected void registerKeys(KeyTable keys) {
        super.registerKeys(keys);
        keys.put("WeightType", v -> weightType = switch (lower(v)) {
            case "tank" -> WeightType.TANK;
            case "car" -> WeightType.CAR;
            default -> WeightType.UNKNOWN;
        });
        keys.put("WeightedCenterZ", v -> weightedCenterZ = toFloat(v, -1000.0F, 1000.0F));
    }
}
