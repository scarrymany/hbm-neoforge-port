package com.hbm.blocks.network;

import com.hbm.api.block.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Ported from CE's {@code com.hbm.blocks.network.BlockConveyorBendable} (read in full). Adds the
 * {@code CURVE} property (CE's {@code PropertyEnum<CurveType>} -> {@link EnumProperty}) for the
 * left/right-bending conveyor variant. Metadata plumbing dropped, same rationale as
 * {@link BlockConveyorBase}.
 */
public class BlockConveyorBendable extends BlockConveyorBase implements IToolable {

    public static final EnumProperty<CurveType> CURVE = EnumProperty.create("curve", CurveType.class);

    public BlockConveyorBendable(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CURVE, CurveType.STRAIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CURVE);
    }

    @Override
    public Direction getInputDirection(Level world, BlockPos pos) {
        return world.getBlockState(pos).getValue(FACING);
    }

    @Override
    public Direction getOutputDirection(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        CurveType curve = state.getValue(CURVE);
        Direction primaryOutput = state.getValue(FACING).getOpposite();
        if (curve == CurveType.RIGHT) return primaryOutput.getClockWise();
        if (curve == CurveType.LEFT) return primaryOutput.getCounterClockWise();
        return primaryOutput;
    }

    @Override
    public Direction getTravelDirection(Level world, BlockPos pos, Vec3 itemPos) {
        BlockState state = world.getBlockState(pos);
        CurveType curve = state.getValue(CURVE);

        if (curve == CurveType.STRAIGHT) {
            return super.getTravelDirection(world, pos, itemPos);
        }

        Direction primary = state.getValue(FACING);
        int dir = (curve == CurveType.LEFT) ? 0 : 1;

        double ix = pos.getX() + 0.5;
        double iz = pos.getZ() + 0.5;
        Direction secondary = primary.getClockWise();

        ix -= -primary.getStepX() * 0.5 + secondary.getStepX() * (0.5 - dir);
        iz -= -primary.getStepZ() * 0.5 + secondary.getStepZ() * (0.5 - dir);

        double dX = Math.abs(itemPos.x - ix);
        double dZ = Math.abs(itemPos.z - iz);
        if (dX + dZ >= 1) {
            if (curve == CurveType.LEFT) {
                return secondary.getOpposite();
            } else {
                return secondary;
            }
        }
        return primary;
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ, InteractionHand hand,
                            IToolable.ToolType tool) {
        if (tool != IToolable.ToolType.SCREWDRIVER) {
            return false;
        }

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);

        if (!player.isShiftKeyDown()) {
            // Rotate the block clockwise
            world.setBlock(pos, state.rotate(Rotation.CLOCKWISE_90), 3);
        } else {
            // Cycle through curve types: STRAIGHT -> LEFT -> RIGHT -> STRAIGHT
            CurveType curve = state.getValue(CURVE);
            int nextOrdinal = (curve.ordinal() + 1) % CurveType.VALUES.length;
            CurveType newCurve = CurveType.VALUES[nextOrdinal];
            world.setBlock(pos, state.setValue(CURVE, newCurve), 3);
        }

        return true;
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirrorIn) {
        BlockState mirroredState = state.setValue(FACING, mirrorIn.mirror(state.getValue(FACING)));
        CurveType curve = mirroredState.getValue(CURVE);
        if (curve == CurveType.LEFT) {
            mirroredState = mirroredState.setValue(CURVE, CurveType.RIGHT);
        } else if (curve == CurveType.RIGHT) {
            mirroredState = mirroredState.setValue(CURVE, CurveType.LEFT);
        }
        return mirroredState;
    }

    /** CE's curve-type enum, {@code PropertyEnum} -> {@link EnumProperty} verbatim. */
    public enum CurveType implements StringRepresentable {
        STRAIGHT("straight"),
        LEFT("left"),
        RIGHT("right");

        public static final CurveType[] VALUES = values();

        private final String name;

        CurveType(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
