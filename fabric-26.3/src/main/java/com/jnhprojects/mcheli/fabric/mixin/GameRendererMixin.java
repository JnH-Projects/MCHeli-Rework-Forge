package com.jnhprojects.mcheli.fabric.mixin;

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
 * The camera hook, at the two places Forge fires its camera angle event (once vanilla has placed
 * the camera for the frame, and again before the camera is copied into the render state), and
 * the first person hand, which is hidden in some seats.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;update(Lnet/minecraft/client/DeltaTracker;)V",
            shift = At.Shift.AFTER))
    private void mcheli$afterCameraUpdate(DeltaTracker deltaTracker, CallbackInfo ci) {
        ClientHooks.computeCameraAngles(mainCamera, deltaTracker.getGameTimeDeltaPartialTick(true), GameRendererMixin::setRotation);
    }

    @Inject(method = "extractCamera", at = @At("HEAD"))
    private void mcheli$beforeExtractCamera(DeltaTracker deltaTracker, float worldPartialTicks, CallbackInfo ci) {
        ClientHooks.computeCameraAngles(mainCamera, worldPartialTicks, GameRendererMixin::setRotation);
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void mcheli$renderItemInHand(CallbackInfo ci) {
        if (ClientHooks.hidesHand()) {
            ci.cancel();
        }
    }

    private static void setRotation(Camera camera, float yaw, float pitch, float roll) {
        ((RollableCamera) camera).mcheli$setRotation(yaw, pitch, roll);
    }
}
