package com.jnhprojects.mcheli.fabric.mixin;

import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Vanilla's camera has no roll. This is {@code Camera#setRotation} with the roll Forge patches in,
 * so a banked aircraft tilts the view the same on every loader.
 */
@Mixin(Camera.class)
abstract class CameraMixin implements RollableCamera {
    @Shadow
    @Final
    private static Vector3fc FORWARDS;
    @Shadow
    @Final
    private static Vector3fc UP;
    @Shadow
    @Final
    private static Vector3fc LEFT;

    @Shadow
    private float xRot;
    @Shadow
    private float yRot;
    @Shadow
    @Final
    private Vector3f forwards;
    @Shadow
    @Final
    private Vector3f up;
    @Shadow
    @Final
    private Vector3f left;
    @Shadow
    @Final
    private Quaternionf rotation;
    @Shadow
    private int matrixPropertiesDirty;

    @Override
    public void mcheli$setRotation(float yRot, float xRot, float roll) {
        this.xRot = xRot;
        this.yRot = yRot;
        this.rotation.rotationYXZ((float) Math.PI - yRot * (float) (Math.PI / 180.0), -xRot * (float) (Math.PI / 180.0),
                roll * (float) (Math.PI / 180.0));
        FORWARDS.rotate(this.rotation, this.forwards);
        UP.rotate(this.rotation, this.up);
        LEFT.rotate(this.rotation, this.left);
        this.matrixPropertiesDirty |= 3;
    }
}
