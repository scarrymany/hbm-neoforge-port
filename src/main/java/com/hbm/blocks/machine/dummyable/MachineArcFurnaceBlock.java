package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineArcFurnaceBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
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

/** CE {@code MachineArcFurnaceLarge} — Dummyable {4,0,2,2,2,2} offset 2 + XR {4,0,3,-2,1,1} + 6 extras. */
public class MachineArcFurnaceBlock extends BlockDummyable {

    public MachineArcFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineArcFurnaceBlockEntity(DummyableProcessBlockEntities.MACHINE_ARC_FURNACE.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_ARC_FURNACE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        if (!super.checkRequirement(level, placedPos, dir, placementOffset)) return false;
        BlockPos core = placedPos.relative(dir, placementOffset);
        return MultiblockHandlerXR.checkSpace(level, core, new int[]{4, 0, 3, -2, 1, 1}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, 0, 3, -2, 1, 1}, this, dir);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(dir, 2).relative(rot));
        makeExtra(level, core.relative(dir, 2).relative(rot.getOpposite()));
        makeExtra(level, core.relative(rot, 2).relative(dir));
        makeExtra(level, core.relative(rot, 2).relative(dir.getOpposite()));
        makeExtra(level, core.relative(rot.getOpposite(), 2).relative(dir));
        makeExtra(level, core.relative(rot.getOpposite(), 2).relative(dir.getOpposite()));
    }
}
