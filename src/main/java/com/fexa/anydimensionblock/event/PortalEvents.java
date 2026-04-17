package com.fexa.anydimensionblock.event;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import com.fexa.anydimensionblock.portal.ModPortalBlock;
import com.fexa.anydimensionblock.portal.PortalFrameValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Gère la destruction des portails quand le cadre est cassé.
 */
public class PortalEvents {

    /**
     * Quand un bloc est cassé, vérifier si c'est un cadre de portail.
     * Si oui, détruire les blocs de portail à l'intérieur.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Vérifier si des blocs de portail adjacents dépendent de ce bloc
        checkAndRemovePortals(level, pos);
    }

    /**
     * Recherche les blocs de portail dans un rayon et les retire si le cadre est incomplet.
     */
    private static void checkAndRemovePortals(Level level, BlockPos brokenPos) {
        int searchRadius = 25; // max portal size + buffer

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos check = brokenPos.offset(dx, dy, dz);
                    BlockState checkState = level.getBlockState(check);

                    if (checkState.getBlock() instanceof ModPortalBlock) {
                        // Tenter de valider que le cadre est encore intact
                        // Si on ne trouve plus de cadre valide → retirer le portail
                        // Simple heuristique : si un bord manque directement, retirer
                        if (!isPortalStillValid(level, check, checkState)) {
                            level.setBlock(check, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    private static boolean isPortalStillValid(Level level, BlockPos portalPos, BlockState state) {
        // Vérifier qu'au moins un bloc solide non-portail adjacent existe (bord)
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighbor = portalPos.relative(dir);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.isSolidRender(level, neighbor)
                    && !(neighborState.getBlock() instanceof ModPortalBlock)
                    && neighborState.getBlock() != net.minecraft.world.level.block.Blocks.AIR) {
                return true;
            }
        }
        return false;
    }
}
