package com.jnhprojects.mcheli.fabric.mixin;

import com.jnhprojects.mcheli.client.ClientHooks;
import com.jnhprojects.mcheli.fabric.RollableCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The camera hook, where Forge fires its camera angle event on 1.20.6 (the camera is placed for
 * the frame, and its rotation is about to become the view matrix), and the first person hand,
 * which is hidden in some seats.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Shadow
    @Final
    private Camera mainCamera;

    @Unique
    private float mcheli$viewRoll;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;resetProjectionMatrix(Lorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    private void mcheli$beforeViewRotation(CallbackInfo ci) {
        mcheli$viewRoll = 0.0F;
        ClientHooks.computeCameraAngles(mainCamera, mainCamera.getPartialTickTime(), (camera, yaw, pitch, roll) -> {
            setRotation(camera, yaw, pitch, roll);
            mcheli$viewRoll = roll;
        });
    }

    /**
     * Vanilla's view matrix has no roll; it stacks on top of the other two, as NeoForge does it. This
     * is the inverse of the camera's rotation, hence the negated roll.
     */
    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotationXYZ(FFF)Lorg/joml/Matrix4f;"))
    private Matrix4f mcheli$withRoll(Matrix4f matrix, float x, float y, float z) {
        return new Matrix4f().rotationZ(-mcheli$viewRoll * Mth.DEG_TO_RAD).mul(matrix.rotationXYZ(x, y, z));
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
