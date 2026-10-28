package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.List;

/**
 * The vehicle recipes live in {@code data/mcheli/recipe} of the imported content pack, generated
 * from the definitions' {@code AddRecipe} lines. The original listed them on its drafting table;
 * until that is ported, every MCHeli recipe goes into a player's recipe book when they join, so they can be found.
 */
public final class ModRecipes {
    private ModRecipes() {
    }

    public static void register() {
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
