package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.MachineMiniRTGBlockEntity;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
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
 * Ported from CE's {@code MachineMiniRTG} - one CE class backs two distinct registry blocks
 * ({@code machine_minirtg} / {@code machine_powerrtg}), branching on block identity inside the tile
 * entity's {@code update()}/{@code getMaxPower()}. This port keeps the two-registered-blocks part of
 * that shape (see the research report's open question: either a block-identity branch or a
 * constructor flag works, "flagging only so it isn't silently 'fixed'") but resolves the branch via
 * an explicit constructor flag on this one shared block class instead of an identity check inside
 * the block entity - {@link #isPolonium()} is read once, at block-entity construction time, by
 * {@link com.hbm.blockentity.machine.PowerGenBlockEntities}'s factory lambda.
 */
public class MachineMiniRTGBlock extends BaseEntityBlock {

    public static final MapCodec<MachineMiniRTGBlock> CODEC = simpleCodec(p -> new MachineMiniRTGBlock(p, false));

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    private final boolean polonium;

    public MachineMiniRTGBlock(Properties properties, boolean polonium) {
        super(properties);
        this.polonium = polonium;
    }

    public boolean isPolonium() {
        return polonium;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineMiniRTGBlockEntity(PowerGenBlockEntities.MACHINE_MINI_RTG.get(), pos, state, polonium);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PowerGenBlockEntities.MACHINE_MINI_RTG.get() ? ITickableBE.ticker() : null;
    }
}
