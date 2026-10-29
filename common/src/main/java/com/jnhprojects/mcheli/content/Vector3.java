package com.jnhprojects.mcheli.content;

/**
 * Immutable vector used by the content definitions.
 *
 * <p>The definition layer deliberately avoids Minecraft types so it can be loaded and tested
 * without a game environment. Convert to {@code net.minecraft.world.phys.Vec3} at the call site.
 */
public record Vector3(double x, double y, double z) {
    public static final Vector3 ZERO = new Vector3(0, 0, 0);

    public Vector3 add(double dx, double dy, double dz) {
        return new Vector3(x + dx, y + dy, z + dz);
    }
}
