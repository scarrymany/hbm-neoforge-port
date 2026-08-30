package com.hbm.blocks.machine.chem;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.chem.ChemIsotopeBlockEntities;
import com.hbm.blockentity.machine.chem.ElectrolyserBlockEntity;
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
 * Ported from CE's {@code MachineElectrolyser} (regname {@code machine_electrolyser}). CE's own
 * asymmetric footprint ({@code getDimensions() = {0,0,5,5,1,3}}, offset 5) and elaborate decorative
 * multi-{@code fillSpace} shell are preserved as far as the dimensions go; the decorative shell's
 * extra {@code fillSpace} calls (see CE's own override) are not reproduced - the standard
 * {@link BlockDummyable#fillSpace} rectangular-box fill is used instead, a cosmetic simplification
 * only (does not change the block's real footprint/collision).
 */
public class ElectrolyserBlock extends BlockDummyable {

    public ElectrolyserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 5, 5, 1, 3};
    }

    @Override
    public int getOffset() {
        return 5;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new ElectrolyserBlockEntity(ChemIsotopeBlockEntities.ELECTROLYSER.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ChemIsotopeBlockEntities.ELECTROLYSER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }
}
