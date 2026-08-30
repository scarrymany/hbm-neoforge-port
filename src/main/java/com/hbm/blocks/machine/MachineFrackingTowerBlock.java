package com.hbm.blocks.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.oil.MachineFrackingTowerBlockEntity;
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
 * Ported from CE's {@code MachineFrackingTower} (regname {@code machine_fracking_tower}, read in
 * full) - by far the largest/most error-prone {@code checkRequirement}/{@code fillSpace} footprint
 * of the four oil-chain blocks (7 distinct offset shapes), transcribed value-for-value from CE per
 * the research report's own recommendation rather than re-derived. Like the pumpjack, non-core dummy
 * positions get no block entity in this port (CE's {@code TileEntityProxyCombo} not ported - see
 * {@code OilDrillBaseBlockEntity}'s javadoc).
 */
public class MachineFrackingTowerBlock extends BlockDummyable {

    public MachineFrackingTowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        int x = placedPos.getX(), y = placedPos.getY(), z = placedPos.getZ();

        if (!MultiblockHandlerXR.checkSpace(level, new BlockPos(x, y + 2, z), new int[]{1, 0, 3, 3, 3, 3}, placedPos, dir)) return false;

        if (!MultiblockHandlerXR.checkSpace(level, new BlockPos(x - 2, y + 2, z - 2), new int[]{-1, 2, 0, 1, 0, 1}, placedPos, Direction.NORTH)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, new BlockPos(x - 2, y + 2, z + 3), new int[]{-1, 2, 0, 1, 0, 1}, placedPos, Direction.NORTH)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, new BlockPos(x + 3, y + 2, z - 2), new int[]{-1, 2, 0, 1, 0, 1}, placedPos, Direction.NORTH)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, new BlockPos(x + 3, y + 2, z + 3), new int[]{-1, 2, 0, 1, 0, 1}, placedPos, Direction.NORTH)) return false;

        if (!MultiblockHandlerXR.checkSpace(level, new BlockPos(x, y, z), new int[]{10, -4, 2, 2, 2, 2}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, new BlockPos(x, y, z), new int[]{24, -9, 1, 1, 1, 1}, placedPos, dir)) return false;

        if (!MultiblockHandlerXR.checkSpace(level, new BlockPos(x, y + 15, z), new int[]{1, 0, 1, 1, -2, 3}, placedPos, dir)) return false;

        return super.checkRequirement(level, placedPos, dir, placementOffset);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        int x = placedPos.getX(), y = placedPos.getY(), z = placedPos.getZ();

        MultiblockHandlerXR.fillSpace(level, new BlockPos(x, y, z), getDimensions(), this, dir);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x, y + 2, z), new int[]{1, 0, 3, 3, 3, 3}, this, dir);

        MultiblockHandlerXR.fillSpace(level, new BlockPos(x - 2, y + 2, z - 2), new int[]{-1, 2, 0, 1, 0, 1}, this, Direction.NORTH);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x - 2, y + 2, z + 3), new int[]{-1, 2, 0, 1, 0, 1}, this, Direction.NORTH);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x + 3, y + 2, z - 2), new int[]{-1, 2, 0, 1, 0, 1}, this, Direction.NORTH);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x + 3, y + 2, z + 3), new int[]{-1, 2, 0, 1, 0, 1}, this, Direction.NORTH);

        MultiblockHandlerXR.fillSpace(level, new BlockPos(x, y, z), new int[]{10, -4, 2, 2, 2, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x, y, z), new int[]{24, -9, 1, 1, 1, 1}, this, dir);

        MultiblockHandlerXR.fillSpace(level, new BlockPos(x, y + 15, z), new int[]{1, 0, 1, 1, -2, 3}, this, Direction.WEST);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachineFrackingTowerBlockEntity(OilChainBlockEntities.MACHINE_FRACKING_TOWER.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == OilChainBlockEntities.MACHINE_FRACKING_TOWER.get() ? ITickableBE.ticker() : null;
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
