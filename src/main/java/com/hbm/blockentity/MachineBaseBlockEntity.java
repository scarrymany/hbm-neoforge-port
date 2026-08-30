package com.hbm.blockentity;

import com.hbm.api.tile.IWorldRenameable;
import com.hbm.capability.NTMEnergyCapabilityWrapper;
import com.hbm.capability.NTMFluidHandlerWrapper;
import com.hbm.lib.CapabilityContextProvider;
import com.hbm.lib.ItemStackHandlerWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Inventoried-machine base, ported from CE's {@code com.hbm.tileentity.TileEntityMachineBase} (385
 * lines, read in full). CE's own file-level annotation reads: "Not spaghetti in itself, but for the
 * love of god please use this base class for all machines" - the same intent carries over here:
 * every future concrete machine block entity in {@code com.hbm.blockentity.machine.**} should
 * extend this class rather than {@link LoadedBaseBlockEntity} directly, unless it genuinely has no
 * inventory (CE's own split: 111 direct subclasses of {@code TileEntityMachineBase} vs. 80 of
 * {@code TileEntityLoadedBase} alone).
 *
 * <p>Owns: an {@link ItemStackHandler} inventory built via the overridable
 * {@link #getNewInventory(int, int)} factory (auto-{@link #setChanged()} on content change) plus
 * {@link #resizeInventory(int)}; a {@link #getCheckedInventory()} wrapper for future Container/GUI
 * use that re-validates through {@link #isItemValidForSlot}; capability-exposure methods (see
 * below) gated by two constructor-time booleans ({@link #enableFluidWrapper},
 * {@link #enableEnergyWrapper}) with per-accessor-position wrapper caching, matching CE's
 * multiblock-proxy-safe caching contract exactly (see the cache fields' javadoc); per-slot
 * accessibility/insert/extract hooks; custom-name plumbing; and inventory NBT round-trip via
 * {@link #saveAdditional}/{@link #loadAdditional}.
 *
 * <h2>Capability exposure - the one genuine API-shape difference from CE, not a port choice</h2>
 * CE's {@code TileEntityMachineBase} exposes capabilities via a per-instance
 * {@code getCapability(Capability<T>, EnumFacing)}/{@code hasCapability} override pair - Forge
 * 1.12's actual mechanism. <b>NeoForge 1.21.1 has no such override point on {@code BlockEntity} at
 * all</b>: capabilities are registered once per {@code BlockEntityType} at mod-init time via
 * {@code RegisterCapabilitiesEvent.registerBlockEntity(capability, type, provider)} (already used,
 * item-side only so far, by this port's own {@code com.hbm.capability.ModCapabilities.register}).
 * This is confirmed, not inferred - see {@code docs/phase2/blockentity_base.md}'s "Key design
 * decisions" section, itself cross-checked against real NeoForge usage.
 * <p>
 * So instead of {@code @Override getCapability}/{@code hasCapability}, this class exposes four
 * plain accessor method pairs with the exact same body CE's override had per capability branch -
 * {@link #getItemHandlerCapability(Direction)}/{@link #hasItemHandlerCapability(Direction)},
 * {@link #getFluidHandlerCapability(Direction)}/{@link #hasFluidHandlerCapability()}, and
 * {@link #getEnergyStorageCapability(Direction)}/{@link #hasEnergyStorageCapability()} - each ready
 * to be handed directly to a future {@code registerBlockEntity} provider lambda once a concrete
 * machine {@code BlockEntityType} exists to register them against:
 * <pre>{@code
 * event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MY_MACHINE_TYPE.get(),
 *         (be, side) -> be.getItemHandlerCapability(side));
 * event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MY_MACHINE_TYPE.get(),
 *         (be, side) -> be.getEnergyStorageCapability(side));
 * event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, MY_MACHINE_TYPE.get(),
 *         (be, side) -> be.getFluidHandlerCapability(side));
 * }</pre>
 * The {@code side == null} "internal access" contract from CE's own {@code getCapability} javadoc
 * ("Contract: facing == null -&gt; internal") is preserved exactly in each accessor.
 */
public abstract class MachineBaseBlockEntity extends LoadedBaseBlockEntity implements IWorldRenameable {

    /**
     * Internal inventory. All operations are unchecked.
     * Use {@link #getCheckedInventory()} for Container/External classes.
     */
    public ItemStackHandler inventory;
    private IItemHandlerModifiable checkedInventory;

    // Capability wrappers handed out by the capability accessor methods below, cached instead of
    // allocated fresh every call: external capability consumers that key a cache off the handler
    // object's identity (AE2's storage buses do exactly this) would otherwise see a "new" handler on
    // every single query and tear down/rebuild their own cache in response, every time. Both caches
    // key on the accessorPos CapabilityContextProvider resolves, NOT on this.pos: a future proxy
    // block entity pushes its OWN position before delegating to a multiblock core's accessor methods
    // (see CapabilityContextProvider), which is the whole point of the mechanism - it is how a
    // multiblock tells its ports apart. So accessorPos is part of the cache key, or the first port to
    // query poisons the entry for every other port of the same multiblock. The item wrapper cache
    // additionally keys on facing.
    // NOTE: this assumes getAccessibleSlotsFromSide(side, accessorPos) is stable for a given (facing,
    // accessorPos) pair over the block entity's lifetime (every override in this class derives its
    // answer only from those two arguments and the machine's placement). If a subclass keys accessible
    // slots off other *mutable* per-instance state (a runtime I/O config toggle, etc.), that subclass
    // must invalidate itemWrapperCache when that state changes, or external capability holders (AE2
    // buses, hoppers, ...) will keep using stale slot data until the chunk unloads and the block
    // entity is recreated.
    private final Map<BlockPos, NTMFluidHandlerWrapper> fluidWrapperCache = new HashMap<>();
    private final Map<Direction, Map<BlockPos, IItemHandlerModifiable>> itemWrapperCache = new EnumMap<>(Direction.class);

    /**
     * {@code enableFluidWrapper}/{@code enableEnergyWrapper} only control whether
     * {@link #getFluidHandlerCapability}/{@link #getEnergyStorageCapability} return a wrapper at
     * all - they do not, by themselves, make the wrapper work. {@link NTMFluidHandlerWrapper}'s
     * constructor requires {@code this} to implement {@code IFluidReceiverMK2}/
     * {@code IFluidProviderMK2} and unconditionally casts it to {@code IFluidUserMK2}; likewise
     * {@link NTMEnergyCapabilityWrapper} requires {@code IEnergyHandlerMK2}. A concrete machine that
     * sets either flag {@code true} MUST also implement the matching marker interface(s) itself, or
     * the capability accessor throws at first use - exactly CE's own contract, just deferred from
     * "the base class checks a private boolean and constructs a wrapper that happens to downcast
     * {@code this}" (Forge 1.12) to the same statement here, unchanged.
     */
    private final boolean enableFluidWrapper;
    private final boolean enableEnergyWrapper;

    @Nullable
    private Component customName;
    private boolean destroyedByCreativePlayer = false;

    @Deprecated
    public MachineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount) {
        this(type, pos, state, scount, 64);
    }

    @Deprecated
    public MachineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount, int slotlimit) {
        this(type, pos, state, scount, slotlimit, false, false);
    }

    public MachineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount,
                                   boolean enableFluidWrapper, boolean enableEnergyWrapper) {
        this(type, pos, state, scount, 64, enableFluidWrapper, enableEnergyWrapper);
    }

    public MachineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount, int slotlimit,
                                   boolean enableFluidWrapper, boolean enableEnergyWrapper) {
        super(type, pos, state);
        this.inventory = getNewInventory(scount, slotlimit);
        this.enableFluidWrapper = enableFluidWrapper;
        this.enableEnergyWrapper = enableEnergyWrapper;
    }

    /**
     * Overridable inventory factory, ported unchanged from CE. {@code onContentsChanged} calls
     * {@link #setChanged()} (CE's {@code markDirty()}) so any content mutation - including through
     * the raw {@link #inventory} field, not just the capability wrappers below - flags the block
     * entity for saving.
     */
    protected ItemStackHandler getNewInventory(int scount, int slotlimit) {
        return new ItemStackHandler(scount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }

            @Override
            public int getSlotLimit(int slot) {
                return slotlimit;
            }
        };
    }

    protected void resizeInventory(int newSlotCount) {
        ItemStackHandler newInventory = getNewInventory(newSlotCount, inventory.getSlotLimit(0));
        for (int i = 0; i < Math.min(inventory.getSlots(), newSlotCount); i++) {
            newInventory.setStackInSlot(i, inventory.getStackInSlot(i));
        }
        this.inventory = newInventory;
        setChanged();
    }

    @Override
    public Component getName() {
        return hasCustomName() ? customName : getDefaultName();
    }

    protected abstract Component getDefaultName();

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public boolean hasCustomName() {
        return customName != null;
    }

    @Override
    public void setCustomName(Component name) {
        this.customName = name;
    }

    /**
     * CE checked {@code world.getTileEntity(pos) != this} plus a 128-block-squared distance bound.
     * Ported onto the vanilla {@link Container#stillValidBlockEntity} helper instead (same real-BE
     * check, plus vanilla's own standard 8-block/64-squared distance bound rather than CE's 128) -
     * matches both this port's own {@code RadioTorchBaseBlockEntity} cross-check and Neo Edition's
     * real {@code MachineBaseBlockEntity.stillValid}. No Menu/Container framework exists yet to call
     * this (see {@code docs/phase2/blockentity_base.md} deferred scope), it is ready for one.
     */
    public boolean isUseableByPlayer(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    /**
     * It mimics the 1.7 IConditionalInvAccess behavior.
     *
     * @param side        The side of the block being accessed.
     * @param accessorPos The position of the block DOING the accessing (the proxy).
     * @return An array of slots accessible from this proxy at this side. null -> full access. Empty array -> no access.
     */
    public int[] getAccessibleSlotsFromSide(Direction side, BlockPos accessorPos) {
        return getAccessibleSlotsFromSide(side);
    }

    /**
     * @return An array of slots accessible at this side. null -> full access. Empty array -> no access.
     */
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{};
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(muffled);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        this.muffled = buf.readBoolean();
    }

    /** No-op placeholder for future GUI button packets, ported unchanged from CE. */
    public void handleButtonPacket(int value, int meta) {
    }

    /**
     * {@code ItemStackHandler.serializeNBT}/{@code deserializeNBT} are assumed here to take a
     * {@link HolderLookup.Provider} argument, per NeoForge's post-1.20.5 {@code INBTSerializable<T>}
     * refactor (every {@code ItemStack}-bearing NBT read/write needs registry access since that
     * version, the same reason {@link ItemStack#save} itself now requires one) - inferred from
     * well-documented NeoForge convention, same as this port's own {@code ItemInventory}'s flagged
     * uncertainty about {@code ItemStackHandler} generally (no live decompiled-source confirmation
     * was reachable in this sandbox; double check against the real class on first build).
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            int expected = inventory.getSlots();
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
            if (inventory.getSlots() < expected) resizeInventory(expected);
        }
    }

    /**
     * Checks if an item can be inserted into a slot.
     * <p>
     * Only affects the {@link IItemHandlerModifiable} obtained via {@link #getCheckedInventory()}
     * and the capability exposed externally.
     */
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return true;
    }

    /**
     * Checks if an item can be inserted into a slot from a specific side and accessor position.
     * Mimics the 1.7 IConditionalInvAccess behavior.
     * <p>
     * Only affects the capability exposed externally.
     */
    public boolean canInsertItem(int slot, ItemStack stack, Direction side, BlockPos accessorPos) {
        return canInsertItem(slot, stack);
    }

    /**
     * Only affects the capability exposed externally.
     */
    public boolean canInsertItem(int slot, ItemStack itemStack) {
        return this.isItemValidForSlot(slot, itemStack);
    }

    /**
     * Checks if an item can be extracted from a slot from a specific side and accessor position.
     * Mimics the 1.7 IConditionalInvAccess behavior.
     * <p>
     * Only affects the capability exposed externally.
     */
    public boolean canExtractItem(int slot, ItemStack stack, int amount, Direction side, BlockPos accessorPos) {
        return canExtractItem(slot, stack, amount);
    }

    /**
     * Only affects the capability exposed externally.
     */
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return true;
    }

    /**
     * Whether the given neighboring block counts as a muffler for {@link #countMufflers()}.
     * Defaults to false: no muffler block has been ported to {@code ModBlocks} yet (CE's own
     * {@code ModBlocks.muffler} field has no equivalent in this port as of this package landing).
     * Once one exists, override this - every machine shares the same check, so a single override
     * point here (or a direct reference once the block exists) is enough for all of them.
     */
    protected boolean isMufflerBlock(BlockState state) {
        return false;
    }

    public int countMufflers() {
        if (level == null) return 0;
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (isMufflerBlock(level.getBlockState(worldPosition.relative(dir)))) count++;
        }
        return count;
    }

    public float getVolume(int toSilence) {
        float volume = 1 - (countMufflers() / (float) toSilence);
        return Math.max(volume, 0);
    }

    /**
     * @return a checked wrapper around the inventory. Intended for future Container and GUI use.
     */
    public IItemHandlerModifiable getCheckedInventory() {
        if (checkedInventory == null) checkedInventory = new CheckedInventory();
        return checkedInventory;
    }

    /**
     * Item-handler capability accessor. {@code side == null} means internal access and returns the
     * raw {@link #inventory} directly (no wrapper, matching CE exactly); external access
     * (non-null {@code side}) resolves the accessor position via
     * {@link CapabilityContextProvider#getAccessor} and returns a per-(side, accessor) cached
     * {@link ItemStackHandlerWrapper} that routes every operation through
     * {@link #canInsertItem(int, ItemStack, Direction, BlockPos)}/
     * {@link #canExtractItem(int, ItemStack, int, Direction, BlockPos)}.
     */
    @Nullable
    public IItemHandlerModifiable getItemHandlerCapability(@Nullable Direction side) {
        if (inventory == null) return null;
        if (side == null) return inventory;

        BlockPos accessorPos = CapabilityContextProvider.getAccessor(this.worldPosition);
        Map<BlockPos, IItemHandlerModifiable> perAccessor = itemWrapperCache.computeIfAbsent(side, f -> new HashMap<>());
        IItemHandlerModifiable cached = perAccessor.get(accessorPos);
        if (cached != null) return cached;

        int[] accessibleSlots = getAccessibleSlotsFromSide(side, accessorPos);
        IItemHandlerModifiable wrapper = new ItemStackHandlerWrapper(inventory, accessibleSlots) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return super.isItemValid(slot, stack) && canInsertItem(slot, stack, side, accessorPos);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (canExtractItem(slot, inventory.getStackInSlot(slot), amount, side, accessorPos)) {
                    return super.extractItem(slot, amount, simulate);
                }
                return ItemStack.EMPTY;
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (canInsertItem(slot, stack, side, accessorPos)) {
                    return super.insertItem(slot, stack, simulate);
                }
                return stack;
            }
        };
        perAccessor.put(accessorPos, wrapper);
        return wrapper;
    }

    public boolean hasItemHandlerCapability(@Nullable Direction side) {
        if (inventory == null) return false;
        if (side == null) return true;
        BlockPos accessorPos = CapabilityContextProvider.getAccessor(this.worldPosition);
        int[] accessible = getAccessibleSlotsFromSide(side, accessorPos);
        return accessible == null || accessible.length > 0;
    }

    /**
     * Fluid-handler capability accessor, gated by {@link #enableFluidWrapper}. {@code side == null}
     * returns a fresh, uncached internal-access wrapper (matching CE); external access is cached by
     * accessor position only (not by side - CE's own cache has no facing dimension for fluids).
     */
    @Nullable
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        if (!enableFluidWrapper) return null;
        if (side == null) return new NTMFluidHandlerWrapper(this, null);
        BlockPos accessorPos = CapabilityContextProvider.getAccessor(this.worldPosition);
        return fluidWrapperCache.computeIfAbsent(accessorPos, acc -> new NTMFluidHandlerWrapper(this, acc));
    }

    public boolean hasFluidHandlerCapability() {
        return enableFluidWrapper;
    }

    /**
     * Energy-storage capability accessor, gated by {@link #enableEnergyWrapper}. Unlike the item
     * and fluid accessors above, CE never caches this one - a fresh {@link NTMEnergyCapabilityWrapper}
     * is constructed per query, ported unchanged.
     */
    @Nullable
    public IEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        if (!enableEnergyWrapper) return null;
        BlockPos accessorPos = side == null ? null : CapabilityContextProvider.getAccessor(this.worldPosition);
        return new NTMEnergyCapabilityWrapper(this, accessorPos);
    }

    public boolean hasEnergyStorageCapability() {
        return enableEnergyWrapper;
    }

    public void setDestroyedByCreativePlayer() {
        destroyedByCreativePlayer = true;
    }

    public boolean isDestroyedByCreativePlayer() {
        return destroyedByCreativePlayer;
    }

    private final class CheckedInventory implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            if (!isItemValidForSlot(slot, stack)) return stack;
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) return ItemStack.EMPTY;
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            inventory.setStackInSlot(slot, stack);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isItemValidForSlot(slot, stack);
        }
    }
}
