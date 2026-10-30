package com.jnhprojects.mcheli.fabric;

import com.jnhprojects.mcheli.client.ClientHooks;
import com.jnhprojects.mcheli.client.particle.GlowParticle;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Client side of the Fabric entry point. The camera, hand, key and rider hooks are mixins (see
 * {@code com.jnhprojects.mcheli.fabric.mixin}), since Fabric API has no events for them.
 */
public final class McheliFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientHooks.init();
        ClientHooks.registerEntityRenderers(EntityRendererRegistry::register);
        ParticleFactoryRegistry.getInstance().register(ModParticles.GLOW.get(), GlowParticle.Provider::new);
        ClientHooks.keyMappings().forEach(KeyBindingHelper::registerKeyBinding);
        ResourceManagerHelper resources = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        ClientHooks.reloadListeners().forEach((id, listener) -> resources.registerReloadListener(new Identified(id, listener)));
        // Drawn after the vanilla HUD, which is where Forge's post sleep layer ends up too.
        HudRenderCallback.EVENT.register(ClientHooks::drawHud);
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

    /** Fabric wants an id on every reload listener. */
    private record Identified(ResourceLocation id, PreparableReloadListener listener) implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return id;
        }

        @Override
        public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, ProfilerFiller preparations,
                                              ProfilerFiller reload, Executor background, Executor main) {
            return listener.reload(barrier, manager, preparations, reload, background, main);
        }
    }
}
