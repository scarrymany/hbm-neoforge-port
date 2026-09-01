package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.FractionSpacerBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code FractionSpacer} — Dummyable {0,0,1,1,1,1} offset 1. TE is render-bbox only
 * ({@code TileEntitySpacer}), no menu.
 */
public class FractionSpacerBlock extends BlockDummyable {

    public FractionSpacerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new FractionSpacerBlockEntity(DummyableProcessBlockEntities.FRACTION_SPACER.get(), pos, state)
                : null;
    }
}
