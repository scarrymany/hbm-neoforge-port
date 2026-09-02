package com.hbm.blocks.machine.rbmk;

import com.hbm.api.block.IToolable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.rbmk.RBMKDisplayBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * CE: com.hbm.blocks.machine.rbmk.RBMKDisplay
 * RBMK display panel - shows a 7x7 grid of RBMK column status. No GUI, rotates with screwdriver.
 * 
 * TODO(CE:com.hbm.render.tileentity.RenderRBMKDisplay.java:1-173): Port TESR renderer that draws
 * colored 3D columns on panel face. BE already scans 7×7 columns (RBMKDisplayBlockEntity:60-97);
 * missing only the client-side renderer registration + draw logic.
 */
public class RBMKDisplayBlock extends RBMKMiniPanelBlock implements IToolable {
    public static final MapCodec<RBMKDisplayBlock> CODEC = simpleCodec(RBMKDisplayBlock::new);

    public RBMKDisplayBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<RBMKDisplayBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new RBMKDisplayBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ITickableBE.ticker();
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;

        if (!world.isClientSide) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof RBMKDisplayBlockEntity display) {
                display.rotate();
            }
        }
        return true;
    }
}
