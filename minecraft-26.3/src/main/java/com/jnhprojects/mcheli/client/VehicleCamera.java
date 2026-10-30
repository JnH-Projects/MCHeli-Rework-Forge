package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.network.CrewActionPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The local crew member's sight, {@code MCH_Camera}: zoom for vehicles with a {@code CameraZoom}
 * above one, and night vision for those with {@code EnableNightVision}. Both reset whenever the
 * player changes seat or leaves the vehicle.
 */
public final class VehicleCamera {
    /** The original's camera modes; the HUD reads the ordinal as {@code cam_mode}. */
    public enum Mode {
        NORMAL,
        NIGHT_VISION
    }

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
     * {@code zoomCamera}: each press doubles the zoom up to the vehicle's {@code CameraZoom}, and
     * the press after that goes back to one. The caller checks that the seat looks through a sight.
     */
    static void onZoomKey(LocalPlayer player, AircraftEntity vehicle) {
        AircraftDefinition def = vehicle.definition();
        int max = def != null ? def.cameraZoom : 1;
        if (max <= 1 || vehicle.isDestroyed()) {
            refuse(player);
            return;
        }
        zoom = zoom >= max - 0.01F ? 1.0F : Math.min(zoom * 2.0F, max);
        player.level().playLocalSound(player, ModSounds.get("zoom"), SoundSource.PLAYERS, 0.5F, 1.0F);
    }

    /** {@code switchCameraMode}: night vision on or off, which the server applies as an effect. */
    static void onCameraModeKey(LocalPlayer player, AircraftEntity vehicle) {
        AircraftDefinition def = vehicle.definition();
        if (def == null || !def.nightVision || vehicle.isDestroyed()) {
            refuse(player);
            return;
        }
        mode = mode == Mode.NORMAL ? Mode.NIGHT_VISION : Mode.NORMAL;
        ModNetwork.sendToServer(new CrewActionPacket(vehicle.getId(),
                mode == Mode.NIGHT_VISION ? CrewActionPacket.Action.NIGHT_VISION_ON : CrewActionPacket.Action.NIGHT_VISION_OFF));
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
