package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * {@link IFluidConnectorMK2} with an added node-creation method: the fluid-side counterpart to
 * {@link com.hbm.api.energymk2.IEnergyConductorMK2}, and the interface every duct/pipe block entity
 * in {@code com.hbm.blockentity.network} implements.
 *
 * <p>Ported from CE, translating 1.12.2 {@code TileEntity#getPos()} to {@code BlockEntity#getBlockPos()}
 * and CE's {@code Library.POS_X}/{@code NEG_X}/... {@code ForgeDirection} constants to plain
 * {@link Direction#EAST}/{@link Direction#WEST}/... - the exact same one-for-one substitution already
 * made, and confirmed compiling, by {@link com.hbm.api.energymk2.IEnergyConductorMK2#createNode()}'s
 * own six {@link DirPos} entries. The fluid pipe anchor's block entity (CE's {@code TileEntityPipelineBase}/
 * {@code TileEntityPipeAnchor}) overrides this default with a wrench-linked, non-face-adjacent
 * connection set instead - see {@code com.hbm.blockentity.network.PipelineBaseBlockEntity#createNode}.
 */
public interface IFluidPipeMK2 extends IFluidConnectorMK2 {

    default FluidNode createNode(FluidType type) {
        BlockEntity self = (BlockEntity) this;
        BlockPos pos = self.getBlockPos();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return new FluidNode(type.getNetworkProvider(), pos).setConnections(
                new DirPos(x + 1, y, z, Direction.EAST),
                new DirPos(x - 1, y, z, Direction.WEST),
                new DirPos(x, y + 1, z, Direction.UP),
                new DirPos(x, y - 1, z, Direction.DOWN),
                new DirPos(x, y, z + 1, Direction.SOUTH),
                new DirPos(x, y, z - 1, Direction.NORTH)
        );
    }
}
