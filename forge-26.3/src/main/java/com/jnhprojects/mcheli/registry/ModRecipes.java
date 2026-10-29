package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.item.VehicleIngredient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * The vehicle recipes live in {@code data/mcheli/recipe} of the imported content pack, generated
 * from the definitions' {@code AddRecipe} lines. The original listed them on its drafting table;
 * until that is ported, every MCHeli recipe goes into a player's recipe book when they join, so they can be found.
 */
public final class ModRecipes {
    private static final DeferredRegister<IIngredientSerializer<?>> INGREDIENTS =
            DeferredRegister.create(ForgeRegistries.Keys.INGREDIENT_SERIALIZERS, MCHeli.MOD_ID);

    static {
        INGREDIENTS.register("vehicle", () -> VehicleIngredient.SERIALIZER);
    }

    private ModRecipes() {
    }

    public static void register(BusGroup modBus) {
        INGREDIENTS.register(modBus);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                List<RecipeHolder<?>> ours = player.level().getServer().getRecipeManager().getRecipes().stream()
                        .filter(holder -> holder.id().identifier().getNamespace().equals(MCHeli.MOD_ID))
                        .toList();
                player.awardRecipes(ours);
            }
        });
    }
}
