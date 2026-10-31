package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The fuel can, {@code MCH_ItemFuel}. It holds 600 units; the durability bar shows how much is
 * left. Using it refills it from the coal and charcoal in the inventory, as in the original.
 *
 * <p>A can in a fuel slot of the vehicle screen empties into the tank while the vehicle stands on
 * something, as in the original. Not in the original: right-clicking a vehicle with the can pours
 * up to 100 units at once, which saves opening the screen.
 */
public class FuelItem extends Item {
    /** Units a full can holds, the original's maximum damage. */
    public static final int CAPACITY = 600;
    /** Units poured into a vehicle per click: what the original moved per fuel slot per update. */
    private static final int POUR = 100;
    private static final int PER_COAL = 100;
    private static final int PER_CHARCOAL = 75;

    public FuelItem(Properties properties) {
        super(properties.durability(CAPACITY));
    }

    public static int contents(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    /** Fills a vehicle's tank from the can. Server only. */
    public static InteractionResult pour(ItemStack stack, AircraftEntity vehicle, Player player) {
        int added = vehicle.refuel(player.getAbilities().instabuild ? POUR : Math.min(contents(stack), POUR));
        if (added <= 0) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.setDamageValue(stack.getDamageValue() + added);
        }
        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Charcoal first, then coal, each one worth part of the can, until it is full. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isDamaged() || player.getAbilities().instabuild) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            refill(stack, player.getInventory(), Items.CHARCOAL, PER_CHARCOAL);
            refill(stack, player.getInventory(), Items.COAL, PER_COAL);
        }
        return InteractionResult.SUCCESS;
    }

    private static void refill(ItemStack can, Inventory inventory, Item coal, int worth) {
        for (int slot = 0; slot < inventory.getContainerSize() && can.isDamaged(); slot++) {
            ItemStack item = inventory.getItem(slot);
            while (item.is(coal) && !item.isEmpty() && can.isDamaged()) {
                can.setDamageValue(Math.max(can.getDamageValue() - worth, 0));
                item.shrink(1);
            }
        }
    }
}
