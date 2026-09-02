package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.TapeDriveBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Port of CE {@code com.hbm.blocks.machine.MachineTapeDrive} - a satellite data tape drive
 * that connects to {@code machine_satlink} to record satellite scan data onto drive items.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/MachineTapeDrive.java
 */
public class MachineTapeDrive extends BlockMachineBase {

    public static final MapCodec<MachineTapeDrive> CODEC = simpleCodec(MachineTapeDrive::new);

    private static final VoxelShape SHAPE_FULL = Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 12, 16, 16);
    private static final VoxelShape SHAPE_WEST = Block.box(4, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 12);
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 4, 16, 16, 16);

    public MachineTapeDrive(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean shouldBeWaterloggable() {
        return false;
    }

    @Override
    protected boolean usesHorizontalFacing() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TapeDriveBlockEntity(pos, state);
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        Direction facing = getFacing(state);
        if (facing == null) return SHAPE_FULL;

        return switch (facing) {
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case SOUTH -> SHAPE_SOUTH;
            case NORTH -> SHAPE_NORTH;
            default -> SHAPE_FULL;
        };
    }
}
