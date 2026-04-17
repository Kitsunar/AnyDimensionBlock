package com.fexa.anydimensionblock.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Génère des chunks entièrement remplis d'un bloc donné.
 * CORRECTIF : on marque chaque chunk comme modifiable dès la génération
 * pour que les modifications du joueur soient bien sauvegardées.
 */
public class BlockFillChunkGenerator extends ChunkGenerator {

    public static final MapCodec<BlockFillChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("block_id")
                            .forGetter(g -> g.targetBlockId)
            ).apply(instance, id -> new BlockFillChunkGenerator(null, id)));

    private final ResourceLocation targetBlockId;
    private final BlockState targetBlock;

    public BlockFillChunkGenerator(RegistryAccess registryAccess, ResourceLocation blockId) {
        super(buildBiomeSource(registryAccess)); // ⚠️ registryAccess DOIT être valide

        this.targetBlockId = blockId;

        Block block = BuiltInRegistries.BLOCK
                .getOptional(blockId)
                .orElse(Blocks.STONE);

        this.targetBlock = block.defaultBlockState();
    }

    private static BiomeSource buildBiomeSource(RegistryAccess registryAccess) {
        if (registryAccess == null) {
            Biome dummyBiome = new Biome.BiomeBuilder()
                    .hasPrecipitation(false)
                    .temperature(0.5f)
                    .downfall(0.5f)
                    .specialEffects(new BiomeSpecialEffects.Builder()
                            .waterColor(0x3F76E4)
                            .waterFogColor(0x050533)
                            .fogColor(0xC0D8FF)
                            .skyColor(0x78A7FF)
                            .build())
                    .mobSpawnSettings(MobSpawnSettings.EMPTY)
                    .generationSettings(BiomeGenerationSettings.EMPTY)
                    .build();
            return new FixedBiomeSource(Holder.direct(dummyBiome));
        }
        Holder<Biome> voidBiome = registryAccess
                .registryOrThrow(Registries.BIOME)
                .getHolder(Biomes.THE_VOID)
                .orElseGet(() -> registryAccess
                        .registryOrThrow(Registries.BIOME)
                        .getHolder(Biomes.PLAINS)
                        .orElseThrow());
        return new FixedBiomeSource(voidBiome);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random,
                              BiomeManager biomeManager, StructureManager structureManager,
                              ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                              RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
                                                         StructureManager structureManager,
                                                         ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(chunk.getPos().getMinBlockX() + x, y,
                            chunk.getPos().getMinBlockZ() + z);
                    if (y == minY) {
                        chunk.setBlockState(pos, Blocks.BEDROCK.defaultBlockState(), false);
                    } else if (y < minY + 60) { // sol
                        chunk.setBlockState(pos, targetBlock, true);
                    } else {
                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
        // CORRECTIF : marquer le chunk comme modifié (unsaved) pour forcer la sauvegarde
        chunk.setLightCorrect(false);
        chunk.setUnsaved(true);
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
                              LevelHeightAccessor level, RandomState random) {
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int height = level.getHeight();
        BlockState[] states = new BlockState[height];
        if (height > 0) states[0] = Blocks.BEDROCK.defaultBlockState();
        for (int i = 1; i < height; i++) states[i] = targetBlock;
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("AnyDimBlock generator: " + targetBlockId);
    }
}
