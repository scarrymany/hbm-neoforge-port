package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blockentity.machine.fusion.WatzReactorBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's Watz reactor segment casing ({@code Watz}) - one 3-block-tall, 7x7-footprint
 * segment; multiple segments stack vertically to form the full reactor (see
 * {@link WatzReactorBlockEntity}'s javadoc for the multiblock-shape and content simplifications this
 * makes versus CE's three-sub-block-type lattice).
 */
public class WatzReactorBlock extends BlockDummyable {

    public WatzReactorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 3, 3, 3, 3};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new WatzReactorBlockEntity(FusionBlockEntities.WATZ_REACTOR.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == FusionBlockEntities.WATZ_REACTOR.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }
}
