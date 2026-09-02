package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKTerminalBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * CE: RBMKTerminal - RBMK terminal panel, displays 7x7 grid of RBMK column states.
 * Opens GUI on right-click. TileEntity: TileEntityRBMKTerminal.
 * 1.21: stub TE, no GUI yet.
 * TODO(CE: RBMKTerminal.java:1-32, TileEntityRBMKTerminal.java:1-185): GUI, RBMK scan.
 */
public class RBMKTerminalBlock extends RBMKMiniPanelBlock {
    public static final MapCodec<RBMKTerminalBlock> CODEC = simpleCodec(RBMKTerminalBlock::new);

    public RBMKTerminalBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<RBMKTerminalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKTerminalBlockEntity(pos, state);
    }
}
