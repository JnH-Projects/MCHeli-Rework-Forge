package com.jnhprojects.mcheli.neoforge.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla keeps {@code GameRenderer#loadEffect} private on 1.20.6. */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
    @Invoker("loadEffect")
    void mcheli$loadEffect(ResourceLocation effect);
}
