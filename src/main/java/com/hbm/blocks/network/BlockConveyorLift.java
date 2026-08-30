package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IEnterableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Ported from CE's {@code com.hbm.blocks.network.BlockConveyorLift} (read in full). Vertical
 * conveyor lift; reuses {@link BlockConveyorChute}'s {@code TYPE} property semantics for stacking
 * lift segments, with a shorter top-segment bounding box.
 */
public class BlockConveyorLift extends BlockConveyorChute {

    // The top exit segment (TYPE 2) only models up to Y=0.5, so its collision box must match.
    private static final VoxelShape TOP_SHAPE = Shapes.box(0, 0, 0, 1, 0.5, 1);

    public BlockConveyorLift(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext context) {
        if (state.getValue(TYPE) == 2) return TOP_SHAPE;
        return super.getShape(state, source, pos, context);
    }

    @Override
    public Direction getInputDirection(Level world, BlockPos pos) {
        return world.getBlockState(pos).getValue(FACING);
    }

    @Override
    public Direction getOutputDirection(Level world, BlockPos pos) {
        return Direction.UP;
    }

    @Override
    public Direction getTravelDirection(Level world, BlockPos pos, Vec3 itemPos) {
        BlockState state = world.getBlockState(pos);
        Block blockAbove = world.getBlockState(pos.above()).getBlock();
        boolean isTop = !(blockAbove instanceof BlockConveyorLift) && !(blockAbove instanceof IEnterableBlock);

        if (isTop) {
            return state.getValue(FACING);
        } else {
            return Direction.DOWN;
        }
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level world, BlockPos pos, Vec3 itemPos) {
        Direction travelDirection = getTravelDirection(world, pos, itemPos);

        if (travelDirection.getAxis() == Direction.Axis.Y) {
            return new Vec3(pos.getX() + 0.5, itemPos.y, pos.getZ() + 0.5);
        } else {
            return super.getClosestSnappingPosition(world, pos, itemPos);
        }
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ, InteractionHand hand,
                            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) {
            return false;
        }

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);

        if (!player.isShiftKeyDown()) {
            world.setBlock(pos, state.rotate(Rotation.CLOCKWISE_90), 3);
        } else {
            BlockState chuteState = ConveyorBlocks.CONVEYOR_CHUTE.get().defaultBlockState().setValue(FACING, state.getValue(FACING));
            world.setBlock(pos, chuteState, 3);
        }
        return true;
    }

    @Override
    public int getUpdatedType(Level world, BlockPos pos, Direction facing) {
        boolean hasLiftBelow = world.getBlockState(pos.below()).getBlock() instanceof BlockConveyorLift;
        boolean hasLiftAbove = world.getBlockState(pos.above()).getBlock() instanceof BlockConveyorLift;
        if (!hasLiftBelow) {
            Block inputBlock = world.getBlockState(pos.relative(facing.getOpposite())).getBlock();
            boolean isFed = (inputBlock instanceof IConveyorBelt || inputBlock instanceof IEnterableBlock);
            return isFed ? 2 : 0;
        }
        return hasLiftAbove ? 1 : 2;
    }
}
