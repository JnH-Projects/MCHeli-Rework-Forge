package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * The laser rangefinder, {@code MCH_ItemRangeFinder}. Held up to the eye it is a scope that reads
 * the range to whatever it points at; the attack key spots targets for the team, using one of
 * its ten charges. Crafting it with redstone charges it again.
 */
public class RangefinderItem extends Item {
    public static final int CHARGES = 10;

    public RangefinderItem(Properties properties) {
        super(properties.durability(CHARGES));
    }

    /** Not from a vehicle seat: the vehicle's own sight does that job. */
    public static boolean canUse(Player player) {
        return !(player.getVehicle() instanceof AircraftEntity);
    }

    /** Charges left, from none to {@link #CHARGES}. */
    public static int charges(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPYGLASS;
    }
}
