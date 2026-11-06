package com.jnhprojects.mcheli.menu;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.registry.ModBlocks;
import com.jnhprojects.mcheli.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.List;

/**
 * The drafting table, {@code MCH_DraftingTableGuiContainer}: the player's inventory and one output
 * slot. The screen browses the MCHeli recipes in the player's recipe book and asks for one by its
 * display id; the server finds the recipe, takes the ingredients from the inventory and puts the
 * result in the output slot.
 */
public class DraftingTableMenu extends AbstractContainerMenu {
    public static final int OUTPUT_X = 178;
    public static final int OUTPUT_Y = 90;

    private final ContainerLevelAccess access;
    private final ResultContainer output = new ResultContainer();
    private final int outputSlot;

    /** The client side, from the open screen packet. */
    public DraftingTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public DraftingTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.DRAFTING_TABLE.get(), containerId);
        this.access = access;
        addStandardInventorySlots(inventory, 30, 140);
        outputSlot = slots.size();
        addSlot(new Slot(output, 0, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    /**
     * {@code createRecipeItem}: makes one of the recipe behind {@code id}, if it is ours, the output
     * slot is free and the player carries the ingredients. Creative players need neither.
     */
    public void craft(ServerPlayer player, RecipeDisplayId id) {
        boolean creative = player.getAbilities().instabuild;
        Slot slot = slots.get(outputSlot);
        if (slot.hasItem() && !creative) {
            return;
        }
        RecipeManager.ServerDisplayInfo info = player.level().getServer().getRecipeManager().getRecipeFromDisplay(id);
        if (info == null) {
            return;
        }
        RecipeHolder<?> holder = info.parent();
        if (!holder.id().identifier().getNamespace().equals(MCHeli.MOD_ID) || !(holder.value() instanceof CraftingRecipe recipe)) {
            return;
        }
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        if (!creative && !take(player.getInventory(), ingredients, true)) {
            return;
        }
        if (!creative) {
            take(player.getInventory(), ingredients, false);
        }
        slot.set(recipe.assemble(CraftingInput.EMPTY, player.registryAccess()));
        broadcastChanges();
    }

    /**
     * Finds, and unless {@code dryRun} removes, one item per ingredient. A stack serves several
     * ingredients as far as its count goes.
     */
    private static boolean take(Inventory inventory, List<Ingredient> ingredients, boolean dryRun) {
        int[] used = new int[inventory.getContainerSize()];
        for (Ingredient ingredient : ingredients) {
            int found = -1;
            for (int i = 0; i < inventory.getContainerSize() && found < 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && stack.getCount() > used[i] && ingredient.test(stack)) {
                    found = i;
                }
            }
            if (found < 0) {
                return false;
            }
            used[found]++;
        }
        if (!dryRun) {
            for (int i = 0; i < used.length; i++) {
                if (used[i] > 0) {
                    inventory.removeItem(i, used[i]);
                }
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DRAFTING_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index != outputSlot) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (!moveItemStackTo(stack, 0, outputSlot, true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    /** What is left in the output goes back to the player, as in the original. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            clearContainer(player, output);
        }
    }
}
