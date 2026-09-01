package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MassStorageBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** CE {@code ContainerMassStorage}: in / filter / out + ghost filter click. */
public class MassStorageMenu extends MenuBase<MassStorageBlockEntity> {

    public MassStorageMenu(int id, Inventory playerInv, MassStorageBlockEntity be) {
        super(DummyableProcessMenus.MASS_STORAGE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, MassStorageBlockEntity.SLOT_IN, 61, 17));
        this.addSlot(new SlotNonRetarded(tile, MassStorageBlockEntity.SLOT_FILTER, 61, 53));
        this.addSlot(new SlotTakeOnly(tile, MassStorageBlockEntity.SLOT_OUT, 61, 89));
        playerInv(playerInv, 8, 139, 197);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);

        if (index == MassStorageBlockEntity.SLOT_OUT && slot != null && !slot.hasItem()) {
            ItemStack extracted = be.quickExtract();
            if (!extracted.isEmpty()) {
                slot.set(extracted);
            }
        }

        if (index >= 3 && slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            if (be.quickInsert(stack)) {
                ItemStack result = stack.copy();
                slot.set(ItemStack.EMPTY);
                slot.onTake(player, stack);
                return result;
            }
        }

        return super.quickMoveStack(player, index);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId != MassStorageBlockEntity.SLOT_FILTER) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        Slot slot = this.getSlot(slotId);
        if (be.getStockpile() > 0) return;

        ItemStack held = this.getCarried();
        slot.set(!held.isEmpty() ? held.copy() : ItemStack.EMPTY);
        if (slot.hasItem()) {
            slot.getItem().setCount(1);
        }
        slot.setChanged();
        this.broadcastChanges();
    }
}
