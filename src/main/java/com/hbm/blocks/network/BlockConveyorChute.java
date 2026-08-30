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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Ported from CE's {@code com.hbm.blocks.network.BlockConveyorChute} (read in full). Vertical
 * drop-chute; a 3-state {@code TYPE} property (bottom/middle/input) is recomputed on
 * {@link #neighborChanged} exactly like CE. Metadata plumbing dropped, same rationale as
 * {@link BlockConveyorBase}.
 */
public class BlockConveyorChute extends BlockConveyorBase {

    /** Bottom 0, Middle 1, Input 2. */
    public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, 2);
    private static final VoxelShape SHAPE = Shapes.block();

    public BlockConveyorChute(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, 0));
    }

    @Override
    public Vec3 getTravelLocation(Level world, int x, int y, int z, Vec3 itemPos, double speed) {
        BlockPos pos = new BlockPos(x, y, z);
        Block belowBlock = world.getBlockState(pos.below()).getBlock();

        if (belowBlock instanceof IConveyorBelt || belowBlock instanceof IEnterableBlock) {
            speed *= 5;
        } else if (itemPos.y > pos.getY() + 0.25) {
            speed *= 3;
        }

        return super.getTravelLocation(world, x, y, z, itemPos, speed);
    }

    @Override
    public Direction getInputDirection(Level world, BlockPos pos) {
        return world.getBlockState(pos).getValue(FACING);
    }

    @Override
    public Direction getOutputDirection(Level world, BlockPos pos) {
        return Direction.DOWN;
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level world, BlockPos pos, Vec3 itemPos) {
        Block below = world.getBlockState(pos.below()).getBlock();
        if (below instanceof IConveyorBelt || below instanceof IEnterableBlock || itemPos.y > pos.getY() + 0.25) {
            return new Vec3(pos.getX() + 0.5, itemPos.y, pos.getZ() + 0.5);
        } else {
            return super.getClosestSnappingPosition(world, pos, itemPos);
        }
    }

    @Override
    public Direction getTravelDirection(Level world, BlockPos pos, Vec3 itemPos) {
        Block belowBlock = world.getBlockState(pos.below()).getBlock();

        if (belowBlock instanceof IConveyorBelt || belowBlock instanceof IEnterableBlock || itemPos.y > pos.getY() + 0.25) {
            return Direction.UP;
        }

        return world.getBlockState(pos).getValue(FACING);
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
            BlockState conveyorState = ConveyorBlocks.CONVEYOR.get().defaultBlockState()
                    .setValue(FACING, state.getValue(FACING))
                    .setValue(BlockConveyorBendable.CURVE, BlockConveyorBendable.CurveType.STRAIGHT);
            world.setBlock(pos, conveyorState, 3);
        }
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, facing).setValue(TYPE, getUpdatedType(context.getLevel(), context.getClickedPos(), facing));
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, world, pos, blockIn, fromPos, movedByPiston);
        if (!world.isClientSide) {
            world.setBlock(pos, state.setValue(TYPE, getUpdatedType(world, pos)), 3);
        }
    }

    public int getUpdatedType(Level world, BlockPos pos) {
        return getUpdatedType(world, pos, world.getBlockState(pos).getValue(FACING));
    }

    public int getUpdatedType(Level world, BlockPos pos, Direction facing) {
        boolean hasChuteBelow = world.getBlockState(pos.below()).getBlock() instanceof BlockConveyorChute;
        Block inputBlock = world.getBlockState(pos.relative(facing)).getBlock();
        boolean hasInputBelt = (inputBlock instanceof IConveyorBelt || inputBlock instanceof IEnterableBlock);
        if (hasChuteBelow) return hasInputBelt ? 2 : 1;
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TYPE);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
