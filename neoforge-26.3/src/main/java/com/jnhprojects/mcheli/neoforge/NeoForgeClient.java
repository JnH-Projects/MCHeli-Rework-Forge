package com.jnhprojects.mcheli.neoforge;

import com.jnhprojects.mcheli.client.ClientHooks;
import com.jnhprojects.mcheli.client.particle.GlowParticle;
import com.jnhprojects.mcheli.registry.ModParticles;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client events on NeoForge. Only loaded on a physical client. The camera hook is a mixin
 * ({@code GameRendererMixin}): NeoForge's camera event fires before vanilla positions the camera,
 * so a cockpit eye set from it would be overwritten.
 */
final class NeoForgeClient {
    private NeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        ClientHooks.init();
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                event -> ClientHooks.registerEntityRenderers(event::registerEntityRenderer));
        modBus.addListener(RegisterParticleProvidersEvent.class,
                event -> event.registerSpriteSet(ModParticles.GLOW.get(), GlowParticle.Provider::new));
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> ClientHooks.keyMappings().forEach(event::register));
        modBus.addListener(AddClientReloadListenersEvent.class, event -> ClientHooks.reloadListeners().forEach(event::addListener));
        modBus.addListener(RegisterGuiLayersEvent.class,
                event -> event.registerAbove(VanillaGuiLayers.SLEEP_OVERLAY, ClientHooks.HUD_LAYER, ClientHooks::drawHud));

        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> ClientHooks.clientTickEnd());
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, event -> event.setCanceled(ClientHooks.hidesHand()));
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Pre.class,
                event -> ClientHooks.beforeRenderLiving(event.getRenderState(), event.getPoseStack(), event.getPartialTick()));
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, NeoForgeClient::onInteractionKey);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> ClientHooks.loggedIn(event.getPlayer()));
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> ClientHooks.loggedOut());
    }

    private static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem()) {
            switch (ClientHooks.onUseKey(event.getHand())) {
                case PASS -> {
                }
                case CONSUME -> {
                    event.setSwingHand(false);
                    event.setCanceled(true);
                }
                case CONSUME_AND_SWING -> event.setCanceled(true);
            }
        } else if (event.isPickBlock() && ClientHooks.onPickBlockKey()) {
            event.setCanceled(true);
        }
    }
}
