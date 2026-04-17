package com.fexa.anydimensionblock.dimension;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import com.fexa.anydimensionblock.worldgen.BlockFillChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Gère la création dynamique de dimensions par bloc.
 *
 * Tous les accès aux champs privés de MinecraftServer sont faits
 * via reflection (executor, progressListenerFactory, isDebug, levels).
 */
public class BlockDimensionManager {

    public static ServerLevel getOrCreateDimension(MinecraftServer server, ResourceLocation blockId) {
        DimensionSavedData savedData = getSavedData(server);
        ResourceLocation dimId = savedData.getDimensionForBlock(blockId);
        if (dimId == null) {
            dimId = createDimensionId(blockId);
            savedData.registerDimension(blockId, dimId);
        }
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimId);
        ServerLevel existing = server.getLevel(levelKey);
        if (existing != null) return existing;
        return createDimension(server, levelKey, blockId);
    }

    private static ServerLevel createDimension(MinecraftServer server,
                                               ResourceKey<Level> levelKey,
                                               ResourceLocation blockId) {
        try {
            Registry<DimensionType> dimTypeRegistry = server.registryAccess()
                    .registryOrThrow(Registries.DIMENSION_TYPE);
            Holder<DimensionType> dimTypeHolder = dimTypeRegistry
                    .getHolder(ResourceKey.create(Registries.DIMENSION_TYPE,
                            ResourceLocation.fromNamespaceAndPath(AnyDimensionBlock.MODID, "block_dimension_type")))
                    .orElseGet(() -> dimTypeRegistry.getHolderOrThrow(
                            net.minecraft.world.level.dimension.BuiltinDimensionTypes.OVERWORLD));

            RegistryAccess registryAccess = server.registryAccess();
            BlockFillChunkGenerator chunkGen = new BlockFillChunkGenerator(registryAccess, blockId);
            LevelStem stem = new LevelStem(dimTypeHolder, chunkGen);

            ServerLevel newLevel = addDynamicLevel(server, levelKey, stem);
            AnyDimensionBlock.LOGGER.info("[AnyDimBlock] Created dimension {} for block {}", levelKey, blockId);
            return newLevel != null ? newLevel : server.overworld();
        } catch (Exception e) {
            AnyDimensionBlock.LOGGER.error("[AnyDimBlock] Failed to create dimension for {}: {}", blockId, e.getMessage(), e);
            return server.overworld();
        }
    }

    /**
     * Tente d'abord la méthode NeoForge patchée, puis l'injection directe.
     */
    @SuppressWarnings("unchecked")
    private static ServerLevel addDynamicLevel(MinecraftServer server,
                                               ResourceKey<Level> levelKey,
                                               LevelStem stem) {
        // Tentative 1 : méthode NeoForge (createNewCustomLevel / addLevel)
        for (Method m : server.getClass().getMethods()) {
            String name = m.getName();
            if (name.equals("createNewCustomLevel") || name.equals("addLevel")) {
                try {
                    m.setAccessible(true);
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length >= 2 && ResourceKey.class.isAssignableFrom(params[0])
                            && LevelStem.class.isAssignableFrom(params[params.length - 1])) {
                        ServerLevel result;
                        if (params.length == 2) {
                            result = (ServerLevel) m.invoke(server, levelKey, stem);
                        } else {
                            result = (ServerLevel) m.invoke(server, levelKey,
                                    server.getWorldData().worldGenOptions().seed(), stem);
                        }
                        if (result != null) return result;
                    }
                } catch (Exception ex) {
                    AnyDimensionBlock.LOGGER.debug("[AnyDimBlock] Method {} failed: {}", name, ex.getMessage());
                }
            }
        }

        // Tentative 2 : injection directe via reflection totale
        try {
            return injectLevelDirectly(server, levelKey, stem);
        } catch (Exception e) {
            AnyDimensionBlock.LOGGER.error("[AnyDimBlock] All level creation methods failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Injection complète via reflection — aucun accès direct aux champs privés.
     * Compatible MC 1.21.1 + NeoForge 21.1.86.
     */
    @SuppressWarnings("unchecked")
    private static ServerLevel injectLevelDirectly(MinecraftServer server,
                                                   ResourceKey<Level> levelKey,
                                                   LevelStem stem) throws Exception {

        // --- 1. Récupérer la Map 'levels' directement par son nom ---
        // On utilise getDeclaredField sur MinecraftServer.class car c'est là qu'il est défini
        Field levelsField = MinecraftServer.class.getDeclaredField("levels");
        levelsField.setAccessible(true);
        Map<ResourceKey<Level>, ServerLevel> levels = (Map<ResourceKey<Level>, ServerLevel>) levelsField.get(server);

        // --- 2. Trouver le LevelStorageAccess ---
        Field storageField = findFieldOfType(server,
                net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.class);
        if (storageField == null)
            throw new NoSuchFieldException("Cannot find LevelStorageAccess in MinecraftServer");
        net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess storageAccess =
                (net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess) storageField.get(server);

        // --- 3. Trouver l'Executor (champ "executor") via reflection ---
        Field executorField = findFieldByName(server, "executor", Executor.class);
        if (executorField == null)
            throw new NoSuchFieldException("Cannot find 'executor' in MinecraftServer");
        Executor executor = (Executor) executorField.get(server);

        // --- 4. Trouver le ChunkProgressListenerFactory ---
        Field listenerFactoryField = findFieldByName(server, "progressListenerFactory",
                net.minecraft.server.level.progress.ChunkProgressListenerFactory.class);
        if (listenerFactoryField == null)
            throw new NoSuchFieldException("Cannot find 'progressListenerFactory' in MinecraftServer");
        net.minecraft.server.level.progress.ChunkProgressListenerFactory listenerFactory =
                (net.minecraft.server.level.progress.ChunkProgressListenerFactory) listenerFactoryField.get(server);

        // --- 5. isDebug ---
        boolean isDebug = false;
        try {
            Field debugField = findFieldByName(server, "debug", boolean.class);
            if (debugField != null) isDebug = (boolean) debugField.get(server);
        } catch (Exception ignored) {}

        // --- 6. Seed ---
        long seed = server.getWorldData().worldGenOptions().seed();

        // --- 7. Créer DerivedLevelData ---
        net.minecraft.world.level.storage.DerivedLevelData derivedData =
                new net.minecraft.world.level.storage.DerivedLevelData(
                        server.getWorldData(),
                        server.getWorldData().overworldData());

        // --- 8. Instancier ServerLevel ---
        ServerLevel newLevel = new ServerLevel(
                server,
                executor,
                storageAccess,
                derivedData,
                levelKey,
                stem,
                listenerFactory.create(0),
                isDebug,
                seed,
                List.of(),
                false,
                server.overworld().getRandomSequences()
        );

        // --- 9. Injection dans la map ---
        levels.put(levelKey, newLevel);
        server.markWorldsDirty(); // ✅ force sauvegarde

        AnyDimensionBlock.LOGGER.info("[AnyDimBlock] Injected level {} directly into server", levelKey);
        return newLevel;
    }

    // ---------------------------------------------------------------
    // Helpers reflection
    // ---------------------------------------------------------------

    /** Cherche un champ Map<ResourceKey<Level>, ?> en vérifiant la première clé. */
    private static Field findFieldByType(Object obj, Class<?> type, ResourceKey<Level> sampleKey) {
        Class<?> cls = obj.getClass();
        while (cls != null) {
            for (Field f : cls.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    try {
                        Object val = f.get(obj);
                        if (val instanceof Map<?, ?> map && !map.isEmpty()) {
                            Object firstKey = map.keySet().iterator().next();
                            if (firstKey instanceof ResourceKey<?> rk &&
                                    rk.registry().equals(Registries.DIMENSION)) {
                                return f;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    /** Cherche un champ du type exact donné, dans toute la hiérarchie. */
    private static Field findFieldOfType(Object obj, Class<?> type) {
        Class<?> cls = obj.getClass();
        while (cls != null) {
            for (Field f : cls.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f;
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    /** Cherche un champ par nom (en clair ou obfusqué), avec type optionnel. */
    private static Field findFieldByName(Object obj, String name, Class<?> type) {
        Class<?> cls = obj.getClass();
        while (cls != null) {
            for (Field f : cls.getDeclaredFields()) {
                if (f.getName().equals(name) && (type == null || type.isAssignableFrom(f.getType()))) {
                    f.setAccessible(true);
                    return f;
                }
            }
            cls = cls.getSuperclass();
        }
        // Pas trouvé par nom → fallback par type uniquement
        if (type != null) return findFieldOfType(obj, type);
        return null;
    }

    // ---------------------------------------------------------------

    public static DimensionSavedData getSavedData(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(DimensionSavedData.factory(), DimensionSavedData.DATA_NAME);
    }

    private static ResourceLocation createDimensionId(ResourceLocation blockId) {
        String path = blockId.getNamespace() + "_" + blockId.getPath().replace('/', '_');
        return ResourceLocation.fromNamespaceAndPath(AnyDimensionBlock.MODID, "dim_" + path);
    }
}