package com.hbm.blockentity.network.energy;

import com.hbm.api.energymk2.Nodespace;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.tileentity.network.energy.TileEntityPylon} (single-block pylon,
 * pairs with {@link com.hbm.blocks.network.energy.PylonRedWireBlock}, read in full). Unlike the
 * multiblock pylon tiers, this one also joins the normal 6-way cable network (CE's own
 * {@code createNode} override adds the standard neighbor stubs on top of the pylon links) - a single
 * pylon block can be fed directly by an adjacent cable.
 */
public class PylonBlockEntity extends PylonBaseBlockEntity {

    public PylonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.SINGLE;
    }

    @Override
    public Vec3[] getMountPos() {
        return new Vec3[]{new Vec3(0.5D, 5.4D, 0.5D)};
    }

    @Override
    public double getMaxWireLength() {
        return 25D;
    }

    @Override
    public Nodespace.PowerNode createNode() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        Nodespace.PowerNode node = new Nodespace.PowerNode(worldPosition).setConnections(
                new DirPos(x + 1, y, z, Direction.EAST),
                new DirPos(x - 1, y, z, Direction.WEST),
                new DirPos(x, y + 1, z, Direction.UP),
                new DirPos(x, y - 1, z, Direction.DOWN),
                new DirPos(x, y, z + 1, Direction.SOUTH),
                new DirPos(x, y, z - 1, Direction.NORTH)
        );
        for (BlockPos p : connected) node.addConnection(new DirPos(p, null));
        return node;
    }
}
