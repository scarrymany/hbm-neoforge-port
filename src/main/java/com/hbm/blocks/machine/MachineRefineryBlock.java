package com.hbm.blocks.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.oil.MachineRefineryBlockEntity;
import com.hbm.blockentity.machine.oil.OilChainBlockEntities;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ported from CE's {@code MachineRefinery} (regname {@code machine_refinery}, read in full).
 * Independent multiblock (unrelated to the three {@link com.hbm.blockentity.machine.oil.OilDrillBaseBlockEntity}
 * extractors, see {@link MachineRefineryBlockEntity}'s javadoc). {@link #getOffset()} = 1 (unlike
 * every other oil-chain block's 0) - CE's core sits one block back from the placement-click position,
 * confirmed by direct source reading, not a typo.
 */
public class MachineRefineryBlock extends BlockDummyable {

    public MachineRefineryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{8, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);

        int x = placedPos.getX(), y = placedPos.getY(), z = placedPos.getZ();
        makeExtra(level, new BlockPos(x - dir.getStepX() + 1, y, z - dir.getStepZ() + 1));
        makeExtra(level, new BlockPos(x - dir.getStepX() + 1, y, z - dir.getStepZ() - 1));
        makeExtra(level, new BlockPos(x - dir.getStepX() - 1, y, z - dir.getStepZ() + 1));
        makeExtra(level, new BlockPos(x - dir.getStepX() - 1, y, z - dir.getStepZ() - 1));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachineRefineryBlockEntity(OilChainBlockEntities.MACHINE_REFINERY.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == OilChainBlockEntities.MACHINE_REFINERY.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    /** See {@link MachineOilWellBlock#getDrops} - identical no-double-drop rationale. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            IPersistentNBT.breakBlock(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        IPersistentNBT.onBlockHarvested(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }
}
