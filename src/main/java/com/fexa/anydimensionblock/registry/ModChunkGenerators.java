package com.fexa.anydimensionblock.registry;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import com.fexa.anydimensionblock.worldgen.BlockFillChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Enregistrement du CODEC de BlockFillChunkGenerator.
 * OBLIGATOIRE pour que NeoForge / Minecraft puisse sérialiser/désérialiser
 * le chunk generator lors de la sauvegarde et du rechargement des dimensions.
 */
public class ModChunkGenerators {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, AnyDimensionBlock.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<BlockFillChunkGenerator>>
            BLOCK_FILL = CHUNK_GENERATORS.register("block_fill", () -> BlockFillChunkGenerator.CODEC);
}
