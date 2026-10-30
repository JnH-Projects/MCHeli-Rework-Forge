package com.jnhprojects.mcheli.fabric.mixin;

import com.jnhprojects.mcheli.client.ClientHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tilts riders with their vehicle, where Forge fires its living entity render event. */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    private void mcheli$beforeSubmit(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera,
                                     CallbackInfo ci) {
        ClientHooks.beforeRenderLiving(state, pose, camera.cameraEntityPartialTicks);
    }
}
