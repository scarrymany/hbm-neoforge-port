package com.hbm.blockentity.network.energy;

import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.api.energymk2.PowerNetMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.network.energy.CableDiodeBlock;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code CableDiode.TileEntityDiode} (inner class, read in full): limits throughput
 * and restricts flow direction, one-way. Every energy-side method
 * ({@code transferPower}/{@code getReceiverSpeed}/{@code update}/{@code canConnect}) is a mechanical
 * transcription onto Phase 0's already-verified {@code IEnergyConnectorMK2}/{@code Nodespace}/
 * {@code UniNodespace}/{@code PowerNetMK2} API - CE's own {@code sendPowerDiode} bypass path is
 * called exactly the way CE's TE already calls it.
 *
 * <p><b>Deferred, not dropped</b>: CE's {@code IGUIProvider}/{@code provideGUI}/{@code provideContainer}
 * (a limit/priority-setting screen, {@code GUIDiode}) has no {@code Screen} port yet - per the research
 * report's own framing, only the GUI half waits; this class's {@link IControlReceiver#receiveControl}
 * is kept as a real, callable method (ready for a future packet handler) even though nothing sends
 * that packet yet, and right-clicking the block currently does nothing (see
 * {@link CableDiodeBlock#useWithoutItem}) rather than opening a menu.
 */
public class CableDiodeBlockEntity extends LoadedBaseBlockEntity implements IEnergyReceiverMK2, IControlReceiver, ITickableBE {

    /** Intra-tick transfer tracker, reset to 0 at the end of every {@link #updateEntity()}. */
    private long power;
    private boolean recursionBrake = false;
    private int pulses = 0;
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    public long limit = 1_000L;

    public CableDiodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The direction data is only ever pulled TOWARDS (opposite the block's {@code FACING}). */
    private Direction outputDir() {
        BlockState state = getBlockState();
        return state.hasProperty(CableDiodeBlock.FACING) ? state.getValue(CableDiodeBlock.FACING).getOpposite() : Direction.NORTH;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        Direction outDir = outputDir();
        for (Direction dir : Direction.values()) {
            if (dir == outDir) continue;
            BlockPos neighbor = worldPosition.relative(dir);
            this.trySubscribe(level, neighbor.getX(), neighbor.getY(), neighbor.getZ(), dir);
        }

        pulses = 0;
        this.setPower(0);
        this.networkPackNT(15);
    }

    @Override
    public boolean canConnect(Direction dir) {
        return dir != outputDir();
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (recursionBrake) return power;

        int effectivePulses = pulses + 1;
        if (this.getPower() >= this.getMaxPower() || effectivePulses > 10) return power;
        if (!simulate) pulses = effectivePulses;

        recursionBrake = true;
        try {
            Direction dir = outputDir();
            BlockPos targetPos = worldPosition.relative(dir);
            Nodespace.PowerNode node = Nodespace.getNode(level, targetPos);
            BlockEntity te = Compat.getBlockEntityStandard(level, targetPos);

            if (node != null && !node.expired && node.hasValidNet()
                    && te instanceof IEnergyConnectorMK2 con && con.canConnect(dir.getOpposite())) {
                long toTransfer = Math.min(power, this.getReceiverSpeed());
                PowerNetMK2 net = node.net;
                long remainder = net.sendPowerDiode(toTransfer, simulate);
                long transferred = toTransfer - remainder;
                if (!simulate) this.power += transferred;
                power -= transferred;
            } else if (te instanceof IEnergyReceiverMK2 rec && te != this && rec.canConnect(dir.getOpposite())) {
                long toTransfer = Math.min(power, rec.getReceiverSpeed());
                long remainder = rec.transferPower(toTransfer, simulate);
                long transferred = toTransfer - remainder;
                if (!simulate) this.power += transferred;
                power -= transferred;
            }
        } finally {
            recursionBrake = false;
        }

        return power;
    }

    @Override
    public long getReceiverSpeed() {
        return this.getMaxPower() - this.getPower();
    }

    @Override
    public long getMaxPower() {
        return this.limit;
    }

    @Override
    public long getPower() {
        return Math.min(power, this.getMaxPower());
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public ConnectionPriority getPriority() {
        return this.priority;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 128.0D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("limit")) this.limit = data.getLong("limit");
        if (data.contains("priority")) {
            byte ordinal = data.getByte("priority");
            ConnectionPriority[] values = ConnectionPriority.VALUES;
            if (ordinal >= 0 && ordinal < values.length) this.priority = values[ordinal];
        }
        if (limit < 0) limit = 0;
        if (limit > 10_000_000_000L) limit = 10_000_000_000L;
        setChanged();
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        if (hasPermission(player)) receiveControl(data);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.limit = tag.getLong("limit");
        byte ordinal = tag.getByte("p");
        ConnectionPriority[] values = ConnectionPriority.VALUES;
        this.priority = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ConnectionPriority.NORMAL;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("limit", limit);
        tag.putByte("p", (byte) this.priority.ordinal());
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(limit);
        buf.writeByte((byte) this.priority.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        limit = buf.readLong();
        byte ordinal = buf.readByte();
        ConnectionPriority[] values = ConnectionPriority.VALUES;
        priority = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ConnectionPriority.NORMAL;
    }
}
