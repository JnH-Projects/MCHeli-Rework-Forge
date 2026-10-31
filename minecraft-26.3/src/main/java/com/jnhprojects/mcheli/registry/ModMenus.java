package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.menu.VehicleMenu;
import com.jnhprojects.mcheli.platform.Registered;
import com.jnhprojects.mcheli.platform.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Our screens with slots. The cargo chest uses vanilla's chest menus and needs no entry here. */
public final class ModMenus {
    public static final Registered<MenuType<VehicleMenu>> VEHICLE = Registration.register(Registries.MENU, "vehicle",
            key -> new MenuType<>(VehicleMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }

    /** Declares the entries; the loader module registers them. */
    public static void init() {
    }
}
