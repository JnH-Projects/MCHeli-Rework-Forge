package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** One tab per vehicle kind, each showing the items generated from that kind's definitions. */
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
                    Item item = ModItems.aircraft(icon);
                    return new ItemStack(item != null ? item : Items.FEATHER);
                })
                .displayItems((params, output) -> ModItems.ofKind(kind).forEach(item -> output.accept(item.get())))
                .build());
    }

    public static void register(BusGroup modBus) {
        TABS.register(modBus);
    }
}
