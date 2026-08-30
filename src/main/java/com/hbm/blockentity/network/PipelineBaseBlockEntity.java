package com.hbm.blockentity.network;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract long-distance, wrench-linked pipe base, ported from CE's
 * {@code com.hbm.tileentity.network.TileEntityPipelineBase} (CE's own "copy pasted crap class" per its
 * comment, read in full). The one exception to pure 6-directional adjacency in this package: instead
 * of {@link com.hbm.api.fluidmk2.IFluidPipeMK2}'s default 6-neighbor {@code createNode}, this overrides
 * it with an explicit {@link #connected} position list, each tagged with a {@code null} {@code Direction}
 * (this port's translation of CE's self-inverse, zero-offset {@code ForgeDirection.UNKNOWN} sentinel -
 * see {@code UniNodespace.PerTypeNodeManager#checkConnection}'s null-direction handling, added
 * specifically for this class) so two distant anchors can join the same {@link com.hbm.api.fluidmk2.FluidNetMK2}
 * without being face-adjacent at all.
 *
 * <p>{@link #addConnection}/{@link #disconnectAll}/{@link #canConnect(PipelineBaseBlockEntity,
 * PipelineBaseBlockEntity)} are the TE-side surface {@code ItemWrench} calls in CE - ported mechanically
 * per {@code docs/phase2/network_fluid_ducts.md}'s Deferred scope ("this package only needs to expose
 * the TE-side methods the wrench calls"); the wrench item itself is a separate package's concern and is
 * not wired to call these yet.
 */
public abstract class PipelineBaseBlockEntity extends PipeBaseBlockEntity {

    protected final List<BlockPos> connected = new ArrayList<>();

    public PipelineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public FluidNode createNode(FluidType type) {
        FluidNode node = new FluidNode(type.getNetworkProvider(), worldPosition).setConnections(new DirPos(worldPosition, null));
        for (BlockPos p : this.connected) node.addConnection(new DirPos(p, null));
        return node;
    }

    /** Called by {@code ItemWrench} to link this anchor to another position. */
    public void addConnection(BlockPos pos) {
        if (level == null) return;

        connected.add(pos);

        if (this.node == null || this.node.expired) {
            if (shouldCreateNode()) {
                this.node = (FluidNode) UniNodespace.getNode(level, worldPosition, type.getNetworkProvider());
                if (this.node == null || this.node.expired) {
                    this.node = this.createNode(type);
                    UniNodespace.createNode(level, this.node);
                }
            }
        }

        if (this.node != null) {
            this.node.recentlyChanged = true;
            this.node.addConnection(new DirPos(pos, null));
        }

        setChanged();

        if (!level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide && (this.node == null || this.node.expired) && pruneStaleConnections()) {
            setChanged();
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }

        super.updateEntity();
    }

    /**
     * Drops connections whose target no longer has a block entity (broken/replaced while unloaded).
     * Simpler than CE's {@code hasTileEntity(state)} check (no {@code Block}-level lookup table exists
     * in 1.21 the way CE's did) but behaviorally equivalent: a position that no longer resolves to any
     * block entity can never again satisfy {@code checkConnection} either way.
     */
    private boolean pruneStaleConnections() {
        boolean changed = false;
        for (int i = 0; i < connected.size(); i++) {
            BlockPos target = connected.get(i);
            if (target.equals(worldPosition)) continue;
            if (!level.isLoaded(target)) continue;
            if (level.getBlockEntity(target) != null) continue;

            connected.remove(i);
            i--;
            changed = true;
        }
        return changed;
    }

    /** Called by {@code ItemWrench} (or block removal) to sever every link this anchor holds. */
    public void disconnectAll() {
        if (level == null || level.isClientSide) return;

        for (BlockPos pos : connected) {
            if (pos.equals(worldPosition)) continue;
            if (level.getBlockEntity(pos) instanceof PipelineBaseBlockEntity pipeline) {

                pipeline.connected.removeIf(p -> p.equals(worldPosition));
                pipeline.setChanged();

                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }

        if (this.node != null) {
            UniNodespace.destroyNode(level, node);
            this.node = null;
        }
    }

    /**
     * {@code disconnectAll()} already destroys this anchor's own node (nulling {@link #node} so
     * {@link PipeBaseBlockEntity#setRemoved()}'s own destroy-if-present check below is a safe no-op
     * instead of a redundant {@code UniNodespace.destroyNode} call - double-destroying the same node
     * would hit {@code UniNodespace}'s "already removed" warning/stack-dump branch).
     */
    @Override
    public void setRemoved() {
        disconnectAll();
        super.setRemoved();
    }

    /**
     * Returns a status code based on the operation.<br>
     * 0: Connected<br>
     * 1: Connections are incompatible<br>
     * 2: Both parties are the same block<br>
     * 3: Connection length exceeds maximum<br>
     * 4: Pipeline fluid types do not match
     */
    public static int canConnect(PipelineBaseBlockEntity first, PipelineBaseBlockEntity second) {
        if (first.getConnectionType() != second.getConnectionType()) return 1;
        if (first == second) return 2;

        if (first.type == Fluids.NONE && second.type != first.type) first.setType(second.type);
        if (second.type == Fluids.NONE && first.type != second.type) second.setType(first.type);

        if (first.type != second.type) return 4;

        double len = Math.min(first.getMaxPipeLength(), second.getMaxPipeLength());
        Vec3 delta = second.getConnectionPoint().subtract(first.getConnectionPoint());

        return len >= delta.length() ? 0 : 3;
    }

    public abstract ConnectionType getConnectionType();

    public abstract Vec3 getMountPos();

    public abstract double getMaxPipeLength();

    public Vec3 getConnectionPoint() {
        return getMountPos().add(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    public List<BlockPos> getConnected() {
        return connected;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("conCount", connected.size());
        for (int i = 0; i < connected.size(); i++) {
            BlockPos p = connected.get(i);
            tag.putIntArray("con" + i, new int[] { p.getX(), p.getY(), p.getZ() });
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int count = tag.getInt("conCount");
        connected.clear();
        for (int i = 0; i < count; i++) {
            int[] c = tag.getIntArray("con" + i);
            if (c.length == 3) connected.add(new BlockPos(c[0], c[1], c[2]));
        }
    }

    public enum ConnectionType {
        SMALL
    }
}
