package com.hbm.blockentity.network.energy;

import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.tileentity.network.energy.TileEntityPylonMedium} (read in full):
 * {@code ConnectionType.TRIPLE}, 3 mount points fanning out from the core's facing direction.
 *
 * <p><b>Simplification vs. CE</b>: CE's {@code hasTransformer()}/extra transformer-side conductor
 * connection ({@code createNode}'s {@code if (hasTransformer())} branch, {@code canConnect}'s
 * transformer-facing gate) checks identity against two specific {@code ModBlocks} fields
 * ({@code red_pylon_medium_wood_transformer}/{@code red_pylon_medium_steel_transformer}) for block
 * variants this task's scope does not include (only plain {@code PylonMedium} is in the named
 * deliverable list). That whole branch is dropped: this class behaves as CE's non-transformer
 * variant unconditionally (pylon links only, no extra cable-facing connector) - a future pass adding
 * the transformer variants can reintroduce it without touching this class's core network logic.
 */
public class PylonMediumBlockEntity extends PylonBaseBlockEntity {

    public PylonMediumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.TRIPLE;
    }

    @Override
    public Vec3[] getMountPos() {
        BlockState state = getBlockState();
        int meta = state.hasProperty(BlockDummyable.META) ? state.getValue(BlockDummyable.META) - BlockDummyable.offset : 0;
        Direction dir = meta >= 0 && meta < 6 ? Direction.from3DDataValue(meta) : Direction.NORTH;
        double height = 7.5D;

        return new Vec3[]{
                new Vec3(0.5D, height, 0.5D),
                new Vec3(0.5D + dir.getStepX(), height, 0.5D + dir.getStepZ()),
                new Vec3(0.5D + dir.getStepX() * 2D, height, 0.5D + dir.getStepZ() * 2D),
        };
    }

    @Override
    public double getMaxWireLength() {
        return 45D;
    }
}
