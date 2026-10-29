package com.jnhprojects.mcheli.fabric;

import com.jnhprojects.mcheli.client.ClientHooks;
import com.jnhprojects.mcheli.client.particle.GlowParticle;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.packs.PackType;

/**
 * Client side of the Fabric entry point. The camera, hand, key and rider hooks are mixins (see
 * {@code com.jnhprojects.mcheli.fabric.mixin}), since Fabric API has no events for them.
 */
public final class McheliFabricClient implements ClientModInitializer {

    // EntityRendererRegistry is deprecated in favour of a vanilla method that is not public yet.
    @SuppressWarnings("deprecation")
    @Override
    public void onInitializeClient() {
        ClientHooks.init();
        ClientHooks.registerEntityRenderers(EntityRendererRegistry::register);
        ParticleProviderRegistry.getInstance().register(ModParticles.GLOW.get(), GlowParticle.Provider::new);
        ClientHooks.keyMappings().forEach(KeyMappingHelper::registerKeyMapping);
        ClientHooks.reloadListeners().forEach(ResourceLoader.get(PackType.CLIENT_RESOURCES)::registerReloadListener);
        HudElementRegistry.attachElementAfter(VanillaHudElements.SLEEP, ClientHooks.HUD_LAYER, ClientHooks::drawHud);
        for (ModNetwork.Clientbound<?> packet : ModNetwork.CLIENTBOUND) {
            clientbound(packet);
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientHooks.clientTickEnd());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientHooks.loggedIn(client.player));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientHooks.loggedOut());
    }

    private static <T extends CustomPacketPayload> void clientbound(ModNetwork.Clientbound<T> packet) {
        ClientPlayNetworking.registerGlobalReceiver(packet.type(), (msg, context) -> packet.handler().accept(msg));
    }
}
