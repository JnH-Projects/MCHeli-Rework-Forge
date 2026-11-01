package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.pack.ContentSource;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.jnhprojects.mcheli.item.ChainItem;
import com.jnhprojects.mcheli.item.FuelItem;
import com.jnhprojects.mcheli.item.LegacyAircraftItem;
import com.jnhprojects.mcheli.item.ParachuteItem;
import com.jnhprojects.mcheli.item.PlacedEntityItem;
import com.jnhprojects.mcheli.item.RangefinderItem;
import com.jnhprojects.mcheli.item.ThrowableItem;
import com.jnhprojects.mcheli.item.WrenchItem;
import com.jnhprojects.mcheli.platform.Registered;
import com.jnhprojects.mcheli.platform.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;

/**
 * One item per vehicle kind; the stack says which vehicle (see {@link AircraftItem}). The 1.0.0
 * items, one per vehicle, are kept for the content present at startup so old worlds keep them. The
 * equipment (fuel can, wrench, parachute and so on) has one item each, and the throwables share one.
 */
public final class ModItems {
    public static final Registered<Item> HELICOPTER = kind("helicopter");
    public static final Registered<Item> PLANE = kind("plane");
    public static final Registered<Item> TANK = kind("tank");
    public static final Registered<Item> VEHICLE = kind("vehicle");

    public static final Registered<Item> FUEL = Registration.register(Registries.ITEM, "fuel", key -> new FuelItem(new Item.Properties().setId(key)));
    public static final Registered<Item> WRENCH = Registration.register(Registries.ITEM, "wrench", key -> new WrenchItem(new Item.Properties().setId(key)));
    public static final Registered<Item> PARACHUTE = Registration.register(Registries.ITEM, "parachute", key -> new ParachuteItem(new Item.Properties().setId(key)));
    public static final Registered<Item> GLTD = Registration.register(Registries.ITEM, "gltd",
            key -> new PlacedEntityItem(ModEntities.GLTD, new Item.Properties().setId(key).stacksTo(1)));
    public static final Registered<Item> RANGEFINDER = Registration.register(Registries.ITEM, "rangefinder", key -> new RangefinderItem(new Item.Properties().setId(key)));
    public static final Registered<Item> CONTAINER = Registration.register(Registries.ITEM, "container",
            key -> new PlacedEntityItem(ModEntities.CONTAINER, new Item.Properties().setId(key)));
    public static final Registered<Item> CHAIN = Registration.register(Registries.ITEM, "chain", key -> new ChainItem(new Item.Properties().setId(key)));
    /** Every throwable; the stack says which (see {@link ThrowableItem}). */
    public static final Registered<Item> THROWABLE = Registration.register(Registries.ITEM, "throwable", key -> new ThrowableItem(new Item.Properties().setId(key)));

    /** The equipment tab, in the order it lists them. */
    public static final List<Registered<Item>> EQUIPMENT = List.of(FUEL, WRENCH, PARACHUTE, GLTD, RANGEFINDER, CONTAINER, CHAIN);

    private static final Map<String, Registered<Item>> BY_KIND =
            Map.of("helicopter", HELICOPTER, "plane", PLANE, "tank", TANK, "vehicle", VEHICLE);

    private ModItems() {
    }

    private static Registered<Item> kind(String kind) {
        return Registration.register(Registries.ITEM, kind, key -> new AircraftItem(kind, new Item.Properties().setId(key).stacksTo(1)));
    }

    /**
     * Declares the entries, with a 1.0.0 item for each original vehicle in {@code content}; the
     * loader module registers them.
     */
    public static void init(ContentRegistry content) {
        for (AircraftDefinition def : content.all()) {
            String name = def.name();
            // Only the imported content existed in 1.0.0; pack vehicles never had their own item.
            if (!def.pack().equals(ContentSource.ORIGINAL) || BY_KIND.containsKey(name) || EQUIPMENT.stream().anyMatch(e -> e.id().getPath().equals(name))) {
                // 1.0.0 could not have had such an item either: it would have clashed the same way.
                continue;
            }
            String kind = def.kind();
            Registration.register(Registries.ITEM, name, key -> new LegacyAircraftItem(kind, name,
                    new Item.Properties().setId(key).stacksTo(1)));
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
