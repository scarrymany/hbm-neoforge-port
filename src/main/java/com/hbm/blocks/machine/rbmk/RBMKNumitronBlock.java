package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKNumitronBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * CE: RBMKNumitron - numitron panel with 2 numeric displays + screwdriver GUI.
 * TileEntity: TileEntityRBMKNumitron (2x DisplayUnit, RTTY polling).
 * 1.21: stub TE, no GUI yet.
 * TODO(CE: RBMKNumitron.java:1-56, TileEntityRBMKNumitron.java:1-293): GUI, RTTY.
 */
public class RBMKNumitronBlock extends RBMKMiniPanelBlock {
    public static final MapCodec<RBMKNumitronBlock> CODEC = simpleCodec(RBMKNumitronBlock::new);

    public RBMKNumitronBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<RBMKNumitronBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKNumitronBlockEntity(pos, state);
    }
}
