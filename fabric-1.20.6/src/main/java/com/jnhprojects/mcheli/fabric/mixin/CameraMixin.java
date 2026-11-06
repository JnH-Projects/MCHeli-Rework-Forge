package com.jnhprojects.mcheli.fabric.mixin;

import com.jnhprojects.mcheli.fabric.RollableCamera;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Lets the camera hook reach {@code Camera#setRotation}. The view matrix on 1.20.6 is built from the
 * camera's angles alone, so the roll is not for this class: {@code GameRendererMixin} applies it.
 */
@Mixin(Camera.class)
abstract class CameraMixin implements RollableCamera {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Override
    public void mcheli$setRotation(float yRot, float xRot, float roll) {
        setRotation(yRot, xRot);
    }
}
