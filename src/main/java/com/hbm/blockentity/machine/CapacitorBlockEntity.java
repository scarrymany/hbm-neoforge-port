package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.CapacitorBlock;
import com.hbm.blocks.machine.CapacitorBusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Capacitor block entity, ported from CE's {@code com.hbm.blocks.machine.MachineCapacitor}'s inline
 * static {@code TileEntityCapacitor} class (read in full - see {@code docs/phase2/machines_storage.md}).
 * Unlike {@link BatteryBlockEntity}, a capacitor has no inventory or GUI at all in CE - it is a pure
 * directional HE buffer that only ever provides to, and receives from, whatever sits directly behind
 * it (through a chain of {@link CapacitorBusBlock} "wire" blocks, if any). It therefore extends
 * {@link LoadedBaseBlockEntity} directly rather than {@link com.hbm.blockentity.MachineBaseBlockEntity}
 * - matching that class's own javadoc ("extend this ... unless it genuinely has no inventory").
 *
 * <p>Not an {@link com.hbm.api.energymk2.IEnergyConductorMK2}: a capacitor never joins a power
 * network as a cable segment the way a buffer-mode {@link BatteryBlockEntity} can, matching CE.
 *
 * <h2>Deliberately narrowed scope vs. CE</h2>
 * <ul>
 *   <li>No OpenComputers callbacks and no redstone-over-radio interactive side - same reasoning as
 *   {@link BatteryBlockEntity}'s javadoc.</li>
 *   <li><b>Facing/rotation simplified.</b> CE's {@code update()} derives its send direction as
 *   {@code ForgeDirection.getOrientation(meta).getRotation(ForgeDirection.DOWN).getOpposite()} before
 *   walking the bus chain, and its receive direction as the un-rotated
 *   {@code getRotation(ForgeDirection.DOWN)} result - an extra 1.12 {@code ForgeDirection} axis
 *   rotation whose intended effect for a full-6-direction {@code BlockDirectional} facing cannot be
 *   confirmed from source alone in this sandbox (no comment explains it, and it reads as either a
 *   coordinate-system compatibility shim or a leftover mistake - CE's own file has an identical
 *   unexplained-oddity precedent elsewhere, per {@code MachineCapacitor.onBlockHarvested}'s "wtf?"
 *   comment). This class instead uses the direct, self-consistent reading: a capacitor sends power
 *   toward its own {@link BlockStateProperties#FACING} value and receives from the opposite side, with
 *   no extra rotation. Flagged here rather than silently guessed at - worth re-deriving against a real
 *   running CE instance if placement-direction parity turns out to matter.</li>
 * </ul>
 */
public class CapacitorBlockEntity extends LoadedBaseBlockEntity
        implements ITickableBE, IEnergyProviderMK2, IEnergyReceiverMK2, IPersistentNBT {

    private long power;
    /** Cached once from the owning {@link CapacitorBlock}, matching CE's own field of the same role. */
    private long maxPower;
    private long powerReceived;
    private long powerSent;
    private long lastPowerReceived;
    private long lastPowerSent;
    private boolean destroyedByCreativePlayer = false;

    public CapacitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private long maxPower() {
        if (maxPower == 0 && getBlockState().getBlock() instanceof CapacitorBlock capacitor) {
            maxPower = capacitor.getMaxPower();
        }
        return maxPower;
    }

    /**
     * Walks a chain of {@link CapacitorBusBlock}s starting directly behind this capacitor (the
     * direction opposite its own {@link BlockStateProperties#FACING}), stopping the instant the
     * chain bends (a bus segment facing a different way than the one before it) - ported from CE's
     * {@code TileEntityCapacitor.update}'s inline {@code while} loop.
     *
     * @return the position just past the end of the chain (or directly behind this capacitor if the
     * chain has zero length) paired with the chain's own facing, or {@code null} if the chain bent.
     */
    private ChainEnd walkBusChain(Direction behind) {
        BlockPos pos = worldPosition.relative(behind);
        Direction last = null;
        boolean stepped = false;

        while (level.getBlockState(pos).getBlock() instanceof CapacitorBusBlock) {
            Direction current = level.getBlockState(pos).getValue(BlockStateProperties.FACING);
            if (!stepped) last = current;
            stepped = true;
            if (last != current) return null;
            pos = pos.relative(current);
        }

        return stepped ? new ChainEnd(pos, last) : new ChainEnd(pos, behind);
    }

    private record ChainEnd(BlockPos pos, Direction dir) {
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
        Direction behind = facing.getOpposite();

        ChainEnd chain = walkBusChain(behind);
        if (chain != null) {
            tryUnsubscribe(level, chain.pos().getX(), chain.pos().getY(), chain.pos().getZ());
            tryProvide(level, chain.pos(), chain.dir());
        }

        trySubscribe(level, worldPosition.relative(behind), behind);

        networkPackNT(15);

        lastPowerSent = powerSent;
        lastPowerReceived = powerReceived;
        powerSent = 0;
        powerReceived = 0;
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (power <= 0) return power;
        long current = getPower();
        long capacity = Math.max(getMaxPower() - current, 0L);
        if (capacity <= 0) return power;
        long accepted = Math.min(power, capacity);
        if (!simulate) {
            setPower(current + accepted);
            powerReceived += accepted;
        }
        return power - accepted;
    }

    @Override
    public void usePower(long power) {
        if (power <= 0) return;
        long used = Math.min(getPower(), power);
        if (used <= 0) return;
        powerSent += used;
        setPower(getPower() - used);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return maxPower();
    }

    @Override
    public long getProviderSpeed() {
        return getMaxPower() / 300;
    }

    @Override
    public long getReceiverSpeed() {
        return getMaxPower() / 100;
    }

    @Override
    public IEnergyReceiverMK2.ConnectionPriority getPriority() {
        return IEnergyReceiverMK2.ConnectionPriority.LOW;
    }

    @Override
    public void setPower(long power) {
        long clamped = maxPower() > 0 ? Math.clamp(power, 0L, maxPower()) : Math.max(0L, power);
        if (this.power == clamped) return;
        this.power = clamped;
        if (level != null && !level.isClientSide) setChanged();
    }

    @Override
    public boolean canConnect(Direction dir) {
        return getBlockState().getValue(BlockStateProperties.FACING) == dir;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeLong(powerReceived);
        buf.writeLong(powerSent);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        powerReceived = buf.readLong();
        powerSent = buf.readLong();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putLong("maxPower", maxPower);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        maxPower = tag.getLong("maxPower");
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        CompoundTag data = new CompoundTag();
        data.putLong("power", power);
        data.putLong("maxPower", maxPower);
        nbt.put(NBT_PERSISTENT_KEY, data);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        CompoundTag data = nbt.getCompound(NBT_PERSISTENT_KEY);
        power = data.getLong("power");
        maxPower = data.getLong("maxPower");
    }

    @Override
    public void setDestroyedByCreativePlayer() {
        this.destroyedByCreativePlayer = true;
    }

    @Override
    public boolean isDestroyedByCreativePlayer() {
        return this.destroyedByCreativePlayer;
    }
}
