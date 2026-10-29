package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.registry.ModDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A vehicle item as 1.0.0 saved it: one item per vehicle, such as {@code mcheli:ah-64}.
 *
 * <p>These are still registered for the vehicles imported at startup, so that stacks in 1.0.0
 * worlds load instead of vanishing. They work as before, are not listed anywhere, and turn into
 * the generic item of their kind ({@link AircraftItem#stackOf}) as soon as they sit in a player's
 * inventory. Stacks in chests convert once a player takes them out.
 */
public final class LegacyAircraftItem extends AircraftItem {
    private final String definitionName;

    public LegacyAircraftItem(String kind, String definitionName, Properties properties) {
        super(kind, properties);
        this.definitionName = definitionName;
    }

    @Override
    protected @Nullable String definitionName(ItemStack stack) {
        return definitionName;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof Player player)) {
            return;
        }
        AircraftDefinition def = MCHeli.content().any(definitionName);
        if (def == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == stack) {
                inventory.setItem(i, convert(stack, def));
                return;
            }
        }
    }

    /** The generic stack for {@code def}, keeping count, custom name and anything else set on the old one. */
    static ItemStack convert(ItemStack legacy, AircraftDefinition def) {
        ItemStack converted = AircraftItem.stackOf(def);
        converted.applyComponents(legacy.getComponentsPatch());
        converted.set(ModDataComponents.VEHICLE.get(), def.name());
        converted.setCount(legacy.getCount());
        return converted;
    }
}
