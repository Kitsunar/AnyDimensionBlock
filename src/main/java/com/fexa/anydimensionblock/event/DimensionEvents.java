package com.fexa.anydimensionblock.event;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Empêche le spawn de mobs dans les dimensions de blocs.
 * Gère aussi l'initialisation quand une dimension est chargée.
 */
public class DimensionEvents {

    /**
     * Bloque tous les spawns de mobs dans les dimensions de blocs.
     *
     * CORRECTIF : MobSpawnEvent.FinalizeSpawn n'existe plus dans NeoForge 21.1.
     * La classe est devenue un event de premier niveau : FinalizeSpawnEvent.
     */
    @SubscribeEvent
    public static void onMobSpawnCheck(FinalizeSpawnEvent event) {
        // getLevel() peut retourner un WorldGenRegion pendant la génération de chunks,
        // qui ne peut pas être casté en Level. On vérifie d'abord.
        if (!(event.getLevel() instanceof Level level)) return;
        if (isBlockDimension(level.dimension())) {
            event.setSpawnCancelled(true);
        }
    }

    /**
     * Log quand une block dimension est chargée.
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ResourceKey<Level> dim = serverLevel.dimension();
            if (isBlockDimension(dim)) {
                AnyDimensionBlock.LOGGER.info("[AnyDimBlock] Block dimension loaded: {}", dim.location());
            }
        }
    }

    /**
     * Vérifie si la dimension est une dimension de bloc créée par ce mod.
     */
    public static boolean isBlockDimension(ResourceKey<Level> dimension) {
        return dimension.location().getNamespace().equals(AnyDimensionBlock.MODID)
                && dimension.location().getPath().startsWith("dim_");
    }
}
