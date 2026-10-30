package com.jnhprojects.mcheli.neoforge.mixin;

import com.jnhprojects.mcheli.client.ClientHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The camera hook, where Forge fires its camera angle event on 1.21.1: once the camera is placed
 * for the frame, just before its rotation becomes the view matrix. NeoForge fires its own event
 * before vanilla sets the camera position, which would undo a cockpit eye.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;rotation()Lorg/joml/Quaternionf;",
            ordinal = 0))
    private void mcheli$beforeViewRotation(DeltaTracker deltaTracker, CallbackInfo ci) {
        ClientHooks.computeCameraAngles(mainCamera, mainCamera.getPartialTickTime(), GameRendererMixin::setRotation);
    }

    /** NeoForge turns the camera the other way round its roll axis than Forge does. */
    private static void setRotation(Camera camera, float yaw, float pitch, float roll) {
        ((CameraInvoker) camera).mcheli$setRotation(yaw, pitch, -roll);
    }
}
