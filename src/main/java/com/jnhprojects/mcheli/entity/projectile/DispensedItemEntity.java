package com.jnhprojects.mcheli.entity.projectile;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.weapon.LegacyItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A canister that uses an item on every block around where it lands, {@code MCH_EntityDispensedItem}:
 * water bombers put out fires, crop dusters spread bone meal or seeds, and so on.
 */
public class DispensedItemEntity extends ProjectileEntity {
    public DispensedItemEntity(EntityType<? extends DispensedItemEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        WeaponDefinition w = weapon();
        if (w != null && !w.disableSmoke) {
            trail(w.trajectoryParticle, 3, 7.0F * w.smokeSize);
        }
        if (!level().isClientSide() && w != null) {
            Vec3 m = getDeltaMovement();
            if (acceleration < 1.0E-4) {
                m = new Vec3(m.x * 0.999, m.y, m.z * 0.999);
            }
            if (isInWater()) {
                m = m.scale(w.velocityInWater);
            }
            setDeltaMovement(m);
        }
        tickBomblets();
    }

    @Override
    protected void impact(Impact hit, float damageFactor) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        WeaponDefinition w = weapon();
        Item item = w != null ? LegacyItems.item(w.dispenseItem, w.dispenseDamage) : null;
        if (w != null && item != null && user() instanceof ServerPlayer player && !player.isRemoved()) {
            BlockPos center = hit.block() != null ? hit.block() : blockPosition();
            int range = w.dispenseRange - 1;
            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        int dist = x * x + y * y + z * z;
                        if (dist <= range * range && (dist <= 0.5 * range * range || random.nextInt(2) == 0)) {
                            useOn(server, center.offset(x, y, z), item);
                        }
                    }
                }
            }
        }
        discard();
    }

    private void useOn(ServerLevel server, BlockPos pos, Item item) {
        if (pos.getY() < server.getMinY() || pos.getY() >= server.getMaxY()) {
            return;
        }
        BlockState state = server.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (item == Items.WATER_BUCKET) {
            // A water drop douses fire and quenches lava instead of placing water.
            FluidState fluid = server.getFluidState(pos);
            if (state.getBlock() instanceof BaseFireBlock) {
                server.removeBlock(pos, false);
            } else if (fluid.is(FluidTags.LAVA)) {
                server.setBlockAndUpdate(pos, fluid.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
            }
            return;
        }
        // Used from above with nobody holding it, the way a dispenser block would.
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0), Direction.UP, pos, false);
        item.useOn(new UseOnContext(server, null, InteractionHand.MAIN_HAND, new ItemStack(item), hit));
    }

    @Override
    protected void sprinkleBomblet() {
        WeaponDefinition w = weapon();
        Vec3 m = getDeltaMovement();
        DispensedItemEntity child = spawnChild(ModEntities.DISPENSED_ITEM.get(), m.x, m.y, m.z, random.nextInt(360), 0.0F);
        if (child == null || w == null) {
            return;
        }
        float spread = w.bombletDiff;
        child.setDeltaMovement(m.x + (random.nextFloat() - 0.5F) * spread,
                m.y / 2.0 + (random.nextFloat() - 0.5F) * spread / 2.0F,
                m.z + (random.nextFloat() - 0.5F) * spread);
        child.setBomblet();
        level().addFreshEntity(child);
    }

    @Override
    public String defaultModel() {
        return "bomb";
    }
}
