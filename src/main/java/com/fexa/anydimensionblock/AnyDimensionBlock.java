package com.fexa.anydimensionblock;

import com.fexa.anydimensionblock.registry.ModBlocks;
import com.fexa.anydimensionblock.registry.ModItems;
import com.fexa.anydimensionblock.registry.ModDimensions;
import com.fexa.anydimensionblock.registry.ModChunkGenerators;
import com.fexa.anydimensionblock.registry.ModBlockEntities;
import com.fexa.anydimensionblock.event.PortalEvents;
import com.fexa.anydimensionblock.event.DimensionEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AnyDimensionBlock.MODID)
public class AnyDimensionBlock {

    public static final String MODID = "anydimensionblock";
    public static final Logger LOGGER = LogManager.getLogger();

    public AnyDimensionBlock(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDimensions.DIMENSION_TYPES.register(modEventBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modEventBus);

        // CORRECTIF : enregistrement du BlockEntityType pour les portails
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        NeoForge.EVENT_BUS.register(PortalEvents.class);
        NeoForge.EVENT_BUS.register(DimensionEvents.class);

        LOGGER.info("Any Dimension Block initialized!");
    }
}
