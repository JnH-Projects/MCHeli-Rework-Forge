package com.jnhprojects.mcheli.fabric.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.jnhprojects.mcheli.client.ClientHooks;
import com.jnhprojects.mcheli.fabric.RollableCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The camera hook, where Forge fires its camera angle event on 1.21.1 (the camera is placed for
 * the frame, and its rotation is about to become the view matrix), and the first person hand,
 * which is hidden in some seats.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;rotation()Lorg/joml/Quaternionf;",
            ordinal = 0))
    private void mcheli$beforeViewRotation(CallbackInfo ci) {
        ClientHooks.computeCameraAngles(mainCamera, mainCamera.getPartialTickTime(), GameRendererMixin::setRotation);
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void mcheli$renderItemInHand(CallbackInfo ci) {
        if (ClientHooks.hidesHand()) {
            ci.cancel();
        }
    }

    /** Zoom through a sight, where Forge fires its field of view event. */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void mcheli$getFov(Camera camera, float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue((double) ClientHooks.fov((float) cir.getReturnValueD()));
    }

    private static void setRotation(Camera camera, float yaw, float pitch, float roll) {
        ((RollableCamera) camera).mcheli$setRotation(yaw, pitch, roll);
    }
}
