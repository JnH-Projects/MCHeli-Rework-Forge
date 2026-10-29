package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.hud.HudRenderer;
import com.jnhprojects.mcheli.client.hud.HudScripts;
import com.jnhprojects.mcheli.client.particle.GlowParticle;
import com.jnhprojects.mcheli.client.render.A10Renderer;
import com.jnhprojects.mcheli.client.render.AircraftRenderer;
import com.jnhprojects.mcheli.client.render.FlareRenderer;
import com.jnhprojects.mcheli.client.render.MeshCache;
import com.jnhprojects.mcheli.client.render.ProjectileRenderer;
import com.jnhprojects.mcheli.client.render.RiderPose;
import com.jnhprojects.mcheli.client.sound.EngineSoundPlayer;
import com.jnhprojects.mcheli.entity.ClientAccess;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;

/** Client only wiring. Kept out of {@code MCHeli} so dedicated servers never load these classes. */
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void init(BusGroup modBus) {
        ClientAccess.install(() -> Minecraft.getInstance().player, ClientPacks.INSTANCE);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            event.registerEntityRenderer(ModEntities.HELICOPTER.get(), AircraftRenderer::new);
            event.registerEntityRenderer(ModEntities.PLANE.get(), AircraftRenderer::new);
            event.registerEntityRenderer(ModEntities.TANK.get(), AircraftRenderer::new);
            event.registerEntityRenderer(ModEntities.VEHICLE.get(), AircraftRenderer::new);
            event.registerEntityRenderer(ModEntities.BULLET.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.ROCKET.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.BOMB.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.AA_MISSILE.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.AS_MISSILE.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.AT_MISSILE.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.TV_MISSILE.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.TORPEDO.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.MARKER_ROCKET.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.DISPENSED_ITEM.get(), ProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.FLARE.get(), FlareRenderer::new);
            event.registerEntityRenderer(ModEntities.A10.get(), A10Renderer::new);
        });
        RegisterParticleProvidersEvent.BUS.addListener(event -> event.registerSpriteSet(ModParticles.GLOW.get(), GlowParticle.Provider::new));
        RegisterKeyMappingsEvent.BUS.addListener(event -> KeyBindings.ALL.forEach(event::register));
        RegisterClientReloadListenersEvent.BUS.addListener(event -> {
            event.registerReloadListener(MeshCache.INSTANCE);
            event.registerReloadListener(HudScripts.INSTANCE);
        });
        // Without the original zip there are no vehicles, so say so where the player will see it.
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event ->
                MCHeli.legacyContent().problem().ifPresent(event.getPlayer()::sendSystemMessage));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> ClientPacks.INSTANCE.loggedOut());
        CrewController.init();
        WeaponController.init();
        VehiclePicker.init();
        WeaponHud.init();
        HudRenderer.init();
        EngineSoundPlayer.init();
        RiderPose.init();
    }
}
