package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.jnhprojects.mcheli.item.LegacyAircraftItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

/**
 * One item per vehicle kind; the stack says which vehicle (see {@link AircraftItem}). The 1.0.0
 * items, one per vehicle, are kept for the content present at startup so old worlds keep them.
 */
public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MCHeli.MOD_ID);

    public static final RegistryObject<Item> HELICOPTER = kind("helicopter");
    public static final RegistryObject<Item> PLANE = kind("plane");
    public static final RegistryObject<Item> TANK = kind("tank");
    public static final RegistryObject<Item> VEHICLE = kind("vehicle");

    private static final Map<String, RegistryObject<Item>> BY_KIND =
            Map.of("helicopter", HELICOPTER, "plane", PLANE, "tank", TANK, "vehicle", VEHICLE);

    private ModItems() {
    }

    private static RegistryObject<Item> kind(String kind) {
        return ITEMS.register(kind, () -> new AircraftItem(kind, new Item.Properties().setId(ITEMS.key(kind)).stacksTo(1)));
    }

    public static void register(BusGroup modBus, ContentRegistry content) {
        for (AircraftDefinition def : content.all()) {
            String name = def.name();
            if (BY_KIND.containsKey(name)) {
                // 1.0.0 could not have had such an item either: it would have clashed the same way.
                continue;
            }
            String kind = def.kind();
            ITEMS.register(name, () -> new LegacyAircraftItem(kind, name,
                    new Item.Properties().setId(ITEMS.key(name)).stacksTo(1)));
        }
        ITEMS.register(modBus);
    }

    /** The generic item of a vehicle kind ({@code helicopter}, {@code plane}, {@code tank} or {@code vehicle}). */
    public static Item forKind(String kind) {
        RegistryObject<Item> item = BY_KIND.get(kind);
        if (item == null) {
            throw new IllegalArgumentException("Unknown vehicle kind " + kind);
        }
        return item.get();
    }
}
