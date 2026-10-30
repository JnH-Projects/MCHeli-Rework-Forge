package com.jnhprojects.mcheli.fabric.mixin;

/** Implemented on {@code Camera} by {@link CameraMixin}. */
public interface RollableCamera {
    /** Like vanilla's {@code setRotation}, with a roll in degrees as Forge applies it. */
    void mcheli$setRotation(float yRot, float xRot, float roll);
}
