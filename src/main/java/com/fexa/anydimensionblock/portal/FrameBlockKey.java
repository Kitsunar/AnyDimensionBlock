package com.fexa.anydimensionblock.portal;

import net.minecraft.util.StringRepresentable;

/**
 * Remplace StringProperty (supprimé dans MC 1.21.1).
 * Chaque valeur correspond à un bloc vanilla courant, plus OTHER pour les blocs moddés.
 */
public enum FrameBlockKey implements StringRepresentable {
    MINECRAFT_STONE("minecraft_stone"),
    MINECRAFT_GRANITE("minecraft_granite"),
    MINECRAFT_DIORITE("minecraft_diorite"),
    MINECRAFT_ANDESITE("minecraft_andesite"),
    MINECRAFT_COBBLESTONE("minecraft_cobblestone"),
    MINECRAFT_OAK_LOG("minecraft_oak_log"),
    MINECRAFT_BIRCH_LOG("minecraft_birch_log"),
    MINECRAFT_SPRUCE_LOG("minecraft_spruce_log"),
    MINECRAFT_JUNGLE_LOG("minecraft_jungle_log"),
    MINECRAFT_ACACIA_LOG("minecraft_acacia_log"),
    MINECRAFT_DARK_OAK_LOG("minecraft_dark_oak_log"),
    MINECRAFT_OAK_PLANKS("minecraft_oak_planks"),
    MINECRAFT_GLASS("minecraft_glass"),
    MINECRAFT_IRON_BLOCK("minecraft_iron_block"),
    MINECRAFT_GOLD_BLOCK("minecraft_gold_block"),
    MINECRAFT_DIAMOND_BLOCK("minecraft_diamond_block"),
    MINECRAFT_EMERALD_BLOCK("minecraft_emerald_block"),
    MINECRAFT_OBSIDIAN("minecraft_obsidian"),
    MINECRAFT_SAND("minecraft_sand"),
    MINECRAFT_GRAVEL("minecraft_gravel"),
    MINECRAFT_DIRT("minecraft_dirt"),
    MINECRAFT_GRASS_BLOCK("minecraft_grass_block"),
    MINECRAFT_NETHERRACK("minecraft_netherrack"),
    MINECRAFT_END_STONE("minecraft_end_stone"),
    MINECRAFT_BRICKS("minecraft_bricks"),
    MINECRAFT_SANDSTONE("minecraft_sandstone"),
    MINECRAFT_QUARTZ_BLOCK("minecraft_quartz_block"),
    MINECRAFT_PRISMARINE("minecraft_prismarine"),
    MINECRAFT_PURPUR_BLOCK("minecraft_purpur_block"),
    MINECRAFT_COAL_BLOCK("minecraft_coal_block"),
    OTHER("other");

    private final String serializedName;

    FrameBlockKey(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    @Override
    public String toString() {
        return serializedName;
    }
}
