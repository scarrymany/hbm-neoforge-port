package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.ElectrolyserBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** CE {@code ContainerElectrolyserFluid}: battery / upgrades / fluid-id+canister 3-10 / byproducts 11-13. */
public class ElectrolyserMenu extends MenuBase<ElectrolyserBlockEntity> {

    public ElectrolyserMenu(int id, Inventory playerInv, ElectrolyserBlockEntity be) {
        super(ChemIsotopeMenus.ELECTROLYSER.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 186, 109));
        this.addSlot(new SlotNonRetarded(tile, 1, 186, 140));
        this.addSlot(new SlotNonRetarded(tile, 2, 186, 158));
        this.addSlot(new SlotNonRetarded(tile, 3, 6, 18));
        this.addSlot(new SlotTakeOnly(tile, 4, 6, 54));
        this.addSlot(new SlotNonRetarded(tile, 5, 24, 18));
        this.addSlot(new SlotTakeOnly(tile, 6, 24, 54));
        this.addSlot(new SlotNonRetarded(tile, 7, 78, 18));
        this.addSlot(new SlotTakeOnly(tile, 8, 78, 54));
        this.addSlot(new SlotNonRetarded(tile, 9, 134, 18));
        this.addSlot(new SlotTakeOnly(tile, 10, 134, 54));
        this.addSlot(new SlotTakeOnly(tile, 11, 154, 18));
        this.addSlot(new SlotTakeOnly(tile, 12, 154, 36));
        this.addSlot(new SlotTakeOnly(tile, 13, 154, 54));

        playerInv(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        int machineSlots = this.slots.size() - 36;
        if (!slot.hasItem()) return newStack;
        ItemStack stack = slot.getItem();
        newStack = stack.copy();
        if (index < machineSlots) {
            if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == newStack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return newStack;
    }
}
