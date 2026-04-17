package com.fexa.anydimensionblock.registry;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import com.fexa.anydimensionblock.portal.ModPortalBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, AnyDimensionBlock.MODID);

    public static final DeferredHolder<Block, ModPortalBlock> DIM_PORTAL =
            BLOCKS.register("dim_portal", ModPortalBlock::new);
}
