package com.jnhprojects.mcheli.neoforge;

import com.jnhprojects.mcheli.item.VehicleIngredient;
import net.minecraft.world.item.ItemStack;
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
    public Stream<ItemStack> getItems() {
        return VehicleIngredient.displayStacks(vehicle);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }
}
