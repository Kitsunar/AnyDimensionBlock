package com.fexa.anydimensionblock.portal;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import com.fexa.anydimensionblock.dimension.BlockDimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;

public class PortalTeleporter {

    private static final int AIR_BUBBLE_RADIUS = 3;
    private static final int RETURN_PORTAL_WIDTH = 4;
    private static final int RETURN_PORTAL_HEIGHT = 5;

    // 160 ticks = 8 secondes de cooldown
    private static final int PORTAL_COOLDOWN_TICKS = 160;

    private static final String NBT_RETURN_DIM   = AnyDimensionBlock.MODID + ":return_dim";
    private static final String NBT_RETURN_X     = AnyDimensionBlock.MODID + ":return_x";
    private static final String NBT_RETURN_Y     = AnyDimensionBlock.MODID + ":return_y";
    private static final String NBT_RETURN_Z     = AnyDimensionBlock.MODID + ":return_z";
    private static final String NBT_RETURN_YAW   = AnyDimensionBlock.MODID + ":return_yaw";
    private static final String NBT_RETURN_PITCH = AnyDimensionBlock.MODID + ":return_pitch";

    public static void handlePortalEntry(ServerPlayer player, BlockState portalState,
                                          BlockPos portalPos, Level level) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        ResourceKey<Level> currentDim = level.dimension();
        String dimPath = currentDim.location().getPath();

