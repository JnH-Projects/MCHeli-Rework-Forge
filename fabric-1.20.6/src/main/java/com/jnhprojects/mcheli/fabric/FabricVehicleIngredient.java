package com.jnhprojects.mcheli.fabric;

import com.jnhprojects.mcheli.item.VehicleIngredient;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** {@link VehicleIngredient} as a Fabric ingredient, read from the {@code fabric:type} key. */
record FabricVehicleIngredient(String vehicle) implements CustomIngredient {
    static final CustomIngredientSerializer<FabricVehicleIngredient> SERIALIZER = new CustomIngredientSerializer<>() {
        private final MapCodec<FabricVehicleIngredient> codec =
                VehicleIngredient.codec(FabricVehicleIngredient::new, FabricVehicleIngredient::vehicle);
        private final StreamCodec<RegistryFriendlyByteBuf, FabricVehicleIngredient> streamCodec =
                VehicleIngredient.streamCodec(FabricVehicleIngredient::new, FabricVehicleIngredient::vehicle);

        @Override
        public ResourceLocation getIdentifier() {
            return VehicleIngredient.ID;
        }

        @Override
        public MapCodec<FabricVehicleIngredient> getCodec(boolean allowEmpty) {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FabricVehicleIngredient> getPacketCodec() {
            return streamCodec;
        }
    };

    @Override
    public boolean test(ItemStack stack) {
        return VehicleIngredient.matches(vehicle, stack);
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        return VehicleIngredient.displayStacks(vehicle).toList();
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
