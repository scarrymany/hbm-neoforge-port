package com.hbm.blocks.machine.dummyable;

import com.hbm.api.block.IToolable;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineEPressBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
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
 * CE {@code MachineEPress} — Dummyable {2,0,0,0,0,0} offset 0, same stack as burner press.
 * Hand-drill dummy {@code safeRem} Exact CE {@code MachineEPress.java:71-83}.
 */
public class MachineEPressBlock extends BlockDummyable implements IToolable {

    public MachineEPressBlock(Properties properties) {
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
                ? new MachineEPressBlockEntity(DummyableProcessBlockEntities.MACHINE_EPRESS.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_EPRESS.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.HAND_DRILL) return false;
        BlockPos pos = new BlockPos(x, y, z);
        int meta = world.getBlockState(pos).getValue(META);
        if (meta >= 12) return false;
        // Exact CE MachineEPress.java:79-82
        safeRem = true;
        world.removeBlock(pos, false);
        safeRem = false;
        return true;
    }
}
