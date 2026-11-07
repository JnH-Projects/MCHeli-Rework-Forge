package com.jnhprojects.mcheli.forge;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.Minecraft;
import com.jnhprojects.mcheli.client.particle.SmokeParticle;
import com.jnhprojects.mcheli.client.CameraAngles;
import com.jnhprojects.mcheli.client.ClientHooks;
import com.jnhprojects.mcheli.client.particle.GlowParticle;
import com.jnhprojects.mcheli.physics.AircraftRotation;
import com.jnhprojects.mcheli.registry.ModParticles;
import net.minecraft.client.Camera;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client events on Forge. Only loaded on a physical client. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void init(IEventBus modBus) {
        ClientHooks.init(effect -> Minecraft.getInstance().gameRenderer.loadEffect(effect));
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> ClientHooks.registerMenuScreens(MenuScreens::register)));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> ClientHooks.registerBlockEntityRenderers(event::registerBlockEntityRenderer));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> ClientHooks.registerEntityRenderers(event::registerEntityRenderer));
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(ModParticles.GLOW.get(), GlowParticle.Provider::new);
            event.registerSpriteSet(ModParticles.SMOKE.get(), SmokeParticle.Provider::new);
        });
        modBus.addListener((RegisterKeyMappingsEvent event) -> ClientHooks.keyMappings().forEach(event::register));
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                ClientHooks.reloadListeners().values().forEach(event::registerReloadListener));
        modBus.addListener((AddGuiOverlayLayersEvent event) -> event.getLayeredDraw()
                .add(ForgeLayeredDraw.POST_SLEEP_STACK, ClientHooks.HUD_LAYER, ClientHooks::drawHud));

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                ClientHooks.clientTickEnd();
            }
        });
        forgeBus.addListener((ViewportEvent.ComputeCameraAngles event) -> {
            ClientHooks.computeCameraAngles(new Angles(event));
            stackRoll(event);
        });
        forgeBus.addListener((ViewportEvent.ComputeFov event) -> event.setFOV(ClientHooks.fov((float) event.getFOV())));
        forgeBus.addListener((RenderHandEvent event) -> event.setCanceled(ClientHooks.hidesHand()));
        forgeBus.addListener(EventPriority.NORMAL, false, RenderLivingEvent.Pre.class,
                event -> ClientHooks.beforeRenderLiving(event.getEntity(), event.getPoseStack(), event.getPartialTick()));
        forgeBus.addListener(ForgeClient::onInteractionKey);
        forgeBus.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> ClientHooks.loggedIn(event.getPlayer()));
        forgeBus.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientHooks.loggedOut());
    }

    /**
     * Forge puts the event's roll straight into {@code rotationXYZ} on 1.20.6, so it turns around the
     * world's Z axis rather than the view's. The camera's look vectors were set before the event and
     * stay as they are, so this only changes the view matrix of this frame.
     */
    private static void stackRoll(ViewportEvent.ComputeCameraAngles event) {
        if (event.getRoll() != 0.0F) {
            float[] angles = AircraftRotation.rollInXyzOrder(event.getYaw(), event.getPitch(), event.getRoll());
            event.setYaw(angles[0]);
            event.setPitch(angles[1]);
            event.setRoll(angles[2]);
        }
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

    private record Angles(ViewportEvent.ComputeCameraAngles event) implements CameraAngles {
        @Override
        public Camera camera() {
            return event.getCamera();
        }

        @Override
        public float partialTick() {
            return (float) event.getPartialTick();
        }

        @Override
        public float yaw() {
            return event.getYaw();
        }

        @Override
        public float pitch() {
            return event.getPitch();
        }

        @Override
        public float roll() {
            return event.getRoll();
        }

        @Override
        public void setYaw(float yaw) {
            event.setYaw(yaw);
        }

        @Override
        public void setPitch(float pitch) {
            event.setPitch(pitch);
        }

        @Override
        public void setRoll(float roll) {
            event.setRoll(roll);
        }
    }
}
