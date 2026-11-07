package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.pack.ContentSource;
import com.jnhprojects.mcheli.content.throwable.ThrowableDefinition;
import com.jnhprojects.mcheli.entity.projectile.ThrowableEntity;
import com.jnhprojects.mcheli.registry.ModDataComponents;
import com.jnhprojects.mcheli.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Hand grenades, {@code MCH_ItemThrowable}. Like the vehicles there is one item, and the
 * {@link ModDataComponents#THROWABLE} component says which definition a stack throws, so pack
 * throwables need no item of their own. Held like a bow: the longer the draw, the further it goes.
 */
public class ThrowableItem extends Item {
    private static final int FULL_DRAW = 35;

    public ThrowableItem(Properties properties) {
        super(properties);
    }

    /** A stack of {@code def}, showing the item model the import wrote for it. */
    public static ItemStack stackOf(ThrowableDefinition def) {
        ItemStack stack = new ItemStack(ModItems.THROWABLE.get());
        stack.set(ModDataComponents.THROWABLE.get(), def.name());
        stack.set(DataComponents.MAX_STACK_SIZE, def.stackSize);
        if (def.pack().equals(ContentSource.ORIGINAL)) {
            MCHeli.legacyContent().iconIndex(def.name())
                    .ifPresent(index -> stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(index)));
        }
        return stack;
    }

    private static @Nullable ThrowableDefinition definition(ItemStack stack) {
        String name = stack.get(ModDataComponents.THROWABLE.get());
        return name != null ? MCHeli.content().throwable(name) : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        String name = stack.get(ModDataComponents.THROWABLE.get());
        if (name == null) {
            return super.getName(stack);
        }
        ThrowableDefinition def = MCHeli.content().throwable(name);
        return Component.translatableWithFallback("item.mcheli." + name, def != null ? def.displayName : name);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (definition(player.getItemInHand(hand)) == null) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        ThrowableDefinition def = definition(stack);
        if (def == null) {
            return;
        }
        int drawn = Math.max(getUseDuration(stack, entity) - remainingTime, 5);
        float strength = drawn <= FULL_DRAW ? drawn / 25.0F : 1.0F;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide()) {
            level.addFreshEntity(ThrowableEntity.thrown(level, entity, def, strength));
        }
        if (entity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        stack.consume(1, entity);
    }

    /** Dispensers throw them straight out, {@code MCH_ItemThrowableDispenseBehavior}. */
    public static class DispenseBehavior extends DefaultDispenseItemBehavior {
        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            ThrowableDefinition def = definition(stack);
            if (def == null) {
                return super.execute(source, stack);
            }
            Direction facing = source.state().getValue(DispenserBlock.FACING);
            Vec3 at = source.center().add(facing.getStepX() * 0.7, facing.getStepY() * 0.7, facing.getStepZ() * 0.7);
            Vec3 direction = new Vec3(facing.getStepX(), facing.getStepY() + 0.1, facing.getStepZ());
            source.level().addFreshEntity(ThrowableEntity.dispensed(source.level(), at, direction, def));
            stack.shrink(1);
            return stack;
        }
    }
}
