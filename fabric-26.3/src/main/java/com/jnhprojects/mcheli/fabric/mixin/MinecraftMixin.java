package com.jnhprojects.mcheli.fabric.mixin;

import com.jnhprojects.mcheli.client.ClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The use and pick block keys, which Forge and NeoForge let mods take through an input event. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Shadow
    public @Nullable LocalPlayer player;
    @Shadow
    public @Nullable HitResult hitResult;
    @Shadow
    private int rightClickDelay;

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void mcheli$startUseItem(CallbackInfo ci) {
        if (player == null) {
            return;
        }
        ClientHooks.UseKey result = ClientHooks.onUseKey(InteractionHand.MAIN_HAND);
        if (result != ClientHooks.UseKey.PASS) {
            // Vanilla sets the same delay before it looks at the hands.
            rightClickDelay = 4;
            if (result == ClientHooks.UseKey.CONSUME_AND_SWING) {
                player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getInteractAnimation(), false);
            }
            ci.cancel();
        }
    }

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void mcheli$pickBlockOrEntity(CallbackInfo ci) {
        if (hitResult != null && hitResult.getType() != HitResult.Type.MISS && ClientHooks.onPickBlockKey()) {
            ci.cancel();
        }
    }
}
