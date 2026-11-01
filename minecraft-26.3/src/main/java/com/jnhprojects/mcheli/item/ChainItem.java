package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.entity.ChainEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The tow chain, {@code MCH_ItemChain}. Use it on what is to be towed, then on what pulls it,
 * between two and sixteen blocks away; use it on either end of a chain to take the chain off.
 * Vehicles and containers hand the click over here.
 */
public class ChainItem extends Item {
    private static final double MIN_DISTANCE = 2.0;
    private static final double MAX_DISTANCE = 16.0;

    /** The first end each player has hooked, until they hook the second. Server only. */
    private static final Map<Player, UUID> HOOKED = new WeakHashMap<>();

    public ChainItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** A click with the chain on {@code target}. */
    public static InteractionResult useOn(Entity target, Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ChainEntity existing = ChainEntity.on(target);
        if (existing != null) {
            existing.disconnect();
            HOOKED.remove(player);
            return InteractionResult.SUCCESS;
        }
        UUID firstId = HOOKED.get(player);
        Entity first = firstId != null ? level.getEntity(firstId) : null;
        if (first == null || first.isRemoved()) {
            HOOKED.put(player, target.getUUID());
            ModSounds.playAt(target, "chain", 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (first == target) {
            return InteractionResult.PASS;
        }
        double distance = target.distanceTo(first);
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {
            ModSounds.playAt(player, "ng", 1.0F, 1.0F);
            return InteractionResult.FAIL;
        }
        ChainEntity.connect(level, first, target);
        HOOKED.remove(player);
        return InteractionResult.SUCCESS;
    }
}
