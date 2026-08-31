package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import com.hbm.blockentity.machine.rbmk.RBMKInletBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

/**
 * Standalone water-feed pipe stub, not an RBMK grid column - see
 * {@link com.hbm.blockentity.machine.rbmk.RBMKInletBlockEntity}'s javadoc. Ported from CE's
 * {@code RBMKInlet} block class.
 */
public class RBMKInletBlock extends BaseEntityBlock {

    public static final MapCodec<RBMKInletBlock> CODEC = simpleCodec(RBMKInletBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public RBMKInletBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKInletBlockEntity(RBMKBlockEntities.INLET.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == RBMKBlockEntities.INLET.get() ? ITickableBE.ticker() : null;
    }
}
