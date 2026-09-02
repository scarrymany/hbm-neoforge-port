package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Port of CE {@code com.hbm.blocks.machine.BlockCargoElevator} - a hydraulic cargo elevator platform.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/BlockCargoElevator.java (3x1x3 Dummyable)
 * <p>
 * TODO(CE): Dynamic height growth (CE :92-117), entity collision (CE :126-138), toggleElevator interaction (CE :119),
 * custom collision boxes (CE :208-219), custom highlight rendering (CE :162-187), dynamic drops based on height (CE :190-205).
 * Current port: static 3x1x3 multiblock, functional core, no lift mechanics.
 */
public class BlockCargoElevator extends BlockDummyable {

    public static final MapCodec<BlockCargoElevator> CODEC = simpleCodec(BlockCargoElevator::new);

    public BlockCargoElevator(Properties props) {
        super(props);
    }

    @Override
    protected @NotNull MapCodec<? extends BlockDummyable> codec() {
        return CODEC;
    }

    @Override
    public int[] getDimensions() {
        // CE getDimensions() returns {0, 0, 1, 1, 1, 1} for a 3x1x3 footprint (xMin, xMax, yMin, yMax, zMin, zMax)
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        // CE getOffset() = 1 (core at y=0)
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        // Only core (META >= 12) has a BlockEntity
        int meta = state.getValue(META);
        if (meta >= 12) {
            return new com.hbm.blockentity.machine.CargoElevatorBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof com.hbm.blockentity.machine.CargoElevatorBlockEntity elevator) {
                elevator.serverTick();
            }
        };
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        // CE uses ENTITYBLOCK_ANIMATED; for now MODEL is simpler (TESR deferred)
        return RenderShape.MODEL;
    }
}
