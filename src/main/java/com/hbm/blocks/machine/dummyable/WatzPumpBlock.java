package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.WatzPumpBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code WatzPump} — Dummyable {1,0,0,0,0,0} offset 0. TE is render-bbox only.
 */
public class WatzPumpBlock extends BlockDummyable {

    public WatzPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new WatzPumpBlockEntity(DummyableProcessBlockEntities.WATZ_PUMP.get(), pos, state)
                : null;
    }
}
