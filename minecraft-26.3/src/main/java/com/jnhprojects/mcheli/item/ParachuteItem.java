package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.entity.ParachuteEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The parachute, {@code MCH_ItemParachute}: used in mid-air, it opens above the player. */
public class ParachuteItem extends Item {
    public ParachuteItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isPassenger() || player.onGround()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ParachuteEntity.open(level, player, ParachuteEntity.Kind.ITEM);
            player.getItemInHand(hand).consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
