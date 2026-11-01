package com.jnhprojects.mcheli.item;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Supplier;

/**
 * Sets up a piece of equipment that lives in the world as an entity: the GLTD, the container, the
 * UAV station. Like the originals' items it goes on top of the block clicked, facing the way the
 * player looks, and only where it fits.
 */
public class PlacedEntityItem extends Item {
    private final Supplier<? extends EntityType<?>> type;

    public PlacedEntityItem(Supplier<? extends EntityType<?>> type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos above = context.getClickedPos().above();
        Entity entity = type.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (entity == null) {
            return InteractionResult.FAIL;
        }
        entity.snapTo(above.getX() + 0.5, above.getY(), above.getZ() + 0.5, player != null ? player.getYRot() : 0.0F, 0.0F);
        if (!level.noCollision(entity, entity.getBoundingBox().deflate(0.1)) || !level.getEntities(entity, entity.getBoundingBox()).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.addFreshEntity(entity);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position());
        }
        ItemStack stack = context.getItemInHand();
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
