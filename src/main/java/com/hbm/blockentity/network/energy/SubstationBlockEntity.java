package com.hbm.blockentity.network.energy;

import com.hbm.api.energymk2.Nodespace;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.tileentity.network.energy.TileEntitySubstation} (read in full):
 * the core-only tile entity of {@link com.hbm.blocks.network.energy.SubstationBlock}, overriding
 * {@link #createNode()} to register 5 {@link BlockPos} cells - the core plus its 4 diagonal
 * "extra"-flagged corner cells (see {@code SubstationBlock#fillSpace}, which places a
 * {@link ProxyConductorBlockEntity} at each) - as one single logical node
 * ({@link com.hbm.uninos.GenNode}'s multi-position support: {@code UniNodeWorld.pushNode} registers
 * the node at every position in its {@code positions} array). A corner's own
 * {@link ProxyConductorBlockEntity} never actually needs to create a separate node of its own: by the
 * time it ticks, {@code Nodespace.getNode(level, cornerPos)} already resolves to this substation
 * node, since this node claims that exact position too - matching CE's own mechanism exactly (its
 * {@code TileEntityProxyConductor} has no special-case code either).
 */
public class SubstationBlockEntity extends PylonBaseBlockEntity {

    public SubstationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.QUAD;
    }

    @Override
    public Vec3[] getMountPos() {
        double topOff = 5.25D;
        BlockState state = getBlockState();
        int meta = state.hasProperty(BlockDummyable.META) ? state.getValue(BlockDummyable.META) - BlockDummyable.offset : 0;

        double angle = (meta == 4 || meta == 5) ? Math.PI * 0.5D : 0D;
        Vec3 vec = rotateY(1D, 0D, angle);

        return new Vec3[]{
                new Vec3(0.5D + vec.x * 0.5D, topOff, 0.5D + vec.z * 0.5D),
                new Vec3(0.5D + vec.x * 1.5D, topOff, 0.5D + vec.z * 1.5D),
                new Vec3(0.5D - vec.x * 0.5D, topOff, 0.5D - vec.z * 0.5D),
                new Vec3(0.5D - vec.x * 1.5D, topOff, 0.5D - vec.z * 1.5D),
        };
    }

    @Override
    public Vec3 getConnectionPoint() {
        return new Vec3(worldPosition.getX() + 0.5D, worldPosition.getY() + 5.25D, worldPosition.getZ() + 0.5D);
    }

    @Override
    public double getMaxWireLength() {
        return 20D;
    }

    @Override
    public Nodespace.PowerNode createNode() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        Nodespace.PowerNode node = new Nodespace.PowerNode(
                worldPosition,
                new BlockPos(x + 1, y, z + 1),
                new BlockPos(x + 1, y, z - 1),
                new BlockPos(x - 1, y, z + 1),
                new BlockPos(x - 1, y, z - 1)
        ).setConnections(
                new DirPos(x + 2, y, z - 1, Direction.EAST),
                new DirPos(x + 2, y, z + 1, Direction.EAST),
                new DirPos(x - 2, y, z - 1, Direction.WEST),
                new DirPos(x - 2, y, z + 1, Direction.WEST),
                new DirPos(x - 1, y, z + 2, Direction.SOUTH),
                new DirPos(x + 1, y, z + 2, Direction.SOUTH),
                new DirPos(x - 1, y, z - 2, Direction.NORTH),
                new DirPos(x + 1, y, z - 2, Direction.NORTH)
        );
        for (BlockPos p : connected) node.addConnection(new DirPos(p, null));
        return node;
    }
}
