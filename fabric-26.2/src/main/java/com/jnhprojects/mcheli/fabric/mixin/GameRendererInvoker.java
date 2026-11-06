package com.jnhprojects.mcheli.fabric.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla keeps {@code GameRenderer#setPostEffect} private on 26.2. */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
    @Invoker("setPostEffect")
    void mcheli$setPostEffect(Identifier effect);
}
