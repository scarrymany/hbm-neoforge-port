package com.hbm.entity.cart;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.entity.cart.EntityMinecartContainerBase} (154 lines) - adds a plain
 * NBT-backed inventory to a cart. Implements vanilla's direct successor to CE's 1.12 {@code
 * IInventory}, {@link Container}, matching the exact same substitution this port's own sibling
 * rail-car package already made for the identical CE shape (see
 * {@code com.hbm.entity.train.EntityRailCarCargo}'s javadoc) - {@code getField}/{@code setField}/
 * {@code getFieldCount} (always-0 1.12 {@code IInventory} boilerplate) and CE's own hand-rolled
 * {@code hasCustomName}/{@code getName}/{@code getDisplayName} (superseded by vanilla {@code Entity}'s
 * own real {@link net.minecraft.world.entity.Nameable} implementation) are dropped for the same
 * reasons that report gives.
 */
public abstract class EntityMinecartContainerBase extends EntityMinecartNTM implements Container {

    protected NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

    protected EntityMinecartContainerBase(EntityType<? extends EntityMinecartContainerBase> type, Level level) {
        super(type, level);
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

    /** CE: {@code markDirty() { }} - a no-op stub in CE too. */
    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return this.isAlive() && player.distanceToSqr(this) <= 64.0D;
    }

    /** CE: {@code isItemValidForSlot(int, ItemStack) { return true; }} - overridden to {@code false}
     *  by {@link EntityMinecartDestroyer} (its 18 slots are a read-only filter template, see that
     *  class's own javadoc). */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
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
