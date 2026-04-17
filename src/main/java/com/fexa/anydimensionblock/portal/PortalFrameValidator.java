package com.fexa.anydimensionblock.portal;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class PortalFrameValidator {

    public static final int MIN_INNER_WIDTH = 2;
    public static final int MIN_INNER_HEIGHT = 3;
    public static final int MAX_INNER_WIDTH = 21;
    public static final int MAX_INNER_HEIGHT = 21;

    public record PortalFrame(
        BlockPos bottomLeft,
        int totalWidth,
        int totalHeight,
        Direction.Axis axis,
        Block frameBlock,
        List<BlockPos> innerPositions
    ) {}

    public static PortalFrame findPortalFrame(Level level, BlockPos searchPos, Block frameBlock) {
        PortalFrame frameX = tryFindFrame(level, searchPos, frameBlock, Direction.Axis.X);
        if (frameX != null) return frameX;
        return tryFindFrame(level, searchPos, frameBlock, Direction.Axis.Z);
    }

    private static PortalFrame tryFindFrame(Level level, BlockPos searchPos, Block frameBlock, Direction.Axis axis) {
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        BlockPos base = searchPos;

        while (isFrameOrEmpty(level, base.below(), frameBlock)) {
            if (level.getBlockState(base.below()).is(frameBlock)) {
                base = base.below();
            } else {
                break;
            }
        }

        if (!level.getBlockState(base).is(frameBlock)) {
            for (int dy = -25; dy <= 25; dy++) {
                if (level.getBlockState(searchPos.above(dy)).is(frameBlock)) {
                    base = searchPos.above(dy);
                    break;
                }
            }
        }

        BlockPos leftBase = base;
        for (int i = 0; i < MAX_INNER_WIDTH + 2; i++) {
            BlockPos prev = leftBase.relative(along.getOpposite());
            if (level.getBlockState(prev).is(frameBlock)) {
                leftBase = prev;
            } else {
                break;
            }
        }

        int totalWidth = 0;
        BlockPos cursor = leftBase;
        while (level.getBlockState(cursor).is(frameBlock) && totalWidth <= MAX_INNER_WIDTH + 2) {
            cursor = cursor.relative(along);
            totalWidth++;
        }

        if (totalWidth < MIN_INNER_WIDTH + 2 || totalWidth > MAX_INNER_WIDTH + 2) return null;

        while (level.getBlockState(leftBase.below()).is(frameBlock)) {
            leftBase = leftBase.below();
        }

        int totalHeight = 0;
        BlockPos vCursor = leftBase;
        while (level.getBlockState(vCursor).is(frameBlock) && totalHeight <= MAX_INNER_HEIGHT + 2) {
            vCursor = vCursor.above();
            totalHeight++;
        }

        if (totalHeight < MIN_INNER_HEIGHT + 2 || totalHeight > MAX_INNER_HEIGHT + 2) return null;

        if (!validateFrame(level, leftBase, totalWidth, totalHeight, frameBlock, axis)) return null;

        List<BlockPos> inner = collectInnerPositions(leftBase, totalWidth, totalHeight, axis);
        return new PortalFrame(leftBase, totalWidth, totalHeight, axis, frameBlock, inner);
    }

    private static boolean isFrameOrEmpty(Level level, BlockPos pos, Block frameBlock) {
        BlockState s = level.getBlockState(pos);
        return s.is(frameBlock) || s.isAir();
    }

    private static boolean validateFrame(Level level, BlockPos bottomLeft, int width, int height,
                                          Block frameBlock, Direction.Axis axis) {
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        for (int i = 0; i < width; i++) {
            BlockPos bottom = bottomLeft.relative(along, i);
            BlockPos top = bottom.above(height - 1);
            if (!level.getBlockState(bottom).is(frameBlock)) return false;
            if (!level.getBlockState(top).is(frameBlock)) return false;
        }

        for (int y = 0; y < height; y++) {
            BlockPos left = bottomLeft.above(y);
            BlockPos right = bottomLeft.relative(along, width - 1).above(y);

            if (!level.getBlockState(left).is(frameBlock)) return false;
            if (!level.getBlockState(right).is(frameBlock)) return false;

            for (int i = 1; i < width - 1; i++) {
                BlockPos inner = bottomLeft.relative(along, i).above(y);
                if (y > 0 && y < height - 1) {
                    BlockState state = level.getBlockState(inner);
                    if (!state.isAir()
                            && !state.is(Blocks.NETHER_PORTAL)
                            && !state.is(Blocks.FIRE)
                            && !(state.getBlock() instanceof ModPortalBlock)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static List<BlockPos> collectInnerPositions(BlockPos bottomLeft, int width, int height,
                                                         Direction.Axis axis) {
        List<BlockPos> positions = new ArrayList<>();
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        for (int y = 1; y < height - 1; y++) {
            for (int i = 1; i < width - 1; i++) {
                positions.add(bottomLeft.relative(along, i).above(y));
            }
        }
        return positions;
    }

    /**
     * Place les blocs de portail + écrit le ResourceLocation exact dans le BlockEntity.
     * CORRECTIF : chaque bloc portail stocke l'ID exact du bloc de cadre via son BlockEntity,
     * ce qui garantit que tous les blocs moddés (mappés à OTHER dans l'enum) mènent
     * chacun à leur propre dimension distincte.
     */
    public static void activatePortal(ServerLevel level, PortalFrame frame, Block frameBlock) {
        ResourceLocation blockRL = BuiltInRegistries.BLOCK.getKey(frameBlock);
        FrameBlockKey propValue = ModPortalBlock.toPropertyValue(blockRL);

        for (BlockPos pos : frame.innerPositions()) {
            level.setBlock(pos,
                com.fexa.anydimensionblock.registry.ModBlocks.DIM_PORTAL.get().defaultBlockState()
                    .setValue(ModPortalBlock.FRAME_BLOCK_KEY_PROP, propValue)
                    .setValue(ModPortalBlock.AXIS_PROP, frame.axis()),
                3);

            // CORRECTIF CRITIQUE : stocker l'ID exact du bloc dans le BlockEntity
            // pour que les blocs moddés (= OTHER) aient chacun leur propre dimension
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PortalBlockEntity pbe) {
                pbe.setFrameBlockId(blockRL);
            }
        }

        AnyDimensionBlock.LOGGER.info("Portal activated for block '{}' ({}, {}x{}, axis {})",
                blockRL, frameBlock.getDescriptionId(),
                frame.totalWidth(), frame.totalHeight(), frame.axis());
    }
}
