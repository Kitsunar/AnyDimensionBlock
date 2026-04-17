package com.fexa.anydimensionblock.registry;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import com.fexa.anydimensionblock.portal.PortalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AnyDimensionBlock.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalBlockEntity>> PORTAL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("portal_block_entity",
                    () -> BlockEntityType.Builder
                            .of(PortalBlockEntity::new, ModBlocks.DIM_PORTAL.get())
                            .build(null));
}
