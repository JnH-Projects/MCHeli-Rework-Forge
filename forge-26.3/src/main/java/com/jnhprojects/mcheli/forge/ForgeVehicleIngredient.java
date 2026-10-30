package com.jnhprojects.mcheli.forge;

import com.jnhprojects.mcheli.item.VehicleIngredient;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import org.jspecify.annotations.Nullable;

/** {@link VehicleIngredient} as a Forge ingredient, read from the {@code type} key. */
final class ForgeVehicleIngredient extends AbstractIngredient {
    static final MapCodec<ForgeVehicleIngredient> CODEC = VehicleIngredient.codec(ForgeVehicleIngredient::new, ForgeVehicleIngredient::vehicle);
    static final StreamCodec<RegistryFriendlyByteBuf, ForgeVehicleIngredient> STREAM_CODEC =
            VehicleIngredient.streamCodec(ForgeVehicleIngredient::new, ForgeVehicleIngredient::vehicle);
    static final IIngredientSerializer<ForgeVehicleIngredient> SERIALIZER = IIngredientSerializer.simple(CODEC, STREAM_CODEC);

    private final String vehicle;

    private ForgeVehicleIngredient(String vehicle) {
        super(VehicleIngredient.kindItems());
        this.vehicle = vehicle;
    }

    String vehicle() {
        return vehicle;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return VehicleIngredient.matches(vehicle, stack);
    }

    @Override
    public SlotDisplay display() {
        return VehicleIngredient.display(vehicle);
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
