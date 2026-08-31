package com.hbm.blocks.network.energy;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.CableBaseBlockEntity;
import com.hbm.blockentity.network.energy.EnergyNetworkBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code blocks/network/energy/WireCoated.java} (33 lines): full-cube {@code BlockContainer}
 * hosting {@code TileEntityCableBaseNT}. Not a thin {@link BlockCable} — CE renders MODEL and
 * conducts via the cable TE only.
 */
public class WireCoated extends Block implements EntityBlock {

    public WireCoated(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBaseBlockEntity(EnergyNetworkBlockEntities.CABLE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == EnergyNetworkBlockEntities.CABLE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
