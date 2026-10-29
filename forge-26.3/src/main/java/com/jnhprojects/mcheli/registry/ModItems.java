package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.item.AircraftItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Items are generated from the loaded content, one per vehicle definition. */
public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MCHeli.MOD_ID);
    private static final Map<String, RegistryObject<Item>> AIRCRAFT = new LinkedHashMap<>();
    private static final Map<String, List<RegistryObject<Item>>> BY_KIND = new LinkedHashMap<>();

    private ModItems() {
    }

    public static void register(BusGroup modBus, ContentRegistry content) {
        for (AircraftDefinition def : content.all()) {
            String name = def.name();
            RegistryObject<Item> item = ITEMS.register(name, () -> new AircraftItem(name,
                    new Item.Properties().setId(ITEMS.key(name)).stacksTo(1)));
            AIRCRAFT.put(name, item);
            BY_KIND.computeIfAbsent(def.kind(), k -> new ArrayList<>()).add(item);
        }
        ITEMS.register(modBus);
    }

    public static @Nullable Item aircraft(String name) {
        RegistryObject<Item> item = AIRCRAFT.get(name);
        return item != null && item.isPresent() ? item.get() : null;
    }

    public static Collection<RegistryObject<Item>> aircraft() {
        return Collections.unmodifiableCollection(AIRCRAFT.values());
    }

    /** Items of one kind ({@code helicopter}, {@code plane}, ...), in content order. */
    public static List<RegistryObject<Item>> ofKind(String kind) {
        return Collections.unmodifiableList(BY_KIND.getOrDefault(kind, List.of()));
    }
}
