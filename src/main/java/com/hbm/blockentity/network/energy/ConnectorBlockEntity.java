package com.hbm.blockentity.network.energy;

import com.hbm.api.energymk2.Nodespace;
import com.hbm.blocks.network.energy.ConnectorRedWireBlock;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.tileentity.network.TileEntityConnector} (read full) - single-face
 * energy connector with 10m range. Extends {@link PylonBaseBlockEntity} (which extends
 * {@link CableBaseBlockEntity}), so it's both a pylon-link node and a standard HE conductor.
 */
public class ConnectorBlockEntity extends PylonBaseBlockEntity {

    public ConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(EnergyNetworkBlockEntities.CONNECTOR.get(), pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.SINGLE;
    }

    @Override
    public Vec3[] getMountPos() {
        return new Vec3[]{new Vec3(0.5, 0.5, 0.5)};
    }

    @Override
    public double getMaxWireLength() {
        return 10;
    }

    @Override
    public Nodespace.PowerNode createNode() {
        Direction dir = getBlockState().getValue(ConnectorRedWireBlock.FACING).getOpposite();
        Nodespace.PowerNode node = new Nodespace.PowerNode(getBlockPos()).setConnections(
                new DirPos(getBlockPos(), null),
                new DirPos(getBlockPos().relative(dir), dir));
        for (BlockPos pos : this.connected) {
            node.addConnection(new DirPos(pos, null));
        }
        return node;
    }

    @Override
    public boolean canConnect(Direction dir) {
        return getBlockState().getValue(ConnectorRedWireBlock.FACING).getOpposite() == dir;
    }
}
