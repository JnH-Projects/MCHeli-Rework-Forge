package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Matches one vehicle, such as an AH-6 in the recipe that turns it into an AH-6X. Every vehicle of
 * a kind shares an item, so a plain item ingredient would accept any of them. In JSON:
 * <pre>{"type": "mcheli:vehicle", "vehicle": "ah-6"}</pre>
 * with the type also under {@code neoforge:ingredient_type} and {@code fabric:type}, since each
 * loader names custom ingredients its own way.
 *
 * <p>This is the loader-independent half; each loader module wraps it in its own ingredient class.
 */
public final class VehicleIngredient {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "vehicle");

    private VehicleIngredient() {
    }

    /** The JSON fields, given a way to build the loader's ingredient from a vehicle name. */
    public static <T> MapCodec<T> codec(Function<String, T> create, Function<T, String> vehicle) {
        return Codec.STRING.fieldOf("vehicle").xmap(create, vehicle);
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> streamCodec(Function<String, T> create, Function<T, String> vehicle) {
        return ByteBufCodecs.STRING_UTF8.map(create, vehicle).cast();
    }

    /** The items a vehicle can be: the generic item of each kind. */
    @SuppressWarnings("deprecation")
    public static HolderSet<Item> kindItems() {
        return HolderSet.direct(Item::builtInRegistryHolder,
                ModItems.HELICOPTER.get(), ModItems.PLANE.get(), ModItems.TANK.get(), ModItems.VEHICLE.get());
    }

    public static Stream<Holder<Item>> items() {
        return kindItems().stream();
    }

    public static boolean matches(String vehicle, @Nullable ItemStack stack) {
        return stack != null && vehicle.equals(AircraftItem.vehicleOf(stack));
    }

    /** What recipe books and JEI show: the vehicle itself, or nothing while it is not loaded. */
    public static Stream<ItemStack> displayStacks(String vehicle) {
        AircraftDefinition def = MCHeli.content().any(vehicle);
        return def != null ? Stream.of(AircraftItem.stackOf(def)) : Stream.empty();
    }
}