        if (dimPath.startsWith("dim_")) {
            teleportToSavedReturn(player, server);
        } else {
            teleportToBlockDimension(player, portalState, portalPos, server, (ServerLevel) level);
        }
    }

    private static void teleportToBlockDimension(ServerPlayer player, BlockState portalState,
                                                   BlockPos portalPos, MinecraftServer server,
                                                   ServerLevel currentLevel) {
        // CORRECTIF PRINCIPAL : lire le ResourceLocation exact depuis le BlockEntity
        // Cela garantit que chaque portail, même fait d'un bloc moddé, va dans sa propre dimension
        ResourceLocation blockId = getBlockIdFromPortal(currentLevel, portalPos, portalState);
        if (blockId == null) {
            AnyDimensionBlock.LOGGER.warn("[AnyDimBlock] Cannot determine frame block for portal at {}", portalPos);
            return;
        }

        Block frameBlock = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (frameBlock == null) {
            AnyDimensionBlock.LOGGER.warn("[AnyDimBlock] Block not found in registry: {}", blockId);
            return;
        }

        // Sauvegarder la position de retour
        saveReturnPosition(player, currentLevel.dimension(), player.position(),
                player.getYRot(), player.getXRot());

        ServerLevel targetDim = BlockDimensionManager.getOrCreateDimension(server, blockId);

        int targetY = targetDim.getMinBuildHeight() + 5;

        // CORRECTIF re-téléportation : le portail de retour est en X=0,Z=0
        // Le joueur arrive en X=0,Z=10 (bien séparé du portail)
        BlockPos portalCenter = new BlockPos(0, targetY, 0);
        BlockPos returnPortalBase = new BlockPos(-(RETURN_PORTAL_WIDTH / 2 + 1), targetY - 1, 0);
        BlockPos arrivalPos = new BlockPos(0, targetY, 10);

        // Créer une large bulle d'air qui englobe portail + zone d'arrivée
        createAirBubble(targetDim, portalCenter, AIR_BUBBLE_RADIUS + 6);
        clearSpawnPlatform(targetDim, portalCenter);
        createReturnPortal(targetDim, returnPortalBase, frameBlock, blockId);

        teleportPlayer(player, targetDim, arrivalPos);
    }

    /**
     * CORRECTIF CRITIQUE : lit d'abord le BlockEntity pour avoir le ResourceLocation exact.
     * Si pas de BlockEntity (portail de retour généré par le mod), cherche le bloc adjacent.
     */
    private static ResourceLocation getBlockIdFromPortal(ServerLevel level, BlockPos portalPos,
                                                           BlockState portalState) {
        // 1. Essayer de lire depuis le BlockEntity (portail créé par la baguette)
        BlockEntity be = level.getBlockEntity(portalPos);
        if (be instanceof PortalBlockEntity pbe && pbe.getFrameBlockId() != null) {
            return pbe.getFrameBlockId();
        }

        // 2. Chercher un bloc portail adjacent qui a un BlockEntity valide
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = portalPos.relative(dir);
            if (level.getBlockState(neighbor).getBlock() instanceof ModPortalBlock) {
                BlockEntity nbe = level.getBlockEntity(neighbor);
                if (nbe instanceof PortalBlockEntity pbe && pbe.getFrameBlockId() != null) {
                    return pbe.getFrameBlockId();
                }
            }
        }

        // 3. Fallback : chercher bloc de cadre adjacent solide
        Block found = findFrameBlockNear(level, portalPos);
        if (found != null) {
            return BuiltInRegistries.BLOCK.getKey(found);
        }

        return null;
    }

    private static void teleportToSavedReturn(ServerPlayer player, MinecraftServer server) {
        CompoundTag persistentData = player.getPersistentData();

        if (persistentData.contains(NBT_RETURN_DIM)) {
            String dimStr = persistentData.getString(NBT_RETURN_DIM);
            double rx = persistentData.getDouble(NBT_RETURN_X);
            double ry = persistentData.getDouble(NBT_RETURN_Y);
            double rz = persistentData.getDouble(NBT_RETURN_Z);
            float yaw = persistentData.getFloat(NBT_RETURN_YAW);
            float pitch = persistentData.getFloat(NBT_RETURN_PITCH);

            ResourceLocation dimRL = ResourceLocation.tryParse(dimStr);
            ServerLevel returnLevel = null;

            if (dimRL != null) {
                ResourceKey<Level> returnKey = ResourceKey.create(Registries.DIMENSION, dimRL);
                returnLevel = server.getLevel(returnKey);
            }

            if (returnLevel == null) {
                returnLevel = server.overworld();
                BlockPos spawnPos = returnLevel.getSharedSpawnPos();
                rx = spawnPos.getX() + 0.5;
                ry = returnLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        spawnPos.getX(), spawnPos.getZ()) + 1;
                rz = spawnPos.getZ() + 0.5;
            }

            persistentData.remove(NBT_RETURN_DIM);
            persistentData.remove(NBT_RETURN_X);
            persistentData.remove(NBT_RETURN_Y);
            persistentData.remove(NBT_RETURN_Z);
            persistentData.remove(NBT_RETURN_YAW);
            persistentData.remove(NBT_RETURN_PITCH);

            player.teleportTo(returnLevel, rx, ry, rz, yaw, pitch);
            player.resetFallDistance();
            player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        } else {
            ServerLevel overworld = server.overworld();
            BlockPos spawnPos = overworld.getSharedSpawnPos();
            int targetY = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    spawnPos.getX(), spawnPos.getZ());
            player.teleportTo(overworld,
                    spawnPos.getX() + 0.5, targetY + 1, spawnPos.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
            player.resetFallDistance();
            player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        }
    }

    private static void saveReturnPosition(ServerPlayer player, ResourceKey<Level> dim,
                                            net.minecraft.world.phys.Vec3 pos, float yaw, float pitch) {
        CompoundTag data = player.getPersistentData();
        data.putString(NBT_RETURN_DIM, dim.location().toString());
        data.putDouble(NBT_RETURN_X, pos.x);
        data.putDouble(NBT_RETURN_Y, pos.y);
        data.putDouble(NBT_RETURN_Z, pos.z);
        data.putFloat(NBT_RETURN_YAW, yaw);
        data.putFloat(NBT_RETURN_PITCH, pitch);
    }

    private static Block findFrameBlockNear(ServerLevel level, BlockPos pos) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos check = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(check);
                    Block b = state.getBlock();
                    if (b != Blocks.AIR && b != Blocks.BEDROCK
                            && !(b instanceof ModPortalBlock)
                            && state.isSolidRender(level, check)) {
                        return b;
                    }
                }
            }
        }
        return null;
    }

    private static void createAirBubble(ServerLevel level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= radius + 2; y++) { // ✅ FIX
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.offset(x, y, z);

                    if (p.getY() > level.getMinBuildHeight() && p.getY() < level.getMaxBuildHeight()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void createReturnPortal(ServerLevel level, BlockPos base, Block frameBlock,
                                            ResourceLocation blockId) {
        int totalW = RETURN_PORTAL_WIDTH + 2;
        int totalH = RETURN_PORTAL_HEIGHT + 2;

        Direction.Axis axis = Direction.Axis.X;
        Direction along = Direction.EAST;

        // Vider la zone
        for (int y = -1; y <= totalH + 1; y++) {
            for (int i = -1; i <= totalW; i++) {
                BlockPos p = base.relative(along, i).above(y);
                if (p.getY() > level.getMinBuildHeight()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Cadre
        for (int i = 0; i < totalW; i++) {
            BlockPos bottom = base.relative(along, i);
            BlockPos top = bottom.above(totalH - 1);
            if (bottom.getY() > level.getMinBuildHeight()) {
                level.setBlock(bottom, frameBlock.defaultBlockState(), 3);
            }
            level.setBlock(top, frameBlock.defaultBlockState(), 3);
        }
        for (int y = 0; y < totalH; y++) {
            BlockPos left = base.above(y);
            BlockPos right = base.relative(along, totalW - 1).above(y);
            if (left.getY() > level.getMinBuildHeight()) {
                level.setBlock(left, frameBlock.defaultBlockState(), 3);
            }
            level.setBlock(right, frameBlock.defaultBlockState(), 3);
        }

        // Intérieur portail avec BlockEntity pour chaque bloc portail
        FrameBlockKey propValue = ModPortalBlock.toPropertyValue(blockId);

        for (int y = 1; y < totalH - 1; y++) {
            for (int i = 1; i < totalW - 1; i++) {
                BlockPos inner = base.relative(along, i).above(y);
                level.setBlock(inner,
                        com.fexa.anydimensionblock.registry.ModBlocks.DIM_PORTAL.get().defaultBlockState()
                                .setValue(ModPortalBlock.AXIS_PROP, axis)
                                .setValue(ModPortalBlock.FRAME_BLOCK_KEY_PROP, propValue),
                        3);
                // CORRECTIF : stocker le ResourceLocation exact dans le BlockEntity du portail de retour
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(inner);
                if (be instanceof PortalBlockEntity pbe) {
                    pbe.setFrameBlockId(blockId);
                }
            }
        }
    }

    private static void teleportPlayer(ServerPlayer player, ServerLevel target, BlockPos pos) {
        player.teleportTo(
                target,
                pos.getX(),
                pos.getY(),
                pos.getZ() - 8,
                player.getYRot(),
                player.getXRot()
        );
        player.resetFallDistance();
        player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
    }

    private static void clearSpawnPlatform(ServerLevel level, BlockPos center) {
        for (int x = -6; x <= 6; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.STONE)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}
