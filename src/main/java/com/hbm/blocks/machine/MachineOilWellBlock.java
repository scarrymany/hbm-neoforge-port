package com.hbm.blocks.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.oil.MachineOilWellBlockEntity;
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
 * Ported from CE's {@code MachineOilWell} (regname {@code machine_well}, internal config name
 * literally {@code "derrick"} - see {@code docs/phase2/oil_production_chain.md}'s headline finding).
 * Cheapest of the four oil-chain multiblocks: no proxy dummy TE variant (dummy positions get no
 * block entity at all, matching CE's own asymmetry - see {@link OilChainBlockEntities}'s javadoc),
 * six chained {@link MultiblockHandlerXR} calls forming an irregular support-beam footprint that
 * {@link #getDimensions()} alone does not describe (transcribed value-for-value from CE per the
 * research report's own recommendation, not re-derived).
 */
public class MachineOilWellBlock extends BlockDummyable {

    public MachineOilWellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{9, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        int x = placedPos.getX(), y = placedPos.getY(), z = placedPos.getZ();
        return MultiblockHandlerXR.checkSpace(level, new BlockPos(x, y, z), new int[]{1, -1, 0, 0, 0, 0}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, new BlockPos(x, y + 1, z), new int[]{8, 0, 1, 1, 1, 1}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, new BlockPos(x + 1, y + 1, z + 1), new int[]{-1, 1, 0, 0, 0, 0}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, new BlockPos(x + 1, y + 1, z - 1), new int[]{-1, 1, 0, 0, 0, 0}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, new BlockPos(x - 1, y + 1, z + 1), new int[]{-1, 1, 0, 0, 0, 0}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, new BlockPos(x - 1, y + 1, z - 1), new int[]{-1, 1, 0, 0, 0, 0}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        int x = placedPos.getX(), y = placedPos.getY(), z = placedPos.getZ();
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x, y, z), new int[]{1, -1, 0, 0, 0, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x, y + 1, z), new int[]{8, 0, 1, 1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x + 1, y + 1, z + 1), new int[]{-1, 1, 0, 0, 0, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x + 1, y + 1, z - 1), new int[]{-1, 1, 0, 0, 0, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x - 1, y + 1, z + 1), new int[]{-1, 1, 0, 0, 0, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(level, new BlockPos(x - 1, y + 1, z - 1), new int[]{-1, 1, 0, 0, 0, 0}, this, dir);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachineOilWellBlockEntity(OilChainBlockEntities.MACHINE_OIL_WELL.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == OilChainBlockEntities.MACHINE_OIL_WELL.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    /**
     * CE's {@code dropBlockAsItemWithChance} no-op, matching CE exactly: this block's persistent
     * item drop is handled entirely by {@link com.hbm.blockentity.IPersistentNBT#breakBlock} (see
     * this class's own {@code onRemove}/{@code playerWillDestroy} overrides below), so the ordinary
     * loot-table {@code dropSelf} path (still generated for datagen
     * validation, see {@code ModBlockLootTableProvider}'s own javadoc precedent for
     * {@code BlockNTMOre}) must not additionally hand out a second, empty copy of this block.
     */
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
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        IPersistentNBT.onBlockHarvested(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
