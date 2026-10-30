package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.ClientConfig;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.hud.HudRenderer;
import com.jnhprojects.mcheli.client.hud.HudScripts;
import com.jnhprojects.mcheli.client.render.A10Renderer;
import com.jnhprojects.mcheli.client.render.AircraftRenderer;
import com.jnhprojects.mcheli.client.render.FlareRenderer;
import com.jnhprojects.mcheli.client.render.MeshCache;
import com.jnhprojects.mcheli.client.render.ProjectileRenderer;
import com.jnhprojects.mcheli.client.render.RiderPose;
import com.jnhprojects.mcheli.client.sound.EngineSoundPlayer;
import com.jnhprojects.mcheli.entity.ClientAccess;
import com.jnhprojects.mcheli.platform.Platform;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

/**
 * Client side counterpart of {@code CommonHooks}: what the mod does on client events. The loader
 * module calls these from its own events. Kept out of the shared entry point so dedicated servers
 * never load these classes.
 */
public final class ClientHooks {
    /** The weapon HUD's layer, drawn above the vanilla HUD. */
    public static final ResourceLocation HUD_LAYER = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "weapons");

    private ClientHooks() {
    }

    /** During mod construction, on a client only. */
    public static void init() {
        ClientConfig.load(Platform.get().configDir());
        ClientAccess.install(() -> Minecraft.getInstance().player, ClientPacks.INSTANCE);
    }

    /** How a loader registers an entity renderer. */
    public interface EntityRenderers {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    public static void registerEntityRenderers(EntityRenderers renderers) {
        renderers.register(ModEntities.HELICOPTER.get(), AircraftRenderer::new);
        renderers.register(ModEntities.PLANE.get(), AircraftRenderer::new);
        renderers.register(ModEntities.TANK.get(), AircraftRenderer::new);
        renderers.register(ModEntities.VEHICLE.get(), AircraftRenderer::new);
        renderers.register(ModEntities.BULLET.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.ROCKET.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.BOMB.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.AA_MISSILE.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.AS_MISSILE.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.AT_MISSILE.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.TV_MISSILE.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.TORPEDO.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.MARKER_ROCKET.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.DISPENSED_ITEM.get(), ProjectileRenderer::new);
        renderers.register(ModEntities.FLARE.get(), FlareRenderer::new);
        renderers.register(ModEntities.A10.get(), A10Renderer::new);
    }

    public static List<KeyMapping> keyMappings() {
        return KeyBindings.ALL;
    }

    /** Client resource reload listeners, by the id Fabric wants for each. */
    public static Map<ResourceLocation, PreparableReloadListener> reloadListeners() {
        return Map.of(ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "meshes"), MeshCache.INSTANCE,
                ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "hud_scripts"), HudScripts.INSTANCE);
    }

    public static void clientTickEnd() {
        CrewController.onClientTick();
        WeaponController.onClientTick();
        HudRenderer.tick();
        EngineSoundPlayer.tick();
    }

    /** Tilts the view with the hull and moves the eye into the cockpit or onto a TV missile. */
    public static void computeCameraAngles(CameraAngles angles) {
        CrewController.onCameraAngles(angles);
    }

    /** Sets the camera's rotation, roll included, the way Forge's {@code Camera#setRotation} does. */
    public interface CameraRotation {
        void set(Camera camera, float yaw, float pitch, float roll);
    }

    /**
     * For loaders without Forge's camera angle event: called where Forge fires it, right after
     * vanilla placed the camera for this frame. Starts from the camera's own angles and no roll,
     * and only touches the rotation when something changed it.
     */
    public static void computeCameraAngles(Camera camera, float partialTick, CameraRotation rotation) {
        Angles angles = new Angles(camera, partialTick, camera.getYRot(), camera.getXRot());
        computeCameraAngles(angles);
        if (angles.yaw != camera.getYRot() || angles.pitch != camera.getXRot() || angles.roll != 0.0F) {
            rotation.set(camera, angles.yaw, angles.pitch, angles.roll);
        }
    }

    private static final class Angles implements CameraAngles {
        private final Camera camera;
        private final float partialTick;
        private float yaw;
        private float pitch;
        private float roll;

        Angles(Camera camera, float partialTick, float yaw, float pitch) {
            this.camera = camera;
            this.partialTick = partialTick;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public Camera camera() {
            return camera;
        }

        @Override
        public float partialTick() {
            return partialTick;
        }

        @Override
        public float yaw() {
            return yaw;
        }

        @Override
        public float pitch() {
            return pitch;
        }

        @Override
        public float roll() {
            return roll;
        }

        @Override
        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        @Override
        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        @Override
        public void setRoll(float roll) {
            this.roll = roll;
        }
    }

    /** Whether to skip drawing the first person hand this frame. */
    public static boolean hidesHand() {
        return CrewController.hidesHand();
    }

    /** What happens to a press of the use key. */
    public enum UseKey {
        /** Vanilla handles it. */
        PASS,
        /** Taken, and the hand does not swing: it is a weapon trigger. */
        CONSUME,
        /** Taken, and the hand swings as for a vanilla click: the player boards a vehicle. */
        CONSUME_AND_SWING
    }

    public static UseKey onUseKey(InteractionHand hand) {
        if (WeaponController.onUseItem()) {
            return UseKey.CONSUME;
        }
        return VehiclePicker.onUseItem(hand) ? UseKey.CONSUME_AND_SWING : UseKey.PASS;
    }

    /** The pick block key switches weapons in a seat that has any; true when it was taken. */
    public static boolean onPickBlockKey() {
        return WeaponController.onPickBlock();
    }

    public static void beforeRenderLiving(LivingEntity entity, PoseStack pose, float partialTicks) {
        RiderPose.beforeRender(entity, pose, partialTicks);
    }

    public static void drawHud(GuiGraphics graphics, DeltaTracker delta) {
        WeaponHud.draw(graphics, delta);
    }

    /** Without the original zip there are no vehicles, so say so where the player will see it. */
    public static void loggedIn(LocalPlayer player) {
        MCHeli.legacyContent().problem().ifPresent(player::sendSystemMessage);
    }

    public static void loggedOut() {
        ClientPacks.INSTANCE.loggedOut();
    }
}
