package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.block.ILockable;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MassStorageMenu;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.items.tool.ToolItems;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityMassStorage}: 3 slots (in / filter / out), stockpile, output toggle.
 * {@code isLocked()} gates {@code canInsert} / hopper Exact CE {@code :112}/{@code :258}/{@code :263}
 * via already-real {@link ILockable} (CE inherits {@code TileEntityLockableBase}). OC skipped.
 * ROR: CE {@code :293-317}.
 */
public class MassStorageBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider, IPersistentNBT, IRORValueProvider, IRORInteractive, ILockable {

    public static final int SLOT_IN = 0;
    public static final int SLOT_FILTER = 1;
    public static final int SLOT_OUT = 2;

    private int stack;
    public boolean output;
    private int capacity;
    public int redstone;

    /** CE {@code TileEntityLockableBase}: lock / isLocked / lockMod / cheesable. */
    private int lock;
    private boolean isLocked;
    private double lockMod = 0.1D;
    private boolean cheesable = true;

    public MassStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 10_000);
    }

    public MassStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, 3, false, false);
        this.capacity = capacity > 0 ? capacity : 10_000;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.massStorage");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot != SLOT_IN) return false;
        ItemStack type = getFilterType();
        return !type.isEmpty() && ItemStack.isSameItemSameComponents(stack, type);
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemStack) {
        // CE TileEntityMassStorage.java:257-258
        return !isLocked() && isItemValidForSlot(slot, itemStack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        // CE :262-263
        return !isLocked() && slot == SLOT_OUT;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{SLOT_IN, SLOT_OUT};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        int newRed = getCapacity() > 0 ? getStockpile() * 15 / getCapacity() : 0;
        if (newRed != redstone) {
            redstone = newRed;
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }

        if (!inventory.getStackInSlot(SLOT_IN).isEmpty()
                && inventory.getStackInSlot(SLOT_IN).is(ToolItems.FLUID_BARREL_INFINITE.get())) {
            this.stack = getCapacity();
        }

        if (getFilterType().isEmpty()) this.stack = 0;

        ItemStack in = inventory.getStackInSlot(SLOT_IN);
        if (canInsert(in)) {
            int remaining = getCapacity() - getStockpile();
            int toRemove = Math.min(remaining, in.getCount());
            inventory.extractItem(SLOT_IN, toRemove, false);
            this.stack += toRemove;
            setChanged();
        }

        if (output && !getFilterType().isEmpty()) {
            ItemStack type = getFilterType();
            ItemStack out = inventory.getStackInSlot(SLOT_OUT);
            if (!out.isEmpty() && !ItemStack.isSameItemSameComponents(out, type)) {
                dataChanged();
                networkPackMK2(32);
                return;
            }
            int amount = Math.min(getStockpile(), type.getMaxStackSize());
            if (amount > 0) {
                if (out.isEmpty()) {
                    ItemStack placed = type.copy();
                    placed.setCount(amount);
                    inventory.setStackInSlot(SLOT_OUT, placed);
                    this.stack -= amount;
                } else {
                    amount = Math.min(amount, out.getMaxStackSize() - out.getCount());
                    out.grow(amount);
                    this.stack -= amount;
                }
                setChanged();
            }
        }

        dataChanged();
        networkPackMK2(32);
    }

    public boolean canInsert(ItemStack stack) {
        // CE :111-112
        if (stack.isEmpty() || this.isLocked()) return false;
        ItemStack type = getFilterType();
        if (type.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(stack, type);
    }

    public boolean quickInsert(ItemStack stack) {
        if (!canInsert(stack)) return false;
        int remaining = getCapacity() - getStockpile();
        if (remaining < stack.getCount()) return false;
        this.stack += stack.getCount();
        stack.setCount(0);
        setChanged();
        return true;
    }

    public ItemStack quickExtract() {
        if (!output) return ItemStack.EMPTY;
        ItemStack type = getFilterType();
        if (type.isEmpty()) return ItemStack.EMPTY;
        int amount = type.getMaxStackSize();
        if (getStockpile() < amount) return ItemStack.EMPTY;
        ItemStack result = type.copy();
        result.setCount(amount);
        this.stack -= amount;
        setChanged();
        return result;
    }

    public void receiveControl(CompoundTag data) {
        if (data.contains("provide") && !inventory.getStackInSlot(SLOT_FILTER).isEmpty()) {
            if (getStockpile() == 0) return;
            int amount = data.getBoolean("provide") ? inventory.getStackInSlot(SLOT_FILTER).getMaxStackSize() : 1;
            amount = Math.min(amount, getStockpile());
            ItemStack type = getFilterType();
            ItemStack out = inventory.getStackInSlot(SLOT_OUT);
            if (!out.isEmpty() && !ItemStack.isSameItemSameComponents(out, type)) return;
            if (out.isEmpty()) {
                ItemStack placed = type.copy();
                placed.setCount(amount);
                inventory.setStackInSlot(SLOT_OUT, placed);
                this.stack -= amount;
            } else {
                amount = Math.min(amount, out.getMaxStackSize() - out.getCount());
                out.grow(amount);
                this.stack -= amount;
            }
            setChanged();
        }
        if (data.contains("toggle")) {
            this.output = !this.output;
            setChanged();
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public ItemStack getFilterType() {
        ItemStack filter = inventory.getStackInSlot(SLOT_FILTER);
        return filter.isEmpty() ? ItemStack.EMPTY : filter.copy();
    }

    public int getStockpile() {
        return stack;
    }

    public void setStockpile(int stack) {
        this.stack = stack;
    }

    /** CE {@code TileEntityLockableBase#canAccess(EntityPlayer)}. */
    public boolean canAccess(Player player) {
        if (!isLocked()) return true;
        if (player == null) return false;
        ItemStack held = player.getMainHandItem();
        int heldPins = held.getItem() instanceof ItemKeyPin ? ItemKeyPin.getPins(held) : 0;
        boolean ok = canAccess(heldPins, held.getItem() instanceof ItemKey);
        if (ok && level != null) {
            level.playSound(null, player.blockPosition(), HBMSoundHandler.lockOpen.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ok;
    }

    @Override
    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public void lock() {
        // CE TileEntityLockableBase.java:38-42 (pin-zero logger omitted)
        if (!isLocked) {
            isLocked = true;
            dataChanged();
            setChanged();
        }
    }

    @Override
    public void unlock() {
        isLocked = false;
        setChanged();
    }

    @Override
    public void setPins(int pins) {
        if (lock != pins) {
            lock = pins;
            dataChanged();
            setChanged();
        }
    }

    @Override
    public int getPins() {
        return lock;
    }

    @Override
    public void setMod(double mod) {
        if (lockMod != mod) {
            lockMod = mod;
            dataChanged();
            setChanged();
        }
    }

    @Override
    public double getMod() {
        return lockMod;
    }

    @Override
    public boolean isCheesable() {
        return cheesable;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("stack", stack);
        tag.putBoolean("output", output);
        tag.putInt("capacity", capacity);
        tag.putByte("redstone", (byte) redstone);
        // CE TileEntityLockableBase.java:83-88
        tag.putInt("lock", lock);
        tag.putBoolean("cheesable", cheesable);
        tag.putBoolean("isLocked", isLocked);
        tag.putDouble("lockMod", lockMod);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stack = tag.getInt("stack");
        output = tag.getBoolean("output");
        capacity = tag.getInt("capacity");
        redstone = tag.getByte("redstone");
        if (capacity <= 0) capacity = 10_000;
        // CE TileEntityLockableBase.java:77-80
        lock = tag.getInt("lock");
        cheesable = !tag.contains("cheesable") || tag.getBoolean("cheesable");
        isLocked = tag.getBoolean("isLocked");
        lockMod = tag.contains("lockMod") ? tag.getDouble("lockMod") : 0.1D;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(getStockpile());
        buf.writeBoolean(output);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, inventory.getStackInSlot(SLOT_FILTER));
        buf.writeInt(capacity);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        stack = buf.readInt();
        output = buf.readBoolean();
        ItemStack type = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!type.isEmpty()) inventory.setStackInSlot(SLOT_FILTER, type);
        capacity = buf.readInt();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        CompoundTag data = new CompoundTag();
        if (level != null) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack slot = inventory.getStackInSlot(i);
                if (slot.isEmpty()) continue;
                data.put("slot" + i, slot.save(level.registryAccess(), new CompoundTag()));
            }
        }
        data.putInt("stack", stack);
        data.putBoolean("output", output);
        data.putInt("capacity", capacity);
        // CE BlockMassStorage.java:140-145 — persist pins only when locked
        if (isLocked()) {
            data.putInt("lock", getPins());
            data.putDouble("lockMod", getMod());
        }
        nbt.put(NBT_PERSISTENT_KEY, data);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        CompoundTag data = nbt.contains(NBT_PERSISTENT_KEY) ? nbt.getCompound(NBT_PERSISTENT_KEY) : nbt;
        if (level != null) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                String key = "slot" + i;
                if (data.contains(key)) {
                    inventory.setStackInSlot(i, ItemStack.parseOptional(level.registryAccess(), data.getCompound(key)));
                }
            }
        }
        if (data.contains("stack")) stack = data.getInt("stack");
        if (data.contains("output")) output = data.getBoolean("output");
        if (data.contains("capacity") && data.getInt("capacity") > 0) capacity = data.getInt("capacity");
        // CE BlockMassStorage.java:179-185
        if (data.contains("lock")) {
            setPins(data.getInt("lock"));
            setMod(data.contains("lockMod") ? data.getDouble("lockMod") : 0.1D);
            lock();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MassStorageMenu(id, inv, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :293-295
        return new String[]{
                PREFIX_VALUE + "type", PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent",
                PREFIX_FUNCTION + "toggleoutput"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :298-306
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + this.stack;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + this.stack * 100 / this.capacity;
        if ((PREFIX_VALUE + "type").equals(name)) {
            if (inventory.getStackInSlot(SLOT_FILTER).isEmpty()) return "None";
            return inventory.getStackInSlot(SLOT_FILTER).getHoverName().getString();
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :310-316
        if ((PREFIX_FUNCTION + "toggleoutput").equals(name)) {
            this.output = !this.output;
            setChanged();
        }
        return null;
    }
}
