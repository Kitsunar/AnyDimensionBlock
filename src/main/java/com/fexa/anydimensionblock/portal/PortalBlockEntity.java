package com.fexa.anydimensionblock.portal;

import com.fexa.anydimensionblock.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity pour le portail dimensionnel.
 * Stocke le ResourceLocation complet du bloc de cadre, ce qui permet
 * de savoir EXACTEMENT quelle dimension ce portail doit ouvrir,
 * y compris pour les blocs moddés non listés dans FrameBlockKey.
 */
public class PortalBlockEntity extends BlockEntity {

    private static final String NBT_BLOCK_ID = "frame_block_id";

    private ResourceLocation frameBlockId = null;

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL_BLOCK_ENTITY.get(), pos, state);
    }

    public ResourceLocation getFrameBlockId() {
        return frameBlockId;
    }

    public void setFrameBlockId(ResourceLocation id) {
        this.frameBlockId = id;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (frameBlockId != null) {
            tag.putString(NBT_BLOCK_ID, frameBlockId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(NBT_BLOCK_ID)) {
            frameBlockId = ResourceLocation.tryParse(tag.getString(NBT_BLOCK_ID));
        }
    }
}
