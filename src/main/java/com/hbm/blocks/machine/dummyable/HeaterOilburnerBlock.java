package com.hbm.blocks.machine.dummyable;

import com.hbm.api.block.IToolable;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.HeaterOilburnerBlockEntity;
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
 * CE {@code HeaterOilburner} — Dummyable {1,0,1,1,1,1} offset 1 + 5 extras.
 * Screwdriver/hand-drill burn-rate Exact CE {@code HeaterOilburner.java:120-142}.
 */
public class HeaterOilburnerBlock extends BlockDummyable implements IToolable {

    public HeaterOilburnerBlock(Properties properties) {
        super(properties);
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
        return state.getValue(META) >= 12
                ? new HeaterOilburnerBlockEntity(DummyableProcessBlockEntities.HEATER_OILBURNER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.HEATER_OILBURNER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        makeExtra(level, core.east());
        makeExtra(level, core.west());
        makeExtra(level, core.north());
        makeExtra(level, core.south());
        makeExtra(level, core.above());
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER && tool != ToolType.HAND_DRILL)
            return false;
        if (world.isClientSide) return true;

        BlockPos core = findCore(world, new BlockPos(x, y, z));
        if (core == null) return false;
        if (!(world.getBlockEntity(core) instanceof HeaterOilburnerBlockEntity tile)) return false;

        // Exact CE HeaterOilburner.java:135-139
        if (tool == ToolType.SCREWDRIVER)
            tile.toggleSettingUp();
        else
            tile.toggleSettingDown();
        tile.setChanged();
        return true;
    }
}
