package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.MachineReactorBreedingBlockEntity;
import com.hbm.blockentity.machine.PWRBlockEntities;
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
 * Ported from CE's {@code MachineReactorBreeding} (regname {@code machine_reactor_breeding}, read in
 * full). Per {@code docs/phase2/reactors_breeding_pwr.md}'s Key design decision, this - unlike
 * {@link MachinePWRControllerBlock} - fits {@code multiblock_framework.md}'s standard
 * {@link BlockDummyable} dummy-block pattern exactly: CE's own class already extends
 * {@code BlockDummyable} with a trivial 2-tall {@code getDimensions()}, matching the same pattern
 * this port's {@code MachineIndustrialTurbineBlock}/{@code CyclotronBlock} already use - no bespoke
 * multiblock code needed here at all.
 */
public class MachineReactorBreedingBlock extends BlockDummyable {

    public MachineReactorBreedingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineReactorBreedingBlockEntity(PWRBlockEntities.REACTOR_BREEDING.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PWRBlockEntities.REACTOR_BREEDING.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }
}
