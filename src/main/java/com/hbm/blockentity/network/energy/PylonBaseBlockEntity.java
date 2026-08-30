package com.hbm.blockentity.network.energy;

import com.hbm.api.energymk2.Nodespace;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.tileentity.network.energy.TileEntityPylonBase} (read in full): the
 * shared pylon-to-pylon wire-linking base for {@link PylonBlockEntity}/{@link PylonLargeBlockEntity}/
 * {@link PylonMediumBlockEntity}/{@link SubstationBlockEntity}. Extends {@link CableBaseBlockEntity}
 * (matching CE's own {@code extends TileEntityCableBaseNT}) - a pylon is still a normal HE conductor
 * node, just one whose {@link com.hbm.api.energymk2.Nodespace.PowerNode} also carries arbitrary-
 * distance links to other pylons instead of only the 6 adjacent neighbors.
 *
 * <p><b>{@code null}-direction {@link DirPos} entries</b>: CE encodes a pylon-to-pylon link as a
 * {@code DirPos} whose direction is {@code ForgeDirection.UNKNOWN} (a 1.12-only 7th sentinel value,
 * self-inverse, zero step) rather than any of the 6 real directions, since two linked pylons are
 * essentially never face-adjacent. {@code net.minecraft.core.Direction} has no such value, so
 * {@code null} is used as the exact same sentinel here - see
 * {@code UniNodespace.PerTypeNodeManager#checkConnection}'s own javadoc for the (already-landed,
 * shared) narrow fix that makes {@code null} behave the same way {@code UNKNOWN} did: zero step,
 * matches only another {@code null}. This is additive to the existing 6-way adjacency check, not a
 * redesign of it.
 *
 * <p><b>Dropped vs. CE</b>: the {@code ForgeDirection.UNKNOWN} self-referencing stub CE's
 * {@code createNode()} overrides always prepend to the connections list (a same-position, zero-step
 * entry) is not reproduced - tracing {@code checkNodeConnection}/{@code checkConnection} shows it
 * only ever produces an inert self-connection (both sides of the "reciprocal" check resolve to the
 * exact same node instance), so it has no observable effect and is safely omitted rather than ported
 * as dead weight. CE's OreDict-based {@code ColorUtil.getColorFromDye} (1.12-only, no OreDict in
 * 1.21) is replaced by a direct {@link DyeItem#getDyeColor()} read - simpler and strictly equivalent
 * for every real dye item, the only kind CE's own dye-name-prefix matching ever accepted anyway.
 */
public abstract class PylonBaseBlockEntity extends CableBaseBlockEntity {

    public List<BlockPos> connected = new ArrayList<>();
    public int color;

    protected PylonBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * @return 0 = OK to connect, 1 = mismatched {@link ConnectionType}, 2 = self-connect, 3 = too far.
     * Ported from CE's {@code TileEntityPylonBase.canConnect(first, second)} (static, called by
     * {@code ItemWiring}).
     */
    public static int canConnect(PylonBaseBlockEntity first, PylonBaseBlockEntity second) {
        if (first.getConnectionType() != second.getConnectionType()) return 1;
        if (first == second) return 2;

        double len = Math.min(first.getMaxWireLength(), second.getMaxWireLength());
        double dist = first.getConnectionPoint().distanceTo(second.getConnectionPoint());
        return len >= dist ? 0 : 3;
    }

    /** @return true if the stack was a new, different dye and one item was consumed. */
    public boolean setColor(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof DyeItem dye)) return false;
        int newColor = dye.getDyeColor().getFireworkColor();
        if (newColor == this.color) return false;

        stack.shrink(1);
        this.color = newColor;
        setChanged();
        networkPackNT(64);
        return true;
    }

    @Override
    public Nodespace.PowerNode createNode() {
        Nodespace.PowerNode node = new Nodespace.PowerNode(worldPosition);
        DirPos[] links = new DirPos[connected.size()];
        for (int i = 0; i < links.length; i++) links[i] = new DirPos(connected.get(i), null);
        node.setConnections(links);
        return node;
    }

    /** Ported from CE's {@code addConnection(x, y, z)}. */
    public void addConnection(BlockPos target) {
        connected.add(target);

        Nodespace.PowerNode node = level != null ? Nodespace.getNode(level, worldPosition) : null;
        if (node != null) {
            node.recentlyChanged = true;
            node.addConnection(new DirPos(target, null));
        }

        setChanged();
        networkPackNT(64);
    }

    /** Ported from CE's {@code disconnectAll()} - called from the owning block's {@code onRemove}. */
    public void disconnectAll() {
        if (level == null) return;

        for (BlockPos linkedPos : new ArrayList<>(connected)) {
            BlockEntity te = level.getBlockEntity(linkedPos);
            if (te == this) continue;

            if (te instanceof PylonBaseBlockEntity pylon) {
                Nodespace.destroyNode(level, linkedPos);
                pylon.connected.removeIf(worldPosition::equals);
                pylon.setChanged();
                pylon.networkPackNT(64);
            }
        }

        Nodespace.destroyNode(level, worldPosition);
        connected.clear();
    }

    public abstract ConnectionType getConnectionType();

    public abstract Vec3[] getMountPos();

    public abstract double getMaxWireLength();

    public Vec3 getConnectionPoint() {
        Vec3[] mounts = getMountPos();
        if (mounts == null || mounts.length == 0) return Vec3.atCenterOf(worldPosition);
        return mounts[0].add(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    /** 2D rotation around the Y axis, ported from CE's {@code Vec3NT.rotateAroundYRad}. */
    protected static Vec3 rotateY(double x, double z, double angleRad) {
        double c = Math.cos(angleRad);
        double s = Math.sin(angleRad);
        return new Vec3(x * c + z * s, 0, -x * s + z * c);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("conCount", connected.size());
        tag.putInt("color", color);
        for (int i = 0; i < connected.size(); i++) {
            BlockPos p = connected.get(i);
            tag.putIntArray("con" + i, new int[]{p.getX(), p.getY(), p.getZ()});
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.color = tag.getInt("color");

        int count = tag.getInt("conCount");
        connected.clear();
        for (int i = 0; i < count; i++) {
            int[] arr = tag.getIntArray("con" + i);
            if (arr.length == 3) connected.add(new BlockPos(arr[0], arr[1], arr[2]));
        }
    }

    /**
     * {@code connected}/{@code color} ride the same per-tick sync payload every other machine in this
     * port uses ({@link com.hbm.blockentity.LoadedBaseBlockEntity#serializeInitial} defaults to this
     * method, so no separate chunk-load override is needed - matching
     * {@code MachineSteamEngineBlockEntity}'s identical convention), pushed on demand via
     * {@link #networkPackNT} whenever {@link #setColor}/{@link #addConnection}/{@link #disconnectAll}
     * change either field, since a pylon otherwise never ticks its own sync out (CE's own
     * {@code TileEntityCableBaseNT.update()} has no periodic network-pack call either - pylons only
     * push updates when something actually changes).
     */
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(color);
        buf.writeVarInt(connected.size());
        for (BlockPos p : connected) buf.writeBlockPos(p);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        color = buf.readInt();
        int count = buf.readVarInt();
        connected.clear();
        for (int i = 0; i < count; i++) connected.add(buf.readBlockPos());
    }

    public enum ConnectionType {
        SINGLE, TRIPLE, QUAD
    }
}
