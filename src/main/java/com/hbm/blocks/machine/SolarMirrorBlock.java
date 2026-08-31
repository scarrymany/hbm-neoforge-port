package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.blockentity.machine.SolarMirrorBlockEntity;
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
 * Ported from CE's {@code SolarMirror} (regname {@code solar_mirror}): a plain single block, not
 * dummyable (mirrors are independently placed and each just points at one target). No GUI - CE aims
 * a mirror with a dedicated tool item this pass does not own, see
 * {@link SolarMirrorBlockEntity}'s javadoc.
 */
public class SolarMirrorBlock extends BaseEntityBlock {

    public static final MapCodec<SolarMirrorBlock> CODEC = simpleCodec(SolarMirrorBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public SolarMirrorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarMirrorBlockEntity(PowerGenBlockEntities.SOLAR_MIRROR.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PowerGenBlockEntities.SOLAR_MIRROR.get() ? ITickableBE.ticker() : null;
    }
}
