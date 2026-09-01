package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineCompressorCompactBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * CE {@code MachineCompressorCompact} — Dummyable {2,0,1,1,3,3} offset 1 + 6 extras.
 * TODO(CE: MachineCompressorCompact.java:26): TileEntityProxyCombo(false,true,true) on extras.
 * TODO(CE: RenderCompressorCompact.java:1): TESR.
 */
public class MachineCompressorCompactBlock extends BlockDummyable {

    public MachineCompressorCompactBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 3, 3};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineCompressorCompactBlockEntity(DummyableProcessBlockEntities.MACHINE_COMPRESSOR_COMPACT.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_COMPRESSOR_COMPACT.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(rot, 3).above());
        makeExtra(level, core.relative(rot, -3).above());
        makeExtra(level, core.relative(dir).relative(rot).above());
        makeExtra(level, core.relative(dir).relative(rot.getOpposite()).above());
        makeExtra(level, core.relative(dir.getOpposite()).relative(rot).above());
        makeExtra(level, core.relative(dir.getOpposite()).relative(rot.getOpposite()).above());
    }
}
