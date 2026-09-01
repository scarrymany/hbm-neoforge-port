package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.SealHatchBlockEntity;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

/**
 * CE {@code BlockSeal} ({@code BlockSeal.java}). No own TE. Horizontal FACING + ACTIVATED.
 * Click / rising-edge RS / {@link IBomb#explode} toggle a {@code seal_frame} square (size 1–6).
 */
public class BlockSeal extends Block implements IBomb {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");

    public BlockSeal(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVATED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ACTIVATED, false);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int size = getFrameSize(level, pos);
        if (size != 0) {
            if (isSealClosed(level, pos, size)) openSeal(level, pos, size);
            else closeSeal(level, pos, size);
        }
        return InteractionResult.CONSUME;
    }

    public static int getFrameSize(Level world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() != SealBlocks.SEAL_CONTROLLER.get()) return 0;
        int max = 7;

        for (int size = 1; size < max; size++) {
            boolean valid = true;
            int xOff = 0;
            int zOff = 0;
            Direction facing = world.getBlockState(pos).getValue(FACING);
            if (facing == Direction.SOUTH) zOff -= size;
            if (facing == Direction.NORTH) zOff += size;
            if (facing == Direction.EAST) xOff -= size;
            if (facing == Direction.WEST) xOff += size;

            for (int x = pos.getX() - size; x <= pos.getX() + size; x++) {
                if (!isFrameOrController(world, new BlockPos(x + xOff, pos.getY(), pos.getZ() + size + zOff))) {
                    valid = false;
                }
            }
            for (int x = pos.getX() - size; x <= pos.getX() + size; x++) {
                if (!isFrameOrController(world, new BlockPos(x + xOff, pos.getY(), pos.getZ() - size + zOff))) {
                    valid = false;
                }
            }
            for (int z = pos.getZ() - size; z <= pos.getZ() + size; z++) {
                if (!isFrameOrController(world, new BlockPos(pos.getX() - size + xOff, pos.getY(), z + zOff))) {
                    valid = false;
                }
            }
            for (int z = pos.getZ() - size; z <= pos.getZ() + size; z++) {
                if (!isFrameOrController(world, new BlockPos(pos.getX() + size + xOff, pos.getY(), z + zOff))) {
                    valid = false;
                }
            }

            if (valid) return size;
        }

        return 0;
    }

    private static boolean isFrameOrController(Level world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return block == SealBlocks.SEAL_FRAME.get() || block == SealBlocks.SEAL_CONTROLLER.get();
    }

    public static void closeSeal(Level world, BlockPos pos, int size) {
        int xOff = 0;
        int zOff = 0;
        Direction facing = world.getBlockState(pos).getValue(FACING);
        if (facing == Direction.SOUTH) zOff -= size;
        if (facing == Direction.NORTH) zOff += size;
        if (facing == Direction.EAST) xOff -= size;
        if (facing == Direction.WEST) xOff += size;

        for (int x = pos.getX() - size + 1; x <= pos.getX() + size - 1; x++) {
            for (int z = pos.getZ() - size + 1; z <= pos.getZ() + size - 1; z++) {
                BlockPos hatchPos = new BlockPos(x + xOff, pos.getY(), z + zOff);
                if (world.getBlockState(hatchPos).getBlock() == Blocks.AIR && !world.isClientSide) {
                    world.setBlock(hatchPos, SealBlocks.SEAL_HATCH.get().defaultBlockState(), 3);
                    if (world.getBlockEntity(hatchPos) instanceof SealHatchBlockEntity hatch) {
                        hatch.setControllerPos(pos);
                    }
                }
            }
        }
    }

    public static void openSeal(Level world, BlockPos pos, int size) {
        int xOff = 0;
        int zOff = 0;
        Direction facing = world.getBlockState(pos).getValue(FACING);
        if (facing == Direction.SOUTH) zOff -= size;
        if (facing == Direction.NORTH) zOff += size;
        if (facing == Direction.EAST) xOff -= size;
        if (facing == Direction.WEST) xOff += size;

        for (int x = pos.getX() - size + 1; x <= pos.getX() + size - 1; x++) {
            for (int z = pos.getZ() - size + 1; z <= pos.getZ() + size - 1; z++) {
                BlockPos hatchPos = new BlockPos(x + xOff, pos.getY(), z + zOff);
                if (world.getBlockState(hatchPos).getBlock() == SealBlocks.SEAL_HATCH.get() && !world.isClientSide) {
                    world.setBlock(hatchPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    public static boolean isSealClosed(Level world, BlockPos pos, int size) {
        int xOff = 0;
        int zOff = 0;
        Direction facing = world.getBlockState(pos).getValue(FACING);
        if (facing == Direction.SOUTH) zOff -= size;
        if (facing == Direction.NORTH) zOff += size;
        if (facing == Direction.EAST) xOff -= size;
        if (facing == Direction.WEST) xOff += size;

        for (int x = pos.getX() - size + 1; x <= pos.getX() + size - 1; x++) {
            for (int z = pos.getZ() - size + 1; z <= pos.getZ() + size - 1; z++) {
                if (world.getBlockState(new BlockPos(x + xOff, pos.getY(), z + zOff)).getBlock() == SealBlocks.SEAL_HATCH.get()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public BombReturnCode explode(Level world, BlockPos pos, Entity detonator) {
        if (!world.isClientSide) {
            int size = getFrameSize(world, pos);
            if (size != 0) {
                if (isSealClosed(world, pos, size)) openSeal(world, pos, size);
                else closeSeal(world, pos, size);
                return BombReturnCode.TRIGGERED;
            }
            return BombReturnCode.ERROR_INCOMPATIBLE;
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean movedByPiston) {
        if (world.hasNeighborSignal(pos)) {
            if (!world.getBlockState(pos).getValue(ACTIVATED)) {
                world.setBlock(pos, world.getBlockState(pos).setValue(ACTIVATED, true), 2);
                int size = getFrameSize(world, pos);
                if (size != 0) {
                    if (isSealClosed(world, pos, size)) openSeal(world, pos, size);
                    else closeSeal(world, pos, size);
                }
            }
        } else if (world.getBlockState(pos).getValue(ACTIVATED)) {
            world.setBlock(pos, world.getBlockState(pos).setValue(ACTIVATED, false), 2);
        }
    }
}
