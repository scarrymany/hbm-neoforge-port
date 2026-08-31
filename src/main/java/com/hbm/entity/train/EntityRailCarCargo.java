package com.hbm.entity.train;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.entity.train.EntityRailCarCargo} (202 lines) - adds a plain
 * NBT-backed inventory to a rail car. CE implements 1.12's {@code IInventory}; this port implements
 * vanilla's direct 1:1 modern successor, {@link Container} (rather than the
 * {@code ItemStackHandler}/{@link net.neoforged.neoforge.items.IItemHandler} shape this port's
 * machine-block-entity framework uses - see {@code TrainCargoTram}'s own javadoc "Why vanilla
 * {@code Container}, not {@code IItemHandler}" section for why that framework's own
 * {@code MenuBase<T extends MachineBaseBlockEntity>} cannot back an entity GUI regardless, so there is
 * no reuse pressure toward matching it here). {@code Container}-backed vanilla {@link
 * net.minecraft.world.inventory.Slot}s are what {@code TrainCargoTram}/{@code TrainCargoTramTrailer}'s
 * menus use, exactly mirroring how CE's own {@code Slot(train, i, x, y)} calls worked directly against
 * an {@code IInventory}.
 * <p>
 * CE's {@code getField}/{@code setField}/{@code getFieldCount} (always 0, unused - a piece of 1.12
 * {@code IInventory} boilerplate every implementer had to stub) has no equivalent in vanilla's
 * {@link Container} interface at all and is not ported - it did nothing in CE either. CE's own
 * {@code hasCustomName}/{@code getName}/{@code getDisplayName}/{@code setEntityName} overrides are
 * also not re-ported: vanilla {@link net.minecraft.world.entity.Entity} already implements
 * {@link net.minecraft.world.entity.Nameable} with its own real custom-name system
 * ({@code setCustomName}/{@code getCustomName}/{@code hasCustomName}/{@code getName}/
 * {@code getDisplayName}), which is the correct, non-duplicated place for this - CE only rolled its
 * own copy because 1.12's {@code IInventory} carried naming methods that {@code Entity} itself did not.
 */
public abstract class EntityRailCarCargo extends EntityRailCarBase implements Container {

    protected static final EntityDataAccessor<Integer> OCCUPIED_SLOTS =
            SynchedEntityData.defineId(EntityRailCarCargo.class, EntityDataSerializers.INT);

    protected NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

    protected EntityRailCarCargo(EntityType<? extends EntityRailCarCargo> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OCCUPIED_SLOTS, 0);
    }

    public int countOccupiedSlots() {
        int count = 0;

        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!this.getItem(i).isEmpty()) count++;
        }

        return count;
    }

    public int getOccupiedSlots() {
        return this.entityData.get(OCCUPIED_SLOTS);
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);

        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
    }

    /** CE: {@code markDirty()} - a no-op stub in CE too (see class javadoc); {@link #OCCUPIED_SLOTS}
     * is instead resynced unconditionally every tick in {@link #tick()}, exactly matching CE's own
     * {@code onUpdate()}-driven resync rather than a reactive dirty-flag one. */
    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return this.isAlive() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) this.entityData.set(OCCUPIED_SLOTS, this.countOccupiedSlots());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ContainerHelper.saveAllItems(tag, this.items, this.registryAccess());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, this.registryAccess());
    }
}
