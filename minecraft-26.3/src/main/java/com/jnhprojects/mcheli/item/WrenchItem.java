package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The wrench, {@code MCH_ItemWrench}. Right-clicking a damaged vehicle repairs 10 hit points, once
 * a second, at one point of the wrench's durability. Sneaking switches the vehicle to its next
 * paint scheme.
 *
 * <p>The original repaired while the use key was held; a click per repair works the same with
 * today's item use, which stops as soon as the cursor leaves the hull.
 */
public class WrenchItem extends Item {
    /** An iron tool's durability, the original's tool material. */
    private static final int DURABILITY = 250;
    private static final int REPAIR = 10;
    private static final int COOLDOWN = 20;

    public WrenchItem(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    /** Server only. */
    public static InteractionResult useOn(ItemStack stack, AircraftEntity vehicle, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return vehicle.nextTexture() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (player.getCooldowns().isOnCooldown(stack) || !vehicle.repair(REPAIR)) {
            return InteractionResult.FAIL;
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        stack.hurtAndBreak(1, player, hand);
        ModSounds.playAt(vehicle, "wrench", 1.0F, 0.9F + vehicle.getRandom().nextFloat() * 0.2F);
        return InteractionResult.SUCCESS;
    }
}
