package com.jnhprojects.mcheli.neoforge;

import com.jnhprojects.mcheli.item.VehicleIngredient;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

/** {@link VehicleIngredient} as a NeoForge ingredient, read from the {@code neoforge:ingredient_type} key. */
record NeoForgeVehicleIngredient(String vehicle) implements ICustomIngredient {
    static final IngredientType<NeoForgeVehicleIngredient> TYPE = new IngredientType<>(
            VehicleIngredient.codec(NeoForgeVehicleIngredient::new, NeoForgeVehicleIngredient::vehicle),
            VehicleIngredient.streamCodec(NeoForgeVehicleIngredient::new, NeoForgeVehicleIngredient::vehicle));

    @Override
    public boolean test(ItemStack stack) {
        return VehicleIngredient.matches(vehicle, stack);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return VehicleIngredient.items();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }

    @Override
    public SlotDisplay display() {
        return VehicleIngredient.display(vehicle);
    }
}
