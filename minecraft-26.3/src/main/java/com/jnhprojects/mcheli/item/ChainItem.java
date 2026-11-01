package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.entity.ChainEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The tow chain, {@code MCH_ItemChain}. Use it on both ends, in either order, two to sixteen
 * blocks apart: the vehicle pulls, so one end must be a vehicle. Use it on either end of a chain
 * to take the chain off, or on the hooked end again to let go of it. Vehicles and containers
 * hand the click over here, and every step is told to the player above the hotbar.
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
            MCHeli.LOGGER.debug("Chain: {} takes the chain off {}", player.getName().getString(), target);
            existing.disconnect();
            HOOKED.remove(player);
            player.sendOverlayMessage(Component.translatable("message.mcheli.chain.removed"));
            return InteractionResult.SUCCESS;
        }
        UUID firstId = HOOKED.get(player);
        Entity first = firstId != null ? level.getEntity(firstId) : null;
        // An end hooked far away was most likely forgotten: start over from this one.
        if (first == null || first.isRemoved() || target.distanceTo(first) > MAX_DISTANCE) {
            MCHeli.LOGGER.debug("Chain: {} hooks {}", player.getName().getString(), target);
            HOOKED.put(player, target.getUUID());
            ModSounds.playAt(target, "chain", 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.mcheli.chain.hooked", target.getDisplayName()));
            return InteractionResult.SUCCESS;
        }
        if (first == target) {
            MCHeli.LOGGER.debug("Chain: {} lets go of {}", player.getName().getString(), target);
            HOOKED.remove(player);
            player.sendOverlayMessage(Component.translatable("message.mcheli.chain.released"));
            return InteractionResult.SUCCESS;
        }
        double distance = target.distanceTo(first);
        if (distance < MIN_DISTANCE) {
            MCHeli.LOGGER.debug("Chain: {} and {} are only {} blocks apart", first, target, distance);
            ModSounds.playAt(player, "ng", 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.mcheli.chain.too_close", String.format("%.1f", distance)));
            return InteractionResult.FAIL;
        }
        if (!ChainEntity.canTow(first) && !ChainEntity.canTow(target)) {
            MCHeli.LOGGER.debug("Chain: neither {} nor {} can tow", first, target);
            ModSounds.playAt(player, "ng", 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.mcheli.chain.no_vehicle"));
            return InteractionResult.FAIL;
        }
        MCHeli.LOGGER.debug("Chain: {} chains {} and {}", player.getName().getString(), first, target);
        ChainEntity chain = ChainEntity.connect(level, first, target);
        HOOKED.remove(player);
        Entity towed = chain.towed();
        Entity tow = chain.tow();
        player.sendOverlayMessage(Component.translatable("message.mcheli.chain.connected",
                towed != null ? towed.getDisplayName() : first.getDisplayName(), tow != null ? tow.getDisplayName() : target.getDisplayName()));
        return InteractionResult.SUCCESS;
    }
}
