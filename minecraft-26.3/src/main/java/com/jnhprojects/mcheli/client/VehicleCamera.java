package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.network.CrewActionPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

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
    private static final Identifier THERMAL_EFFECT = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "thermal");

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
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        List<Identifier> effects = player.getActivePostEffects();
        boolean active = effects.contains(THERMAL_EFFECT);
        if (mode == Mode.THERMAL && !active) {
            effects.add(THERMAL_EFFECT);
        } else if (mode != Mode.THERMAL && active) {
            effects.remove(THERMAL_EFFECT);
        }
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
        Mode next = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
        if (mode == Mode.NORMAL || next == Mode.NORMAL) {
            ModNetwork.sendToServer(new CrewActionPacket(vehicle.getId(),
                    next == Mode.NORMAL ? CrewActionPacket.Action.NIGHT_VISION_OFF : CrewActionPacket.Action.NIGHT_VISION_ON));
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
