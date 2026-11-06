package com.jnhprojects.mcheli.fabric;

/** Implemented on {@code Camera} by {@code CameraMixin}. Outside the mixin package, which Mixin refuses to load classes from. */
public interface RollableCamera {
    /** Like vanilla's {@code setRotation}, with a roll in degrees as Forge applies it. */
    void mcheli$setRotation(float yRot, float xRot, float roll);
}
