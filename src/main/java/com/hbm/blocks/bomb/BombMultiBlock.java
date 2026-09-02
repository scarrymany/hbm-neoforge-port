package com.hbm.blocks.bomb;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

/**
 * CE: BombMulti - customizable cluster bomb with GUI to configure explosion params.
 * TileEntity: TileEntityBombMulti (stores config: explosionValue, clusterCount, fireRadius, etc).
 * 1.21: minimal stub, no TE yet, no GUI, no explosion logic.
 * TODO(CE: BombMulti.java:1-264, TileEntityBombMulti.java): GUI, TE, explosion on redstone.
 */
public class BombMultiBlock extends BaseEntityBlock {
    public static final MapCodec<BombMultiBlock> CODEC = simpleCodec(BombMultiBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public BombMultiBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null; // TODO: TileEntityBombMulti
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }
}
