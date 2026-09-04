package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blockentity.machine.fusion.IcfBlockEntity;
import com.hbm.blockentity.machine.fusion.IcfControllerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Exact CE {@code BlockICF} (regname {@code icf_block}). Placeholder after assemble.
 * {@link #IO_ENABLED} = port. Never obtainable — no BlockItem.
 * {@code onRemove}: restore original + {@code assembled=false} — CE {@code :80-94},
 * PWR {@code onRemove} shape so restore is not torn down by {@code super}.
 */
public class IcfBlock extends BaseEntityBlock {

    public static final MapCodec<IcfBlock> CODEC = simpleCodec(IcfBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final BooleanProperty IO_ENABLED = BooleanProperty.create("io");

    public IcfBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(IO_ENABLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IO_ENABLED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IcfBlockEntity(FusionBlockEntities.ICF_BLOCK.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == FusionBlockEntities.ICF_BLOCK.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // CE BlockICF.java:80-94 — restore original, mark controller disassembled
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof IcfBlockEntity icf) {

            BlockPos corePos = icf.getCorePos();
            if (corePos != null && level.getBlockEntity(corePos) instanceof IcfControllerBlockEntity controller) {
                controller.assembled = false;
                controller.setChanged();
            }

            BlockState original = icf.getOriginalBlockState();
            if (original != null) {
                level.setBlock(pos, original, 3);
                return;
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
