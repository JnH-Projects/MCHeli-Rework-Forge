package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.network.CrewActionPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.function.Consumer;

/**
 * The local crew member's sight, {@code MCH_Camera}: zoom for vehicles with a {@code CameraZoom}
 * above one, and night vision and thermal view for those with {@code EnableNightVision}. Both
 * reset whenever the player changes seat or leaves the vehicle.
 */
public final class VehicleCamera {
    /** The original's camera modes; the HUD reads the ordinal as {@code cam_mode}. */
    public enum Mode {
        NORMAL,
        NIGHT_VISION,
        /** Night vision seen through a white hot filter, the original's "pencil" shader mode. */
        THERMAL
    }

    /** The post effect of the thermal view, {@code assets/mcheli/post_effect/thermal.json}. */
    private static final ResourceLocation THERMAL_EFFECT = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "thermal");

    private static float zoom = 1.0F;
    private static Mode mode = Mode.NORMAL;

    private VehicleCamera() {
    }

    public static float zoom() {
        return zoom;
    }

    public static Mode mode() {
        return mode;
    }

    /** Back to the plain view, on a seat change or after leaving the vehicle. */
    static void reset() {
        zoom = 1.0F;
        mode = Mode.NORMAL;
    }

    /**
     * Keeps the thermal filter in the player's post effects while the thermal view is on. The list
     * is the one a server's post effect packet fills, so ours is put back if that replaces it.
     */
    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean active = THERMAL_EFFECT.equals(mc.gameRenderer.currentPostEffect());
        if (mode == Mode.THERMAL && !active) {
            ClientHooks.postEffects().load(THERMAL_EFFECT);
        } else if (mode != Mode.THERMAL && active) {
            mc.gameRenderer.clearPostEffect();
        }
    }

    /**
     * {@code zoomCamera}: each press doubles the zoom up to the vehicle's {@code CameraZoom}, and
     * the press after that goes back to one. The caller checks that the seat looks through a sight.
     */
    static void onZoomKey(LocalPlayer player, AircraftEntity vehicle) {
        AircraftDefinition def = vehicle.definition();
        zoomStep(player, def != null && !vehicle.isDestroyed() ? def.cameraZoom : 1);
    }

    /** One press of the zoom key on a sight that zooms up to {@code max}. */
    static void zoomStep(LocalPlayer player, int max) {
        if (max <= 1) {
            refuse(player);
            return;
        }
        zoom = zoom >= max - 0.01F ? 1.0F : Math.min(zoom * 2.0F, max);
        player.level().playLocalSound(player, ModSounds.get("zoom"), SoundSource.PLAYERS, 0.5F, 1.0F);
    }

    /**
     * {@code switchCameraMode}: steps from the plain view to night vision, to thermal view and
     * back. The server applies night vision as an effect in both of the last two; the thermal
     * filter is drawn here.
     */
    static void onCameraModeKey(LocalPlayer player, AircraftEntity vehicle) {
        AircraftDefinition def = vehicle.definition();
        if (def == null || !def.nightVision || vehicle.isDestroyed()) {
            refuse(player);
            return;
        }
        nextMode(player, on -> ModNetwork.sendToServer(new CrewActionPacket(vehicle.getId(),
                on ? CrewActionPacket.Action.NIGHT_VISION_ON : CrewActionPacket.Action.NIGHT_VISION_OFF)));
    }

    /**
     * Steps to the next camera mode on a sight that has night vision. {@code nightVision} asks
     * the server to switch the effect on or off, only when that changes.
     */
    static void nextMode(LocalPlayer player, Consumer<Boolean> nightVision) {
        Mode next = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
        if (mode == Mode.NORMAL || next == Mode.NORMAL) {
            nightVision.accept(next != Mode.NORMAL);
        }
        mode = next;
        player.level().playLocalSound(player, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3F, 1.0F);
    }

    /** The view's field of view, narrowed by the zoom. */
    public static float fov(float fov) {
        return zoom > 1.0F ? fov / zoom : fov;
    }

    /** {@code playSoundNG}: the key does nothing in this seat. */
    private static void refuse(LocalPlayer player) {
        player.level().playLocalSound(player, ModSounds.get("ng"), SoundSource.PLAYERS, 0.5F, 1.0F);
    }
}
