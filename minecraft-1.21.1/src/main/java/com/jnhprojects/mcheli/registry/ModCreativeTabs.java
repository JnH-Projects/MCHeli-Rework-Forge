package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.jnhprojects.mcheli.platform.Platform;
import com.jnhprojects.mcheli.platform.Registered;
import com.jnhprojects.mcheli.platform.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One tab per vehicle kind, listing a stack for each loaded definition of that kind. The list is
 * read from the content registry whenever a tab is built, so reloaded content shows up.
 */
public final class ModCreativeTabs {
    public static final Registered<CreativeModeTab> HELICOPTERS = register("helicopters", "helicopter", "ah-64",
            CreativeModeTabs.SPAWN_EGGS);
    public static final Registered<CreativeModeTab> PLANES = register("planes", "plane", "f-35b", key(HELICOPTERS));
    public static final Registered<CreativeModeTab> TANKS = register("tanks", "tank", "m1a2", key(PLANES));
    public static final Registered<CreativeModeTab> VEHICLES = register("vehicles", "vehicle", "mk15", key(TANKS));

    private ModCreativeTabs() {
    }

    private static ResourceKey<CreativeModeTab> key(Registered<CreativeModeTab> tab) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, tab.id());
    }

    private static Registered<CreativeModeTab> register(String name, String kind, String icon, ResourceKey<CreativeModeTab> after) {
        return Registration.register(Registries.CREATIVE_MODE_TAB, name, key -> Platform.get().creativeTab(after)
                .title(Component.translatable("itemGroup.mcheli." + name))
                .icon(() -> {
                    AircraftDefinition def = MCHeli.content().any(icon);
                    return def != null ? AircraftItem.stackOf(def) : new ItemStack(ModItems.forKind(kind));
                })
                .displayItems((params, output) -> {
                    for (AircraftDefinition def : MCHeli.content().all()) {
                        if (def.kind().equals(kind)) {
                            output.accept(AircraftItem.stackOf(def));
                        }
                    }
                })
                .build());
    }

    /** Our tabs, for rebuilding their contents after a content reload. */
    public static List<CreativeModeTab> all() {
        return List.of(HELICOPTERS.get(), PLANES.get(), TANKS.get(), VEHICLES.get());
    }

    /** Declares the entries; the loader module registers them. */
    public static void init() {
    }
}
