package com.jnhprojects.mcheli.forge;

import com.jnhprojects.mcheli.item.VehicleIngredient;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import org.jspecify.annotations.Nullable;

/** {@link VehicleIngredient} as a Forge ingredient, read from the {@code type} key. */
final class ForgeVehicleIngredient extends AbstractIngredient {
    static final MapCodec<ForgeVehicleIngredient> CODEC = VehicleIngredient.codec(ForgeVehicleIngredient::new, ForgeVehicleIngredient::vehicle);
    static final StreamCodec<RegistryFriendlyByteBuf, ForgeVehicleIngredient> STREAM_CODEC =
            VehicleIngredient.streamCodec(ForgeVehicleIngredient::new, ForgeVehicleIngredient::vehicle);
    static final IIngredientSerializer<ForgeVehicleIngredient> SERIALIZER = new IIngredientSerializer<>() {
        @Override
        public MapCodec<? extends ForgeVehicleIngredient> codec() {
            return CODEC;
        }

        @Override
        public void write(RegistryFriendlyByteBuf buffer, ForgeVehicleIngredient value) {
            STREAM_CODEC.encode(buffer, value);
        }

        @Override
        public ForgeVehicleIngredient read(RegistryFriendlyByteBuf buffer) {
            return STREAM_CODEC.decode(buffer);
        }
    };

    private final String vehicle;

    private ForgeVehicleIngredient(String vehicle) {
        this.vehicle = vehicle;
    }

    String vehicle() {
        return vehicle;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return VehicleIngredient.matches(vehicle, stack);
    }

    /** Read when shown, so a vehicle loaded after the recipes still appears. */
    @Override
    public ItemStack[] getItems() {
        return VehicleIngredient.displayStacks(vehicle).toArray(ItemStack[]::new);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> serializer() {
        return SERIALIZER;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForgeVehicleIngredient other && vehicle.equals(other.vehicle);
    }

    @Override
    public int hashCode() {
        return vehicle.hashCode();
    }
}
