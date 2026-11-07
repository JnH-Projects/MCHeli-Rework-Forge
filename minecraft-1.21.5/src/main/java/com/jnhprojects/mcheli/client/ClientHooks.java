package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.ClientConfig;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.hud.HudRenderer;
import com.jnhprojects.mcheli.client.hud.HudScripts;
import com.jnhprojects.mcheli.client.render.A10Renderer;
import com.jnhprojects.mcheli.client.render.AircraftRenderer;
import com.jnhprojects.mcheli.client.render.ChainRenderer;
import com.jnhprojects.mcheli.client.render.DraftingTableRenderer;
import com.jnhprojects.mcheli.client.render.EquipmentRenderer;
import com.jnhprojects.mcheli.client.render.FlareRenderer;
import com.jnhprojects.mcheli.client.render.GltdRenderer;
import com.jnhprojects.mcheli.client.render.Glow;
import com.jnhprojects.mcheli.client.render.MeshCache;
import com.jnhprojects.mcheli.client.render.ParachuteRenderer;
import com.jnhprojects.mcheli.client.render.ProjectileRenderer;
import com.jnhprojects.mcheli.client.render.RiderPose;
import com.jnhprojects.mcheli.client.render.ThrowableRenderer;
import com.jnhprojects.mcheli.client.render.UavStationRenderer;
import com.jnhprojects.mcheli.client.screen.DraftingTableScreen;
import com.jnhprojects.mcheli.client.screen.TeamManagerScreen;
import com.jnhprojects.mcheli.client.screen.UavStationScreen;
import com.jnhprojects.mcheli.client.screen.VehicleScreen;
import com.jnhprojects.mcheli.client.sound.EngineSoundPlayer;
import com.jnhprojects.mcheli.client.workshop.WorkshopClient;
import com.jnhprojects.mcheli.entity.ClientAccess;
import com.jnhprojects.mcheli.platform.Platform;
import com.jnhprojects.mcheli.registry.ModBlockEntities;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.registry.ModMenus;
import com.jnhprojects.mcheli.team.Markers;
import com.jnhprojects.mcheli.team.TeamManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

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

    private static boolean shaderProgramsAssigned;

    private ClientHooks() {
    }

    /** Loads a post effect into the game renderer, whose own method is private on 1.21.5. */
    public interface PostEffects {
        void load(ResourceLocation effect);
    }

    private static PostEffects postEffects = effect -> {
    };

    static PostEffects postEffects() {
        return postEffects;
    }

    /** During mod construction, on a client only. */
    public static void init(PostEffects effects) {
        postEffects = effects;
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
        renderers.register(ModEntities.PARACHUTE.get(), ParachuteRenderer::new);
        renderers.register(ModEntities.THROWABLE.get(), ThrowableRenderer::new);
        renderers.register(ModEntities.GLTD.get(), GltdRenderer::new);
        renderers.register(ModEntities.CHAIN.get(), ChainRenderer::new);
        renderers.register(ModEntities.CONTAINER.get(), context -> new EquipmentRenderer<>(context, "container", 0.2F, 0xFFBFBFBF, 1.0F));
        renderers.register(ModEntities.UAV_STATION.get(), UavStationRenderer::new);
        renderers.register(ModEntities.RAPPEL.get(), NoopRenderer::new);
    }

    /** How a loader registers a block entity renderer. */
    public interface BlockEntityRenderers {
        <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider);
    }

    public static void registerBlockEntityRenderers(BlockEntityRenderers renderers) {
        renderers.register(ModBlockEntities.DRAFTING_TABLE.get(), DraftingTableRenderer::new);
    }

    /** How a loader registers the screen of a menu: vanilla's own method is not public on all of them. */
    public interface MenuScreenRegistrar {
        <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type,
                                                                                            MenuScreens.ScreenConstructor<M, U> screen);
    }

    /** On the main thread during client setup, once our menu types are registered. */
    public static void registerMenuScreens(MenuScreenRegistrar screens) {
        screens.register(ModMenus.VEHICLE.get(), VehicleScreen::new);
        screens.register(ModMenus.UAV_STATION.get(), UavStationScreen::new);
        screens.register(ModMenus.DRAFTING_TABLE.get(), DraftingTableScreen::new);
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
        if (!shaderProgramsAssigned) {
            // On the first tick rather than at construction, so Iris has surely loaded by now.
            shaderProgramsAssigned = true;
            Glow.assignShaderPrograms();
        }
        CrewController.onClientTick();
        GltdController.onClientTick();
        RangefinderController.onClientTick();
        LauncherController.onClientTick();
        VehicleCamera.tick();
        WeaponController.onClientTick();
        HudRenderer.tick();
        EngineSoundPlayer.tick();
        TargetMarkers.tick();
        WorkshopClient.tick();
        while (KeyBindings.TEAM_MANAGER.consumeClick()) {
            openTeamManager();
        }
    }

    /** Operators get the team manager; anyone else is told it is theirs to open. */
    private static void openTeamManager() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (TeamManager.mayManage(mc.player)) {
            mc.setScreen(new TeamManagerScreen());
        } else {
            mc.player.displayClientMessage(Component.translatable("message.mcheli.team.operators_only"), true);
        }
    }

    /** Tilts the view with the hull and moves the eye into the cockpit or onto a TV missile. */
    public static void computeCameraAngles(CameraAngles angles) {
        CrewController.onCameraAngles(angles);
        GltdController.onCameraAngles(angles);
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

    /** The field of view for this frame, narrowed while the player zooms through a sight. */
    public static float fov(float fov) {
        return LauncherController.fov(RangefinderController.fov(VehicleCamera.fov(fov)));
    }

    /** Whether to skip drawing the first person hand this frame. */
    public static boolean hidesHand() {
        return CrewController.hidesHand() || GltdController.hidesHand() || RangefinderController.isScoped() || LauncherController.isRaised();
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
        if (WeaponController.onUseItem() || GltdController.onUseItem()) {
            return UseKey.CONSUME;
        }
        return VehiclePicker.onUseItem(hand) ? UseKey.CONSUME_AND_SWING : UseKey.PASS;
    }

    /** The pick block key switches weapons in a seat that has any; true when it was taken. */
    public static boolean onPickBlockKey() {
        return WeaponController.onPickBlock();
    }

    /** The partial tick the camera entity is drawn at, for the loaders whose event does not carry it. */
    public static float cameraPartialTicks() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPartialTickTime();
    }

    public static void beforeRenderLiving(LivingEntityRenderState state, PoseStack pose, float partialTicks) {
        RiderPose.beforeRender(state, pose, partialTicks);
    }

    public static void drawHud(GuiGraphics graphics, DeltaTracker delta) {
        WeaponHud.draw(graphics, delta);
        GltdController.drawHud(graphics, delta);
        RangefinderController.drawHud(graphics, delta);
        LauncherController.drawHud(graphics, delta);
        TargetMarkers.draw(graphics, delta);
    }

    /** Without the original zip there are no vehicles, so say so where the player will see it. */
    public static void loggedIn(LocalPlayer player) {
        MCHeli.legacyContent().problem().ifPresent(message -> player.displayClientMessage(message, false));
    }

    public static void loggedOut() {
        ClientPacks.INSTANCE.loggedOut();
        WorkshopClient.loggedOut();
        Markers.clear();
    }
}
