package com.hbm.blockentity.network;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.Library;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exhaust/vent duct that carries all three smoke {@link FluidType}s (regular/leaded/poison)
 * simultaneously over three independent {@link FluidNode}s, ported from CE's
 * {@code com.hbm.tileentity.network.TileEntityPipeExhaust}. Deliberately does <em>not</em> extend
 * {@link PipeBaseBlockEntity} - CE's own class extends {@code TileEntity} directly and ticks its own
 * three-node lifecycle inline rather than inheriting the single-type node from
 * {@code TileEntityPipeBaseNT}, confirmed at implementation time per
 * {@code docs/phase2/network_fluid_ducts.md}'s Open questions flag ("worth double-checking ... whether
 * {@code TileEntityPipeExhaust} duplicates the same node-lifecycle logic inline") - it does, and this
 * class mirrors that duplication rather than forcing it into the single-node base.
 */
public class PipeExhaustBlockEntity extends LoadedBaseBlockEntity implements IFluidPipeMK2, ITickableBE, ICachedPipeConnections {

    private static final FluidType[] SMOKES = { Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON };

    protected final FluidNode[] nodes = new FluidNode[SMOKES.length];

    private byte cachedConnectionMask;
    private boolean cachedConnectionMaskValid;

    public PipeExhaustBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidType[] getSmokes() {
        return SMOKES;
    }

    @Override
    public byte getCachedConnectionMask(BlockGetter access) {
        if (level != null && level.isClientSide) return computeConnectionMask(access);
        if (!cachedConnectionMaskValid) {
            cachedConnectionMask = computeConnectionMask(access);
            cachedConnectionMaskValid = true;
        }
        return cachedConnectionMask;
    }

    @Override
    public void invalidateConnectionCache() {
        cachedConnectionMaskValid = false;
    }

    private byte computeConnectionMask(BlockGetter access) {
        byte mask = 0;
        for (Direction facing : Direction.values()) {
            BlockPos adj = worldPosition.relative(facing);
            if (access instanceof Level lvl && !lvl.isLoaded(adj)) continue;
            for (FluidType smoke : SMOKES) {
                if (Library.canConnectFluid(access, adj, facing, smoke)) {
                    mask |= (byte) (1 << facing.get3DDataValue());
                    break;
                }
            }
        }
        return mask;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            invalidateConnectionCache();
            for (Direction facing : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(facing);
                if (!level.isLoaded(neighborPos)) continue;
                if (level.getBlockEntity(neighborPos) instanceof ICachedPipeConnections cached) {
                    cached.invalidateConnectionCache();
                }
            }
        }
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide || !canUpdate()) return;

        for (int i = 0; i < SMOKES.length; i++) {
            if (nodes[i] == null || nodes[i].expired) {
                nodes[i] = (FluidNode) UniNodespace.getNode(level, worldPosition, SMOKES[i].getNetworkProvider());

                if (nodes[i] == null || nodes[i].expired) {
                    nodes[i] = this.createNode(SMOKES[i]);
                    UniNodespace.createNode(level, nodes[i]);
                }
            }
        }
    }

    public boolean canUpdate() {
        for (FluidNode node : nodes) {
            if (node == null || node.net == null || !node.net.isValid()) return !isRemoved();
        }
        return false;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            for (int i = 0; i < SMOKES.length; i++) {
                if (nodes[i] != null) UniNodespace.destroyNode(level, nodes[i]);
            }
        }
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null && (type == Fluids.SMOKE || type == Fluids.SMOKE_LEADED || type == Fluids.SMOKE_POISON);
    }
}
