package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.BatteryBlock;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single-block HE battery, ported from CE's {@code com.hbm.tileentity.machine.TileEntityMachineBattery}
 * (read in full - see {@code docs/phase2/machines_storage.md}). CE marks the owning block
 * {@code @Deprecated} in favor of a later multiblock battery bank, but (per this port's own precedent
 * on {@code ItemBattery}, restated by the research report) every one of its five registered grades is
 * still live, player-reachable CE content - ported as real Phase 2 content, not dead weight.
 *
 * <h2>What this ports unchanged from CE</h2>
 * <ul>
 *   <li>The 4-slot charge/discharge inventory (slot 0: insert a charged battery item to drain it into
 *   this block's HE pool; slot 1: take-only, receives the now-empty item; slot 2: insert a battery
 *   item to charge it from this block's HE pool; slot 3: take-only, receives the now-full item) via
 *   {@link Library#chargeTEFromItems}/{@link Library#chargeItemsFromTE} (both newly ported alongside
 *   this class - see {@code Library}'s own javadoc).</li>
 *   <li>The buffer/input/output/disabled 4-mode behavior, redstone-gated via {@code redLow}/
 *   {@code redHigh} (the mode used when unpowered/powered) and a {@code priority} used by
 *   {@link IEnergyReceiverMK2#getPriority()} - both {@link IEnergyConductorMK2} network-join paths
 *   (buffer mode creates/joins its own {@link Nodespace.PowerNode} and registers as both a provider
 *   and receiver on it, matching CE's "acts like a cable block" comment) and the direct-neighbor
 *   {@link IEnergyProviderMK2#tryProvide}/{@link IEnergyReceiverMK2#trySubscribe} paths for the other
 *   three modes are ported as-is.</li>
 *   <li>The rolling 20-tick power-delta log used by the GUI's "+/- N HE/s" readout.</li>
 * </ul>
 *
 * <h2>Deliberately narrowed scope vs. CE - documented in {@code machines_storage.md}</h2>
 * <ul>
 *   <li><b>No GUI mode-toggle buttons.</b> CE's {@code GUIMachineBattery} sends an
 *   {@code AuxButtonPacket} to cycle {@code redLow}/{@code redHigh}/{@code priority} on click; this
 *   port has no server-bound GUI-button packet infrastructure yet (confirmed absent -
 *   {@code com.hbm.packet} currently has only the client-bound {@code BufPacket}, and
 *   {@link MachineBaseBlockEntity#handleButtonPacket} is explicitly documented as "a no-op
 *   placeholder for future GUI button packets"). This is a small, self-contained cross-cutting gap
 *   shared by every future machine GUI with a clickable mode button, not something a single
 *   storage-machines package should invent its own one-off packet class for. The battery still works
 *   correctly without it: CE's own field defaults ({@code redLow = 0} i.e. charge-when-unpowered,
 *   {@code redHigh = 2} i.e. discharge-when-powered) are kept as this class's defaults too, so a
 *   freshly placed battery behaves exactly like a freshly placed CE one before anyone touches its GUI.</li>
 *   <li><b>No OpenComputers callbacks.</b> Per the research report's explicit recommendation (matching
 *   this port's precedent of dropping other Forge-1.12-era optional-mod integrations with no confirmed
 *   NeoForge 1.21 build, e.g. Galacticraft), CE's {@code @Optional.Interface}-gated
 *   {@code SimpleComponent}/{@code @Callback} methods are not ported.</li>
 * </ul>
 * ROR: CE {@code TileEntityMachineBattery.java:362-415}.
 */
public class BatteryBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IEnergyConductorMK2, IEnergyProviderMK2, IEnergyReceiverMK2, IPersistentNBT,
        IRORValueProvider, IRORInteractive {

    public static final int MODE_INPUT = 0;
    public static final int MODE_BUFFER = 1;
    public static final int MODE_OUTPUT = 2;
    public static final int MODE_NONE = 3;

    private final long[] log = new long[20];
    public long delta = 0;
    private long power = 0;

    private Nodespace.PowerNode node;

    public short redLow = 0;
    public short redHigh = 2;
    public IEnergyReceiverMK2.ConnectionPriority priority = IEnergyReceiverMK2.ConnectionPriority.LOW;
    private byte lastRedstone = 0;
    private boolean isIndirectlyPowered = false;
    private short modeCache = 0;

    /** Cached once from the owning {@link BatteryBlock}, matching CE's own {@code bufferedMax} field. */
    private long bufferedMax = 0;

    public BatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.battery");
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == 0) return Library.isDischargeableBattery(stack);
        if (i == 2) return Library.isChargeableBattery(stack);
        return false;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemStack) {
        return isItemValidForSlot(slot, itemStack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == 1 || slot == 3;
    }

    private void tryMoveItems() {
        ItemStack drained = inventory.getStackInSlot(0);
        if (Library.isEmptyBattery(drained) && inventory.getStackInSlot(1).isEmpty()) {
            inventory.setStackInSlot(1, drained);
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }
        ItemStack charged = inventory.getStackInSlot(2);
        if (Library.isFullBattery(charged) && inventory.getStackInSlot(3).isEmpty()) {
            inventory.setStackInSlot(3, charged);
            inventory.setStackInSlot(2, ItemStack.EMPTY);
        }
    }

    /** Called from {@link com.hbm.blocks.machine.BatteryBlock#neighborChanged}, matching CE's own immediate pickup (rather than waiting for the 20-tick poll in {@link #updateEntity()} below). */
    public void setIndirectlyPowered(boolean isIndirectlyPowered) {
        this.isIndirectlyPowered = isIndirectlyPowered;
    }

    private short getRelevantMode(boolean useCache) {
        if (useCache) return this.modeCache;
        this.modeCache = isIndirectlyPowered ? redHigh : redLow;
        return this.modeCache;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            isIndirectlyPowered = level.hasNeighborSignal(worldPosition);
        }

        if (priority == null) priority = IEnergyReceiverMK2.ConnectionPriority.LOW;

        int mode = getRelevantMode(false);
        long prevPower = power;

        power = Library.chargeItemsFromTE(inventory, 2, power, getMaxPower());

        // Buffer mode: acts like a cable segment, joining/creating its own power node and
        // registering itself as both provider and receiver on it (CE's own "acts like a cable
        // block" comment). CE achieves the provider-registration half via a self-targeted
        // tryProvide(world, pos, ForgeDirection.UNKNOWN) call, which only works because CE's
        // ForgeDirection has a null-like UNKNOWN placeholder value with no 1.21 Direction
        // equivalent; calling node.net.addProvider(this) directly (right next to the existing
        // addReceiver(this) call below) reaches the exact same end state without that placeholder.
        if (mode == MODE_BUFFER) {
            if (this.node == null || this.node.expired) {
                this.node = Nodespace.getNode(level, worldPosition);
                if (this.node == null || this.node.expired) {
                    this.node = createNode();
                    Nodespace.createNode(level, this.node);
                }
            }
            if (node != null && node.hasValidNet()) {
                node.net.addProvider(this);
                node.net.addReceiver(this);
            }
        } else {
            if (this.node != null) {
                Nodespace.destroyNode(level, worldPosition);
                this.node = null;
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                Nodespace.PowerNode dirNode = Nodespace.getNode(level, neighborPos);

                if (mode == MODE_OUTPUT) {
                    tryProvide(level, neighborPos, dir);
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeProvider(this);
                }

                if (mode == MODE_INPUT) {
                    if (dirNode != null && dirNode.hasValidNet()) dirNode.net.addReceiver(this);
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeReceiver(this);
                }
            }
        }

        byte comp = getComparatorPower();
        tryMoveItems();
        if (comp != lastRedstone) {
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        lastRedstone = comp;

        power = Library.chargeTEFromItems(inventory, 0, power, getMaxPower());

        long avg = (power + prevPower) / 2;
        delta = avg - log[0];
        System.arraycopy(log, 1, log, 0, log.length - 1);
        log[log.length - 1] = avg;

        networkPackNT(20);
    }

    public long getPowerRemainingScaled(long scale) {
        long max = getMaxPower();
        return max <= 0 ? 0 : (power * scale) / max;
    }

    public byte getComparatorPower() {
        if (power == 0) return 0;
        double frac = (double) power / (double) getMaxPower() * 15D;
        return (byte) Math.clamp((long) frac + 1, 0, 15);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        if (bufferedMax == 0 && getBlockState().getBlock() instanceof BatteryBlock battery) {
            bufferedMax = battery.getMaxPower();
        }
        return bufferedMax;
    }

    @Override
    public long getProviderSpeed() {
        int mode = getRelevantMode(true);
        return mode == MODE_OUTPUT || mode == MODE_BUFFER ? getMaxPower() / 20 : 0;
    }

    @Override
    public long getReceiverSpeed() {
        int mode = getRelevantMode(true);
        return mode == MODE_INPUT || mode == MODE_BUFFER ? getMaxPower() / 20 : 0;
    }

    @Override
    public boolean canConnect(Direction dir) {
        return true;
    }

    @Override
    public IEnergyReceiverMK2.ConnectionPriority getPriority() {
        return priority;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(delta);
        buf.writeShort(redLow);
        buf.writeShort(redHigh);
        buf.writeByte(priority.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        delta = buf.readLong();
        redLow = buf.readShort();
        redHigh = buf.readShort();
        priority = IEnergyReceiverMK2.ConnectionPriority.VALUES[buf.readByte()];
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putShort("redLow", redLow);
        tag.putShort("redHigh", redHigh);
        tag.putByte("lastRedstone", lastRedstone);
        tag.putByte("priority", (byte) priority.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        redLow = tag.getShort("redLow");
        redHigh = tag.getShort("redHigh");
        lastRedstone = tag.getByte("lastRedstone");
        priority = IEnergyReceiverMK2.ConnectionPriority.VALUES[tag.getByte("priority")];
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        CompoundTag data = new CompoundTag();
        data.putLong("power", power);
        data.putShort("redLow", redLow);
        data.putShort("redHigh", redHigh);
        data.putInt("priority", priority.ordinal());
        nbt.put(NBT_PERSISTENT_KEY, data);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        CompoundTag data = nbt.getCompound(NBT_PERSISTENT_KEY);
        power = data.getLong("power");
        redLow = data.getShort("redLow");
        redHigh = data.getShort("redHigh");
        priority = IEnergyReceiverMK2.ConnectionPriority.VALUES[data.getInt("priority")];
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :362-364
        return new String[]{
                PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent", PREFIX_VALUE + "delta",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)",
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :367-371
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + getPowerRemainingScaled(100);
        if ((PREFIX_VALUE + "delta").equals(name)) return "" + delta;
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :375-415
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int next = IRORInteractive.parseInt(params[0], 0, 3);
            if (next != this.redLow) {
                this.redLow = (short) next;
                setChanged();
                return null;
            } else if (params.length > 1) {
                this.redLow = (short) IRORInteractive.parseInt(params[1], 0, 3);
                setChanged();
                return null;
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setredmode").equals(name) && params.length > 0) {
            int next = IRORInteractive.parseInt(params[0], 0, 3);
            if (next != this.redHigh) {
                this.redHigh = (short) next;
                setChanged();
                return null;
            } else if (params.length > 1) {
                this.redHigh = (short) IRORInteractive.parseInt(params[1], 0, 3);
                setChanged();
                return null;
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            int p = IRORInteractive.parseInt(params[0], 0, 2) + 1;
            this.priority = IEnergyReceiverMK2.ConnectionPriority.VALUES[p];
            setChanged();
            return null;
        }
        return null;
    }
}
