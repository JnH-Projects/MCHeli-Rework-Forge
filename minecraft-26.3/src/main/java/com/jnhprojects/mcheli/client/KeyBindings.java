package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.MCHeli;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Mod key bindings. Throttle, steering, hover, brake and eject reuse the vanilla movement and jump
 * keys (W/S, A/D and Space by default), which matches the original defaults. Firing uses the
 * vanilla use key and the pick block button switches weapons, the right and middle mouse buttons
 * of the original. The rest keep the original's letters where vanilla leaves them free.
 */
public final class KeyBindings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "main"));

    /** Hold to look around without steering. The original used Left Ctrl, which now clashes with sprint. */
    public static final KeyMapping FREE_LOOK = new KeyMapping("key.mcheli.free_look", InputConstants.KEY_LALT, CATEGORY);
    /** Cycles the pilot's camera positions, or toggles the gunner sight in seats that allow it. */
    public static final KeyMapping SWITCH_CAMERA = new KeyMapping("key.mcheli.switch_camera", InputConstants.KEY_H, CATEGORY);
    public static final KeyMapping LANDING_GEAR = new KeyMapping("key.mcheli.landing_gear", InputConstants.KEY_B, CATEGORY);
    /** Zooms through a sight, as the original Z key did; otherwise folds hatches and wings, sweeps variable geometry wings. */
    public static final KeyMapping FOLD = new KeyMapping("key.mcheli.fold", InputConstants.KEY_Z, CATEGORY);
    /** The original used F, which vanilla now uses to swap hands. */
    public static final KeyMapping VTOL = new KeyMapping("key.mcheli.vtol", InputConstants.KEY_V, CATEGORY);
    public static final KeyMapping SWITCH_SEAT = new KeyMapping("key.mcheli.switch_seat", InputConstants.KEY_R, CATEGORY);
    public static final KeyMapping RACK_LOAD = new KeyMapping("key.mcheli.rack_load", InputConstants.KEY_J, CATEGORY);
    public static final KeyMapping RACK_UNLOAD = new KeyMapping("key.mcheli.rack_unload", InputConstants.KEY_U, CATEGORY);
    /** The second weapon switch key of the original; the first is the vanilla pick block button. */
    public static final KeyMapping SWITCH_WEAPON = new KeyMapping("key.mcheli.switch_weapon", InputConstants.KEY_G, CATEGORY);
    public static final KeyMapping WEAPON_MODE = new KeyMapping("key.mcheli.weapon_mode", InputConstants.KEY_X, CATEGORY);
    /** The original used V, which VTOL took over. */
    public static final KeyMapping FLARE = new KeyMapping("key.mcheli.flare", InputConstants.KEY_C, CATEGORY);
    /** The original resupplied from the vehicle's inventory screen, which does not exist yet. */
    public static final KeyMapping RESUPPLY = new KeyMapping("key.mcheli.resupply", InputConstants.KEY_N, CATEGORY);
    /** Night vision on and off in vehicles with {@code EnableNightVision}. The original used C, now the flare key. */
    public static final KeyMapping CAMERA_MODE = new KeyMapping("key.mcheli.camera_mode", InputConstants.KEY_K, CATEGORY);

    public static final List<KeyMapping> ALL = List.of(FREE_LOOK, SWITCH_CAMERA, LANDING_GEAR, FOLD, VTOL, SWITCH_SEAT, RACK_LOAD, RACK_UNLOAD,
            SWITCH_WEAPON, WEAPON_MODE, FLARE, RESUPPLY, CAMERA_MODE);

    private KeyBindings() {
    }
}
