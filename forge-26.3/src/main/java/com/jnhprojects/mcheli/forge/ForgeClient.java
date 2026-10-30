package com.jnhprojects.mcheli.forge;

import com.jnhprojects.mcheli.client.CameraAngles;
import com.jnhprojects.mcheli.client.ClientHooks;
import com.jnhprojects.mcheli.client.particle.GlowParticle;
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
import net.minecraftforge.event.TickEvent;

/** Client events on Forge. Only loaded on a physical client. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void init() {
        ClientHooks.init();
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> ClientHooks.registerEntityRenderers(event::registerEntityRenderer));
        RegisterParticleProvidersEvent.BUS.addListener(event -> event.registerSpriteSet(ModParticles.GLOW.get(), GlowParticle.Provider::new));
        RegisterKeyMappingsEvent.BUS.addListener(event -> ClientHooks.keyMappings().forEach(event::register));
        RegisterClientReloadListenersEvent.BUS.addListener(event -> ClientHooks.reloadListeners().values().forEach(event::registerReloadListener));
        AddGuiOverlayLayersEvent.BUS.addListener(event -> event.getLayeredDraw()
                .add(ForgeLayeredDraw.POST_SLEEP_STACK, ClientHooks.HUD_LAYER, ClientHooks::drawHud));

        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> ClientHooks.clientTickEnd());
        ViewportEvent.ComputeCameraAngles.BUS.addListener(event -> ClientHooks.computeCameraAngles(new Angles(event)));
        ViewportEvent.ComputeFov.BUS.addListener(event -> event.setFOV(ClientHooks.fov(event.getFOV())));
        RenderHandEvent.BUS.addListener(ForgeClient::hideHand);
        RenderLivingEvent.Pre.BUS.addListener(event -> {
            ClientHooks.beforeRenderLiving(event.getState(), event.getPoseStack(), event.getCameraState().cameraEntityPartialTicks);
            return false;
        });
        InputEvent.InteractionKeyMappingTriggered.UseItem.BUS.addListener(event -> switch (ClientHooks.onUseKey(event.getHand())) {
            case PASS -> false;
            case CONSUME -> {
                event.setSwingHand(false);
                yield true;
            }
            case CONSUME_AND_SWING -> true;
        });
        InputEvent.InteractionKeyMappingTriggered.PickBlock.BUS.addListener(ForgeClient::pickBlock);
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> ClientHooks.loggedIn(event.getPlayer()));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> ClientHooks.loggedOut());
    }

    private static boolean hideHand(RenderHandEvent event) {
        return ClientHooks.hidesHand();
    }

    private static boolean pickBlock(InputEvent.InteractionKeyMappingTriggered.PickBlock event) {
        return ClientHooks.onPickBlockKey();
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
