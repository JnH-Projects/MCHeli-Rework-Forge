package com.jnhprojects.mcheli.content.aircraft;

import com.jnhprojects.mcheli.content.Vector3;

import java.util.List;

/**
 * {@code AddRack = names, x, y, z, entryX, entryY, entryZ[, range, parachuteAltitude, yaw, pitch, rotatingSeat]}.
 *
 * <p>A rack is a seat for cargo instead of crew: another vehicle (or, later, a container) rides
 * there. Cargo is picked up when it is within {@code range} of the entry point and dropped at the
 * entry point again.
 *
 * @param accepts vehicle names or kind names ({@code plane}, {@code tank}, ...) this rack carries
 * @param entry   load and unload point; the original lifts it 1.5 blocks above the given value
 * @param parachuteAltitude cargo dropped from higher than this deploys a parachute
 */
public record RackDefinition(
        List<String> accepts,
        Vector3 position,
        Vector3 entry,
        float range,
        float parachuteAltitude,
        float yaw,
        float pitch,
        boolean rotatingSeat) {

    public boolean accepts(String name, String kind) {
        for (String s : accepts) {
            if (s.equalsIgnoreCase(name) || s.equalsIgnoreCase(kind)) {
                return true;
            }
        }
        return false;
    }
}
