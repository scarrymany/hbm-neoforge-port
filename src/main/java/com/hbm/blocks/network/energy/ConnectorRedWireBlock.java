package com.hbm.blocks.network.energy;

import com.hbm.blockentity.network.energy.ConnectorBlockEntity;
import com.hbm.blockentity.network.energy.EnergyNetworkBlockEntities;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.blocks.network.ConnectorRedWire} (read full) - single-face energy
 * connector with 10m range, extends CE's {@code PylonBase}. Has direction-based collision box and TE
 * {@code TileEntityConnector}.
 */
public class ConnectorRedWireBlock extends BaseEntityBlock implements ITooltipProvider {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final double f = 1d / 16d;
    private static final double min = 5 * f;
    private static final double max = 11 * f;

    private static final VoxelShape AABB_UP = Block.box(min * 16, 0.0D, min * 16, max * 16, max * 16, max * 16);
    private static final VoxelShape AABB_DOWN = Block.box(min * 16, min * 16, min * 16, max * 16, 16.0D, max * 16);
    private static final VoxelShape AABB_SOUTH = Block.box(min * 16, min * 16, 0.0D, max * 16, max * 16, max * 16);
    private static final VoxelShape AABB_NORTH = Block.box(min * 16, min * 16, min * 16, max * 16, max * 16, 16.0D);
    private static final VoxelShape AABB_EAST = Block.box(0.0D, min * 16, min * 16, max * 16, max * 16, max * 16);
    private static final VoxelShape AABB_WEST = Block.box(min * 16, min * 16, min * 16, 16.0D, max * 16, max * 16);

    public static final MapCodec<ConnectorRedWireBlock> CODEC = simpleCodec(ConnectorRedWireBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ConnectorRedWireBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConnectorBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> AABB_NORTH;
            case SOUTH -> AABB_SOUTH;
            case WEST -> AABB_WEST;
            case EAST -> AABB_EAST;
            case UP -> AABB_UP;
            case DOWN -> AABB_DOWN;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ConnectorBlockEntity connector) {
            connector.disconnectAll();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("§6Connection Type: §eSingle"));
        list.add(Component.literal("§6Connection Range: §e10m"));
    }
}
