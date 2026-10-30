package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.pack.ContentSource;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.jnhprojects.mcheli.item.LegacyAircraftItem;
import com.jnhprojects.mcheli.platform.Registered;
import com.jnhprojects.mcheli.platform.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.Map;

/**
 * One item per vehicle kind; the stack says which vehicle (see {@link AircraftItem}). The 1.0.0
 * items, one per vehicle, are kept for the content present at startup so old worlds keep them.
 */
public final class ModItems {
    public static final Registered<Item> HELICOPTER = kind("helicopter");
    public static final Registered<Item> PLANE = kind("plane");
    public static final Registered<Item> TANK = kind("tank");
    public static final Registered<Item> VEHICLE = kind("vehicle");

    private static final Map<String, Registered<Item>> BY_KIND =
            Map.of("helicopter", HELICOPTER, "plane", PLANE, "tank", TANK, "vehicle", VEHICLE);

    private ModItems() {
    }

    private static Registered<Item> kind(String kind) {
        return Registration.register(Registries.ITEM, kind, key -> new AircraftItem(kind, new Item.Properties().stacksTo(1)));
    }

    /**
     * Declares the entries, with a 1.0.0 item for each original vehicle in {@code content}; the
     * loader module registers them.
     */
    public static void init(ContentRegistry content) {
        for (AircraftDefinition def : content.all()) {
            String name = def.name();
            // Only the imported content existed in 1.0.0; pack vehicles never had their own item.
            if (!def.pack().equals(ContentSource.ORIGINAL) || BY_KIND.containsKey(name)) {
                // 1.0.0 could not have had such an item either: it would have clashed the same way.
                continue;
            }
            String kind = def.kind();
            Registration.register(Registries.ITEM, name, key -> new LegacyAircraftItem(kind, name,
                    new Item.Properties().stacksTo(1)));
        }
    }

    /** The generic item of a vehicle kind ({@code helicopter}, {@code plane}, {@code tank} or {@code vehicle}). */
    public static Item forKind(String kind) {
        Registered<Item> item = BY_KIND.get(kind);
        if (item == null) {
            throw new IllegalArgumentException("Unknown vehicle kind " + kind);
        }
        return item.get();
    }
}
