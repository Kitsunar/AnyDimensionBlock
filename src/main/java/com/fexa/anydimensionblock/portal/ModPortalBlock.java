package com.fexa.anydimensionblock.portal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloc portail dimensionnel avec BlockEntity pour stocker le ResourceLocation du cadre.
 * CORRECTIF : utilise BaseEntityBlock pour pouvoir avoir un BlockEntity
 * qui stocke l'identifiant exact du bloc de cadre (y compris blocs moddés).
 */
public class ModPortalBlock extends BaseEntityBlock {

    // Requis par BaseEntityBlock dans NeoForge/MC 1.21.1
    public static final MapCodec<ModPortalBlock> CODEC = MapCodec.unit(ModPortalBlock::new);

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final EnumProperty<Direction.Axis> AXIS_PROP =
            BlockStateProperties.HORIZONTAL_AXIS;

    public static final EnumProperty<FrameBlockKey> FRAME_BLOCK_KEY_PROP =
            EnumProperty.create("frame_block", FrameBlockKey.class);

    public ModPortalBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .strength(-1.0F)
                .lightLevel(state -> 11)
                .noLootTable()
                .pushReaction(PushReaction.BLOCK)
        );
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(AXIS_PROP, Direction.Axis.X)
                        .setValue(FRAME_BLOCK_KEY_PROP, FrameBlockKey.MINECRAFT_STONE)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AXIS_PROP, FRAME_BLOCK_KEY_PROP);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(AXIS_PROP) == Direction.Axis.X) {
            return net.minecraft.world.level.block.Block.box(0, 0, 6, 16, 16, 10);
        } else {
            return net.minecraft.world.level.block.Block.box(6, 0, 0, 10, 16, 16);
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.isOnPortalCooldown()) return;
        PortalTeleporter.handlePortalEntry(player, state, pos, level);
    }

    public boolean canBeReplaced(net.minecraft.world.item.context.BlockPlaceContext useContext) {
        return false;
    }

    /**
     * Convertit un ResourceLocation de bloc en valeur de propriété valide.
     * Si le bloc n'est pas dans l'enum, retourne OTHER.
     * L'identifiant exact est stocké dans le BlockEntity.
     */
    public static FrameBlockKey toPropertyValue(net.minecraft.resources.ResourceLocation blockId) {
        String candidate = blockId.toString().replace(':', '_').replace('/', '_').toUpperCase();
        try {
            return FrameBlockKey.valueOf(candidate);
        } catch (IllegalArgumentException e) {
            return FrameBlockKey.OTHER;
        }
    }
}
