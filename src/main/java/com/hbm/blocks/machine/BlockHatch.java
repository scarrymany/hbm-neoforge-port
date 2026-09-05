package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.SealBlockEntities;
import com.hbm.blockentity.machine.SealHatchBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code BlockHatch} ({@code BlockHatch.java}). Interior lid placed by {@link BlockSeal#closeSeal}.
 * Hardness/resistance ∞; creative tab null. TE {@code TileEntityHatch} / {@code tileentity_seal_lid}.
 */
public class BlockHatch extends BaseEntityBlock {

    public static final MapCodec<BlockHatch> CODEC = simpleCodec(BlockHatch::new);

    public BlockHatch(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SealHatchBlockEntity(SealBlockEntities.SEAL_HATCH.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == SealBlockEntities.SEAL_HATCH.get() ? ITickableBE.ticker() : null;
    }

    public void setControllerPos(Level world, BlockPos pos, BlockPos controller) {
        if (world.getBlockEntity(pos) instanceof SealHatchBlockEntity hatch) {
            hatch.setControllerPos(controller);
        }
    }
}
