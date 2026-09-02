package com.hbm.blocks.network.energy;

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
 * CE: BlockCableGauge - red wire gauge display, shows HE power flowing through attached cable.
 * TileEntity: extends TileEntityCableBaseNT, displays power + IRORValueProvider for RoR.
 * 1.21: minimal stub, no TE yet, no power display, no cable connection.
 * TODO(CE: BlockCableGauge.java:1-190): TE, power reading, cable connection, RoR.
 */
public class CableGaugeBlock extends BaseEntityBlock {
    public static final MapCodec<CableGaugeBlock> CODEC = simpleCodec(CableGaugeBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public CableGaugeBlock(Properties props) {
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
        return null; // TODO: TileEntityCableGauge
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
