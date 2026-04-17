package com.fexa.anydimensionblock.item;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import com.fexa.anydimensionblock.portal.ModPortalBlock;
import com.fexa.anydimensionblock.portal.PortalFrameValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class DimensionWandItem extends Item {

    public DimensionWandItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(0)
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();

        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockState clickedState = level.getBlockState(clickedPos);
        Block clickedBlock = clickedState.getBlock();

        // Si on clique sur un bloc portail existant → ignorer
        if (clickedBlock instanceof ModPortalBlock) {
            return InteractionResult.PASS;
        }

        // Si on clique sur l'air (intérieur potentiel du portail), chercher le cadre autour
        Block frameBlock = null;
        BlockPos frameSearchPos = null;

        if (clickedBlock == Blocks.AIR || !clickedState.isSolidRender(level, clickedPos)) {
            // Chercher un bloc de cadre solide adjacent
            frameBlock = findSolidNeighbor(level, clickedPos);
            if (frameBlock == null) {
                player.displayClientMessage(
                    Component.translatable("item.anydimensionblock.dimension_wand.no_frame_near")
                        .withStyle(ChatFormatting.YELLOW),
                    true
                );
                return InteractionResult.FAIL;
            }
            frameSearchPos = clickedPos;
        } else {
            // Clic sur un bloc solide (cadre potentiel)
            if (clickedBlock == Blocks.BEDROCK) {
                player.displayClientMessage(
                    Component.translatable("item.anydimensionblock.dimension_wand.invalid_block")
                        .withStyle(ChatFormatting.RED),
                    true
                );
                return InteractionResult.FAIL;
            }
            frameBlock = clickedBlock;
            frameSearchPos = clickedPos;
        }

        // Chercher un cadre de portail valide autour du bloc cliqué
        PortalFrameValidator.PortalFrame frame = PortalFrameValidator.findPortalFrame(level, frameSearchPos, frameBlock);

        if (frame == null) {
            player.displayClientMessage(
                Component.translatable("item.anydimensionblock.dimension_wand.no_frame",
                    frameBlock.getName().getString())
                    .withStyle(ChatFormatting.YELLOW),
                true
            );
            return InteractionResult.FAIL;
        }

        // Activer le portail
        PortalFrameValidator.activatePortal(serverLevel, frame, frameBlock);

        player.displayClientMessage(
            Component.translatable("item.anydimensionblock.dimension_wand.activated",
                frameBlock.getName().getString())
                .withStyle(ChatFormatting.AQUA),
            true
        );

        // Effet sonore
        level.playSound(null, frameSearchPos, net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.0f);

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Cherche un bloc solide (non-air, non-portail) adjacent à la position donnée.
     * Utilisé quand le joueur clique à l'intérieur du cadre.
     */
    private Block findSolidNeighbor(Level level, BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState state = level.getBlockState(neighbor);
            Block b = state.getBlock();
            if (b != Blocks.AIR && b != Blocks.BEDROCK
                    && !(b instanceof ModPortalBlock)
                    && state.isSolidRender(level, neighbor)) {
                return b;
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
            Component.translatable("item.anydimensionblock.dimension_wand.tooltip1")
                .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
            Component.translatable("item.anydimensionblock.dimension_wand.tooltip2")
                .withStyle(ChatFormatting.DARK_AQUA)
        );
    }
}
