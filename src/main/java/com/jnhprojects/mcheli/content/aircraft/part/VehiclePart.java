package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code AddPart} and {@code AddChildPart} on ground vehicles.
 *
 * <p>{@code AddPart = drawFirstPerson, yaw, pitch, type, x, y, z[, recoil]}. Each part turns with
 * the gunner's view around its position. Children inherit the parent's transform.
 *
 * @param type 0 plain, 1 rotating barrel, 2 recoiling barrel, 3 missile hidden while reloading
 */
public record VehiclePart(
        String part,
        Vector3 position,
        boolean drawFirstPerson,
        boolean yaw,
        boolean pitch,
        int type,
        float recoil,
        List<VehiclePart> children) {

    public static final int TYPE_ROTATING_BARREL = 1;
    public static final int TYPE_RECOIL = 2;
    public static final int TYPE_MISSILE = 3;

    public VehiclePart {
        children = new ArrayList<>(children);
    }
}
