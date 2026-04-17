package com.fexa.anydimensionblock.dimension;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Stocke la correspondance block_id ↔ dimension_id dans le world save.
 * Accessible uniquement côté serveur.
 */
public class DimensionSavedData extends SavedData {

    public static final String DATA_NAME = AnyDimensionBlock.MODID + "_dimensions";

    // block resource location → dimension resource location
    private final Map<String, String> blockToDimension = new HashMap<>();

    public DimensionSavedData() {}

    public static DimensionSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        DimensionSavedData data = new DimensionSavedData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.blockToDimension.put(entry.getString("block"), entry.getString("dim"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        blockToDimension.forEach((block, dim) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("block", block);
            entry.putString("dim", dim);
            list.add(entry);
        });
        tag.put("entries", list);
        return tag;
    }

    /**
     * Retourne l'ID de dimension pour ce bloc, ou null si pas encore créée.
     */
    public ResourceLocation getDimensionForBlock(ResourceLocation blockId) {
        String result = blockToDimension.get(blockId.toString());
        if (result == null) return null;
        return ResourceLocation.tryParse(result);
    }

    /**
     * Enregistre la dimension pour ce bloc.
     */
    public void registerDimension(ResourceLocation blockId, ResourceLocation dimensionId) {
        blockToDimension.put(blockId.toString(), dimensionId.toString());
        setDirty();
        AnyDimensionBlock.LOGGER.info("[AnyDimBlock] Registered dimension {} for block {}",
                dimensionId, blockId);
    }

    public boolean hasDimension(ResourceLocation blockId) {
        return blockToDimension.containsKey(blockId.toString());
    }

    public Map<String, String> getAllEntries() {
        return Map.copyOf(blockToDimension);
    }

    public static Factory<DimensionSavedData> factory() {
        return new Factory<>(DimensionSavedData::new, DimensionSavedData::load, null);
    }
}
