package com.jnhprojects.mcheli.forge;

import com.jnhprojects.mcheli.CommonHooks;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.item.VehicleIngredient;
import com.jnhprojects.mcheli.platform.Platform;
import com.jnhprojects.mcheli.platform.Registration;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** The Forge entry point: everything here only connects Forge to the shared code. */
@Mod(MCHeli.MOD_ID)
public final class McheliForge {

    public McheliForge(FMLJavaModLoadingContext context) {
        BusGroup modBus = context.getModBusGroup();
        Platform.install(new ForgePlatform());
        MCHeli.init();

        RegisterEvent.getBus(modBus).addListener(event -> {
            for (ResourceKey<? extends Registry<?>> registry : Registration.registries()) {
                register(event, Registration.typed(registry));
            }
            event.register(ForgeRegistries.Keys.INGREDIENT_SERIALIZERS,
                    helper -> helper.register(VehicleIngredient.ID, ForgeVehicleIngredient.SERIALIZER));
        });
        ForgeNetwork.register();

        AddPackFindersEvent.BUS.addListener(event -> event.addRepositorySource(CommonHooks.packSource()));
        ServerAboutToStartEvent.BUS.addListener(event -> CommonHooks.serverAboutToStart(event.getServer()));
        ServerStoppedEvent.BUS.addListener(event -> CommonHooks.serverStopped());
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> CommonHooks.serverTickEnd(event.server()));
        RegisterCommandsEvent.BUS.addListener(event -> CommonHooks.registerCommands(event.getDispatcher()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                CommonHooks.playerLoggedIn(player);
            }
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                CommonHooks.playerLoggedOut(player);
            }
        });

        if (Platform.get().isClient()) {
            ForgeClient.init(modBus);
        }
    }

    /** Only once Forge asks for that registry, with our entries in declaration order. */
    private static <T> void register(RegisterEvent event, ResourceKey<? extends Registry<T>> registry) {
        if (event.getRegistryKey().equals(registry)) {
            event.register(registry, helper -> Registration.registerAll(registry, helper::register));
        }
    }
}
