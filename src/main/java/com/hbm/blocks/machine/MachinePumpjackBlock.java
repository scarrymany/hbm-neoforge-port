package com.hbm.blocks.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.oil.MachinePumpjackBlockEntity;
import com.hbm.blockentity.machine.oil.OilChainBlockEntities;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
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
 * Ported from CE's {@code MachinePumpjack} (regname {@code machine_pumpjack}, read in full). Unlike
 * the derrick, non-core dummy positions ({@code meta} 6-11) get no block entity either in this port
 * (CE uses {@code TileEntityProxyCombo} there - not ported, see
 * {@code OilDrillBaseBlockEntity}'s javadoc "shell now" TODO for the follow-up).
 */
public class MachinePumpjackBlock extends BlockDummyable {

    public MachinePumpjackBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 0, 0, 6};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        if (!super.checkRequirement(level, placedPos, dir, placementOffset)) return false;

        BlockPos corePos = placedPos.relative(dir, placementOffset);
        return MultiblockHandlerXR.checkSpace(level, corePos, new int[]{0, 0, -1, 1, -2, 4}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, corePos, new int[]{0, 0, 1, -1, -1, 5}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);

        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = placedPos.getX(), y = placedPos.getY(), z = placedPos.getZ();

        MultiblockHandlerXR.fillSpace(level, new BlockPos(x + rot.getStepX() * 3, y, z + rot.getStepZ() * 3), new int[]{0, 0, -1, 1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x + rot.getStepX() * 3, y, z + rot.getStepZ() * 3), new int[]{0, 0, 1, -1, 2, 2}, this, dir);

        makeExtra(level, new BlockPos(x + rot.getStepX() * 3 + 1, y, z + rot.getStepZ() * 3 + 1));
        makeExtra(level, new BlockPos(x + rot.getStepX() * 3 + 1, y, z + rot.getStepZ() * 3 - 1));
        makeExtra(level, new BlockPos(x + rot.getStepX() * 3 - 1, y, z + rot.getStepZ() * 3 + 1));
        makeExtra(level, new BlockPos(x + rot.getStepX() * 3 - 1, y, z + rot.getStepZ() * 3 - 1));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachinePumpjackBlockEntity(OilChainBlockEntities.MACHINE_PUMPJACK.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == OilChainBlockEntities.MACHINE_PUMPJACK.get() ? ITickableBE.ticker() : null;
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
