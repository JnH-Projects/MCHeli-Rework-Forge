package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.block.DraftingTableBlock;
import com.jnhprojects.mcheli.platform.Registered;
import com.jnhprojects.mcheli.platform.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Our blocks: the drafting table, lit or not. */
public final class ModBlocks {
    public static final Registered<Block> DRAFTING_TABLE = Registration.register(Registries.BLOCK, "drafting_table",
            key -> new DraftingTableBlock(BlockBehaviour.Properties.of()
                    .strength(0.2F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(DraftingTableBlock.LIT) ? 15 : 0)));

    private ModBlocks() {
    }

    /** Declares the entries; the loader module registers them. */
    public static void init() {
    }
}
