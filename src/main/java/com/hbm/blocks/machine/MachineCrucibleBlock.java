package com.hbm.blocks.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.CrucibleBlockEntities;
import com.hbm.blockentity.machine.MachineCrucibleBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.material.Mats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code com.hbm.blocks.machine.MachineCrucible} (199 lines, read in full).
 * {@code getDimensions()}/{@code getOffset()}, the 5 hand-authored basin+rim {@code AxisAlignedBB}s
 * ({@link BlockDummyable#bounding} - the base class's {@code getShape}/highlight-draw logic already
 * handles rendering/collision from this list, no override needed here), and the shovel-scoop /
 * {@link ICrucibleAcceptor} delegation to the core block entity are all ported. The
 * meta&gt;=12-only-gets-a-real-block-entity split follows
 * {@link com.hbm.blocks.machine.chem.ElectrolyserBlock}'s established (simpler than CE's
 * {@code TileEntityProxyCombo}) pattern of returning {@code null} for every non-core position.
 * <p>
 * <b>Real behavior correction vs. this task's own research report</b>: a direct read of CE's
 * {@code onBlockActivated} shows the shovel-scoop fires when the player is <i>not</i> sneaking and
 * holding a shovel (nested inside {@code if(!player.isSneaking())}) - sneaking always falls through
 * to {@code standardOpenBehavior}, which itself no-ops on a sneaking click and only opens the GUI
 * when not sneaking. Ported exactly as read from CE source (not the inverted "sneak + shovel"
 * description the research report's own prose used).
 */
public class MachineCrucibleBlock extends BlockDummyable implements ICrucibleAcceptor {

    public MachineCrucibleBlock(Properties properties) {
        super(properties);

        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, 1.5D, 0.5D, 1.5D));
        this.bounding.add(new AABB(-1.25D, 0.5D, -1.25D, 1.25D, 1.5D, -1D));
        this.bounding.add(new AABB(-1.25D, 0.5D, -1.25D, -1D, 1.5D, 1.25D));
        this.bounding.add(new AABB(-1.25D, 0.5D, 1D, 1.25D, 1.5D, 1.25D));
        this.bounding.add(new AABB(1D, 0.5D, -1.25D, 1.25D, 1.5D, 1.25D));
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachineCrucibleBlockEntity(CrucibleBlockEntities.MACHINE_CRUCIBLE.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == CrucibleBlockEntities.MACHINE_CRUCIBLE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && held.is(ItemTags.SHOVELS)) {
                if (level.isClientSide) return InteractionResult.SUCCESS;

                BlockPos corePos = findCore(level, pos);
                if (corePos == null) return InteractionResult.FAIL;
                if (level.getBlockEntity(corePos) instanceof MachineCrucibleBlockEntity crucible) {
                    crucible.scoopOut(player);
                }
                return InteractionResult.CONSUME;
            }
        }

        return standardOpenBehavior(level, pos, player);
    }

    /** CE: {@code MachineCrucible.breakBlock} - dumps the crucible's melt/waste pools as scrap items before the multiblock actually comes apart. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof MachineCrucibleBlockEntity crucible) {
            crucible.dropAllAsScraps();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        BlockPos corePos = findCore(level, pos);
        return corePos != null && level.getBlockEntity(corePos) instanceof MachineCrucibleBlockEntity crucible
                && crucible.canAcceptPartialPour(level, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        BlockPos corePos = findCore(level, pos);
        if (corePos == null) return stack;
        return level.getBlockEntity(corePos) instanceof MachineCrucibleBlockEntity crucible
                ? crucible.pour(level, pos, dX, dY, dZ, side, stack)
                : stack;
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return false;
    }

    @Override
    public Mats.MaterialStack flow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return null;
    }
}
