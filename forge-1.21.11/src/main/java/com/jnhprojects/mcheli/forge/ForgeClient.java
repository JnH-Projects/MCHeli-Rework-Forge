package com.jnhprojects.mcheli.forge;

import com.jnhprojects.mcheli.client.CameraAngles;
import com.jnhprojects.mcheli.client.ClientHooks;
import com.jnhprojects.mcheli.client.particle.GlowParticle;
import com.jnhprojects.mcheli.client.particle.SmokeParticle;
import com.jnhprojects.mcheli.registry.ModParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client events on Forge. Only loaded on a physical client. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void init(BusGroup modBus) {
        ClientHooks.init(effect -> Minecraft.getInstance().gameRenderer.setPostEffect(effect));
        FMLClientSetupEvent.getBus(modBus).addListener(event -> event.enqueueWork(() -> ClientHooks.registerMenuScreens(MenuScreens::register)));
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> ClientHooks.registerEntityRenderers(event::registerEntityRenderer));
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> ClientHooks.registerBlockEntityRenderers(event::registerBlockEntityRenderer));
        RegisterParticleProvidersEvent.BUS.addListener(event -> {
            event.registerSpriteSet(ModParticles.GLOW.get(), GlowParticle.Provider::new);
            event.registerSpriteSet(ModParticles.SMOKE.get(), SmokeParticle.Provider::new);
        });
        RegisterKeyMappingsEvent.BUS.addListener(event -> ClientHooks.keyMappings().forEach(event::register));
        RegisterClientReloadListenersEvent.BUS.addListener(event -> ClientHooks.reloadListeners().values().forEach(event::registerReloadListener));
        AddGuiOverlayLayersEvent.BUS.addListener(event -> event.getLayeredDraw()
                .add(ForgeLayeredDraw.POST_SLEEP_STACK, ClientHooks.HUD_LAYER, ClientHooks::drawHud));

        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> ClientHooks.clientTickEnd());
        ViewportEvent.ComputeCameraAngles.BUS.addListener(event -> ClientHooks.computeCameraAngles(new Angles(event)));
        ViewportEvent.ComputeFov.BUS.addListener(event -> event.setFOV(ClientHooks.fov(event.getFOV())));
        RenderHandEvent.BUS.addListener(ForgeClient::hideHand);
        RenderLivingEvent.Pre.BUS.addListener(event -> {
            ClientHooks.beforeRenderLiving(event.getState(), event.getPoseStack(), ClientHooks.cameraPartialTicks());
            return false;
        });
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener(ForgeClient::onInteractionKey);
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> ClientHooks.loggedIn(event.getPlayer()));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> ClientHooks.loggedOut());
    }

    private static boolean hideHand(RenderHandEvent event) {
        return ClientHooks.hidesHand();
    }

    private static boolean onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem()) {
            return switch (ClientHooks.onUseKey(event.getHand())) {
                case PASS -> false;
                case CONSUME -> {
                    event.setSwingHand(false);
                    yield true;
                }
                case CONSUME_AND_SWING -> true;
            };
        }
        return event.isPickBlock() && ClientHooks.onPickBlockKey();
    }

    private record Angles(ViewportEvent.ComputeCameraAngles event) implements CameraAngles {
        @Override
        public Camera camera() {
            return event.getCamera();
        }

        @Override
        public float partialTick() {
            return event.getPartialTick();
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
