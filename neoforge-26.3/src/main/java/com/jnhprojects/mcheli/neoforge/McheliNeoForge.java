package com.jnhprojects.mcheli.neoforge;

import com.jnhprojects.mcheli.CommonHooks;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.item.VehicleIngredient;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.platform.Platform;
import com.jnhprojects.mcheli.platform.Registration;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entry point: everything here only connects NeoForge to the shared code. */
@Mod(MCHeli.MOD_ID)
public final class McheliNeoForge {

    public McheliNeoForge(IEventBus modBus) {
        Platform.install(new NeoForgePlatform());
        MCHeli.init();

        modBus.addListener(RegisterEvent.class, event -> {
            for (ResourceKey<? extends Registry<?>> registry : Registration.registries()) {
                register(event, Registration.typed(registry));
            }
            event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES,
                    helper -> helper.register(VehicleIngredient.ID, NeoForgeVehicleIngredient.TYPE));
        });
        modBus.addListener(RegisterPayloadHandlersEvent.class, McheliNeoForge::registerPackets);
        modBus.addListener(AddPackFindersEvent.class, event -> event.addRepositorySource(CommonHooks.packSource()));

        NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, event -> CommonHooks.serverAboutToStart(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> CommonHooks.serverStopped());
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> CommonHooks.serverTickEnd(event.getServer()));
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> CommonHooks.registerCommands(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                CommonHooks.playerLoggedIn(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                CommonHooks.playerLoggedOut(player);
            }
        });

        if (Platform.get().isClient()) {
            NeoForgeClient.init(modBus);
        }
    }

    /** Only once NeoForge asks for that registry, with our entries in declaration order. */
    private static <T> void register(RegisterEvent event, ResourceKey<? extends Registry<T>> registry) {
        if (event.getRegistryKey().equals(registry)) {
            event.register(registry, helper -> Registration.registerAll(registry, helper::register));
        }
    }

    /** Handlers run on the main thread, which is NeoForge's default. */
    private static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Integer.toString(ModNetwork.PROTOCOL_VERSION));
        for (ModNetwork.Serverbound<?> packet : ModNetwork.SERVERBOUND) {
            serverbound(registrar, packet);
        }
        for (ModNetwork.Clientbound<?> packet : ModNetwork.CLIENTBOUND) {
            clientbound(registrar, packet);
        }
    }

    private static <T extends CustomPacketPayload> void serverbound(PayloadRegistrar registrar, ModNetwork.Serverbound<T> packet) {
        registrar.playToServer(packet.type(), packet.codec(),
                (msg, context) -> packet.handler().accept(msg, (ServerPlayer) context.player()));
    }

    private static <T extends CustomPacketPayload> void clientbound(PayloadRegistrar registrar, ModNetwork.Clientbound<T> packet) {
        registrar.playToClient(packet.type(), packet.codec(), (msg, context) -> packet.handler().accept(msg));
    }
}
