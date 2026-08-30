package com.hbm.blockentity.network;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.blocks.network.FluidPipeAnchorBlock;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Long-distance, wrench-linked fluid pipe anchor, ported from CE's
 * {@code com.hbm.tileentity.network.TileEntityPipeAnchor}. Connects normally on exactly one face (the
 * opposite of the way it's facing - the "back" of the anchor block, matching CE's own
 * {@code ForgeDirection.getOrientation(getBlockMetadata()).getOpposite()}, ported here as the block's
 * {@link FluidPipeAnchorBlock#FACING} state property), plus any wrench-linked
 * {@link PipelineBaseBlockEntity#connected} positions.
 */
public class PipeAnchorBlockEntity extends PipelineBaseBlockEntity {

    public PipeAnchorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.SMALL;
    }

    @Override
    public Vec3 getMountPos() {
        return new Vec3(0.5, 0.5, 0.5);
    }

    @Override
    public double getMaxPipeLength() {
        return 10;
    }

    private Direction backFacing() {
        return getBlockState().getValue(FluidPipeAnchorBlock.FACING).getOpposite();
    }

    @Override
    public FluidNode createNode(FluidType type) {
        Direction dir = backFacing();
        FluidNode node = new FluidNode(type.getNetworkProvider(), worldPosition).setConnections(
                new DirPos(worldPosition, null),
                new DirPos(worldPosition.relative(dir), dir));
        for (BlockPos p : this.connected) node.addConnection(new DirPos(p, null));
        return node;
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return backFacing() == dir && type == this.type;
    }
}
