package com.hbm.blockentity.network.energy;

import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.tileentity.network.energy.TileEntityPylonLarge} (read in full):
 * the core-only block entity of the {@link com.hbm.blocks.network.energy.PylonLargeBlock} multiblock
 * ({@code ConnectionType.QUAD}, 4 mount points, rotated by the core's own {@link BlockDummyable#META}
 * facing). Does not override {@link #createNode()} - CE's own {@code TileEntityPylonLarge} does not
 * either, relying on {@link PylonBaseBlockEntity}'s default (pylon links only, no 6-way cable
 * adjacency) since this multiblock's dummy footprint has no cable-receptacle positions.
 */
public class PylonLargeBlockEntity extends PylonBaseBlockEntity {

    public PylonLargeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.QUAD;
    }

    @Override
    public Vec3[] getMountPos() {
        double topOff = 0.75D + 0.0625D;
        double sideOff = 3.375D;

        BlockState state = getBlockState();
        int meta = state.hasProperty(BlockDummyable.META) ? state.getValue(BlockDummyable.META) - BlockDummyable.offset : 0;

        double angle = switch (meta) {
            case 4 -> Math.PI * 0.25D;
            case 3 -> Math.PI * 0.5D;
            case 5 -> Math.PI * 0.75D;
            default -> 0D;
        };
        Vec3 vec = rotateY(sideOff, 0D, angle);

        return new Vec3[]{
                new Vec3(0.5D + vec.x, 11.5D + topOff, 0.5D + vec.z),
                new Vec3(0.5D + vec.x, 11.5D - topOff, 0.5D + vec.z),
                new Vec3(0.5D - vec.x, 11.5D + topOff, 0.5D - vec.z),
                new Vec3(0.5D - vec.x, 11.5D - topOff, 0.5D - vec.z),
        };
    }

    @Override
    public double getMaxWireLength() {
        return 100D;
    }
}
