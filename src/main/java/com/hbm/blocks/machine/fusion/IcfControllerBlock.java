package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blockentity.machine.fusion.IcfControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code MachineICFController} (regname {@code icf_controller}): a single-block
 * HE-charged laser accumulator, not dummyable - see {@link IcfControllerBlockEntity}'s javadoc for
 * the capacitor/turbocharger sub-multiblock simplification this makes versus CE.
 */
public class IcfControllerBlock extends BaseEntityBlock {

    /** Reuses {@link HorizontalDirectionalBlock}'s own static property rather than declaring a duplicate. */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public IcfControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IcfControllerBlockEntity(FusionBlockEntities.ICF_CONTROLLER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == FusionBlockEntities.ICF_CONTROLLER.get() ? ITickableBE.ticker() : null;
    }
}
