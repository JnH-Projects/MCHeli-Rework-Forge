package com.jnhprojects.mcheli.fabric;

import com.jnhprojects.mcheli.item.VehicleIngredient;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.stream.Stream;

/** {@link VehicleIngredient} as a Fabric ingredient, read from the {@code fabric:type} key. */
record FabricVehicleIngredient(String vehicle) implements CustomIngredient {
    static final CustomIngredientSerializer<FabricVehicleIngredient> SERIALIZER = new CustomIngredientSerializer<>() {
        private final MapCodec<FabricVehicleIngredient> codec =
                VehicleIngredient.codec(FabricVehicleIngredient::new, FabricVehicleIngredient::vehicle);
        private final StreamCodec<RegistryFriendlyByteBuf, FabricVehicleIngredient> streamCodec =
                VehicleIngredient.streamCodec(FabricVehicleIngredient::new, FabricVehicleIngredient::vehicle);

        @Override
        public Identifier getIdentifier() {
            return VehicleIngredient.ID;
        }

        @Override
        public MapCodec<FabricVehicleIngredient> getCodec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FabricVehicleIngredient> getStreamCodec() {
            return streamCodec;
        }
    };

    @Override
    public boolean test(ItemStack stack) {
        return VehicleIngredient.matches(vehicle, stack);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return VehicleIngredient.items();
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public SlotDisplay display() {
        return VehicleIngredient.display(vehicle);
    }
}
