package com.jnhprojects.mcheli.neoforge.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** NeoForge adds a roll to {@code Camera#setRotation}, but leaves it protected. */
@Mixin(Camera.class)
public interface CameraInvoker {
    @Invoker("setRotation")
    void mcheli$setRotation(float yRot, float xRot, float roll);
}
