package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Matches one vehicle, such as an AH-6 in the recipe that turns it into an AH-6X. Every vehicle of
 * a kind shares an item, so a plain item ingredient would accept any of them. In JSON:
 * <pre>{"type": "mcheli:vehicle", "vehicle": "ah-6"}</pre>
 */
public final class VehicleIngredient extends AbstractIngredient {
    public static final MapCodec<VehicleIngredient> CODEC =
            Codec.STRING.fieldOf("vehicle").xmap(VehicleIngredient::new, VehicleIngredient::vehicle);
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleIngredient> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(VehicleIngredient::new, VehicleIngredient::vehicle).cast();
    public static final IIngredientSerializer<VehicleIngredient> SERIALIZER = IIngredientSerializer.simple(CODEC, STREAM_CODEC);

    private final String vehicle;

    public VehicleIngredient(String vehicle) {
        super(kindItems());
        this.vehicle = vehicle;
    }

    @SuppressWarnings("deprecation")
    private static HolderSet<Item> kindItems() {
        return HolderSet.direct(Item::builtInRegistryHolder,
                ModItems.HELICOPTER.get(), ModItems.PLANE.get(), ModItems.TANK.get(), ModItems.VEHICLE.get());
    }

    public String vehicle() {
        return vehicle;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return stack != null && vehicle.equals(AircraftItem.vehicleOf(stack));
    }

    @Override
    public SlotDisplay display() {
        AircraftDefinition def = MCHeli.content().any(vehicle);
        return def != null ? new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(AircraftItem.stackOf(def)))
                : SlotDisplay.Empty.INSTANCE;
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
        return o instanceof VehicleIngredient other && vehicle.equals(other.vehicle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicle);
    }
}
