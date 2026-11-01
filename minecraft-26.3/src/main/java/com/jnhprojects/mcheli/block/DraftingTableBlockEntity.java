package com.jnhprojects.mcheli.block;

import com.jnhprojects.mcheli.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** {@code MCH_DraftingTableTileEntity}: holds nothing; it is there so the table's model can be drawn. */
public class DraftingTableBlockEntity extends BlockEntity {
    public DraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAFTING_TABLE.get(), pos, state);
    }
}
