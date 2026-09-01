package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineAutocrafterBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** CE {@code ContainerAutocrafter}: ghost template 0-9 + recipe 10-19 + battery 20. */
public class AutocrafterMenu extends MenuBase<MachineAutocrafterBlockEntity> {

    public AutocrafterMenu(int id, Inventory playerInv, MachineAutocrafterBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_AUTOCRAFTER.get(), id, be);
        this.addSlots(tile, 0, 44, 22, 3, 3);
        this.addSlot(new SlotTakeOnly(tile, 9, 116, 40));
        this.addSlots(tile, 10, 44, 86, 3, 3);
        this.addSlot(new SlotTakeOnly(tile, 19, 116, 104));
        this.addSlot(new SlotNonRetarded(tile, 20, 17, 99));
        playerInv(playerInv, 8, 158);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId > 9) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        Slot slot = this.getSlot(slotId);
        if (slotId == 9) {
            if (button == 1 && clickType == ClickType.PICKUP && slot.hasItem()) be.nextTemplate();
            return;
        }
        if (button == 1 && clickType == ClickType.PICKUP && slot.hasItem()) {
            be.nextMode(slotId);
            return;
        }
        ItemStack stamp = this.getCarried().isEmpty() ? ItemStack.EMPTY : this.getCarried().copyWithCount(1);
        be.getCheckedInventory().setStackInSlot(slotId, stamp);
        slot.setChanged();
        be.initPattern(stamp, slotId);
        be.updateTemplateGrid();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= 10 && index <= 20) return super.quickMoveStack(player, index);
        return ItemStack.EMPTY;
    }
}
