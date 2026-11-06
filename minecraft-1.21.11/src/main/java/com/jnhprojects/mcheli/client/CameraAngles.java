package com.jnhprojects.mcheli.client;

import net.minecraft.client.Camera;

/**
 * The camera's angles for this frame, after vanilla placed the camera and before it renders, as
 * the loader module hands them to {@link ClientHooks#computeCameraAngles}. Forge and NeoForge
 * back this with their viewport event; Fabric with a mixin at the same spot.
 */
public interface CameraAngles {
    Camera camera();

    float partialTick();

    float yaw();

    float pitch();

    float roll();

    void setYaw(float yaw);

    void setPitch(float pitch);

    void setRoll(float roll);
}
