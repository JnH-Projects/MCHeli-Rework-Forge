package com.jnhprojects.mcheli.fabric;

import com.jnhprojects.mcheli.CommonHooks;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.platform.Platform;
import com.jnhprojects.mcheli.platform.Registration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;

/**
 * The Fabric entry point: everything here only connects Fabric to the shared code. The imported
 * content pack is added by {@code PackRepositoryMixin}, since Fabric has no pack finder event.
 */
public final class McheliFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Platform.install(new FabricPlatform());
        MCHeli.init();

        for (ResourceKey<? extends Registry<?>> registry : Registration.registries()) {
            register(Registration.typed(registry));
        }
        CustomIngredientSerializer.register(FabricVehicleIngredient.SERIALIZER);
        registerPackets();

        ServerLifecycleEvents.SERVER_STARTING.register(CommonHooks::serverAboutToStart);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CommonHooks.serverStopped());
        ServerTickEvents.END_SERVER_TICK.register(CommonHooks::serverTickEnd);
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> CommonHooks.registerCommands(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CommonHooks.playerLoggedIn(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CommonHooks.playerLoggedOut(handler.getPlayer()));
    }

    @SuppressWarnings("unchecked")
    private static <T> void register(ResourceKey<Registry<T>> key) {
        Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(key.identifier());
        if (registry == null) {
            throw new IllegalStateException("No registry " + key.identifier());
        }
        Registration.registerAll(key, (id, value) -> Registry.register(registry, id, value));
    }

    /** Client handlers are registered by {@link McheliFabricClient}; handlers run on the main thread. */
    private static void registerPackets() {
        for (ModNetwork.Serverbound<?> packet : ModNetwork.SERVERBOUND) {
            serverbound(packet);
        }
        for (ModNetwork.Clientbound<?> packet : ModNetwork.CLIENTBOUND) {
            clientbound(packet);
        }
    }

    private static <T extends CustomPacketPayload> void clientbound(ModNetwork.Clientbound<T> packet) {
        PayloadTypeRegistry.clientboundPlay().register(packet.type(), packet.codec());
    }

    private static <T extends CustomPacketPayload> void serverbound(ModNetwork.Serverbound<T> packet) {
        PayloadTypeRegistry.serverboundPlay().register(packet.type(), packet.codec());
        ServerPlayNetworking.registerGlobalReceiver(packet.type(), (msg, context) -> packet.handler().accept(msg, context.player()));
    }
}
