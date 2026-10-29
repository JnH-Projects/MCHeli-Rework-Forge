package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.item.AircraftItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * One tab per vehicle kind, listing a stack for each loaded definition of that kind. The list is
 * read from the content registry whenever a tab is built, so reloaded content shows up.
 */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MCHeli.MOD_ID);

    public static final RegistryObject<CreativeModeTab> HELICOPTERS = register("helicopters", "helicopter", "ah-64",
            CreativeModeTabs.SPAWN_EGGS);
    public static final RegistryObject<CreativeModeTab> PLANES = register("planes", "plane", "f-35b", HELICOPTERS.getKey());
    public static final RegistryObject<CreativeModeTab> TANKS = register("tanks", "tank", "m1a2", PLANES.getKey());
    public static final RegistryObject<CreativeModeTab> VEHICLES = register("vehicles", "vehicle", "mk15", TANKS.getKey());

    private ModCreativeTabs() {
    }

    private static RegistryObject<CreativeModeTab> register(String name, String kind, String icon, ResourceKey<CreativeModeTab> after) {
        return TABS.register(name, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mcheli." + name))
                .withTabsBefore(after)
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

    public static void register(BusGroup modBus) {
        TABS.register(modBus);
    }
}
