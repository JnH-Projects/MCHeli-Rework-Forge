package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code AddPartRotor} on planes, with the {@code AddBlade} lines that follow it.
 *
 * <p>The rotor itself tilts with the VTOL nozzle state ({@code state * factor} degrees around
 * {@code axis}), like the nacelles of a tilt rotor. Each blade group then spins with the engine.
 */
public record PropellerPart(String part, Vector3 position, Vector3 axis, float factor, List<Blade> blades) {

    public PropellerPart {
        blades = new ArrayList<>(blades);
    }

    /**
     * {@code AddBlade = count, step, x, y, z, axisX, axisY, axisZ}. The blade part is drawn
     * {@code count} times, each copy turned a further {@code step} degrees.
     */
    public record Blade(String part, int count, int step, Vector3 position, Vector3 axis) {
    }
}
