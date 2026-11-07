package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.block.DraftingTableBlockEntity;
import com.jnhprojects.mcheli.platform.Registered;
import com.jnhprojects.mcheli.platform.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;


/** Our block entities: the drafting table's, which draws its model. */
public final class ModBlockEntities {
    public static final Registered<BlockEntityType<DraftingTableBlockEntity>> DRAFTING_TABLE = Registration.register(Registries.BLOCK_ENTITY_TYPE,
            "drafting_table", key -> BlockEntityType.Builder.of(DraftingTableBlockEntity::new, ModBlocks.DRAFTING_TABLE.get()).build(null));

    private ModBlockEntities() {
    }

    /** Declares the entries; the loader module registers them. */
    public static void init() {
    }
}
