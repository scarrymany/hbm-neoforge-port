package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.ElectrolyserBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** CE {@code ContainerElectrolyserMetal}: battery, upgrades, crystal 14, outputs 15-20. */
public class ElectrolyserMetalMenu extends MenuBase<ElectrolyserBlockEntity> {

    public ElectrolyserMetalMenu(int id, Inventory playerInv, ElectrolyserBlockEntity be) {
        super(ChemIsotopeMenus.ELECTROLYSER_METAL.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 186, 109));
        this.addSlot(new SlotNonRetarded(tile, 1, 186, 140));
        this.addSlot(new SlotNonRetarded(tile, 2, 186, 158));
        this.addSlot(new SlotNonRetarded(tile, 14, 10, 22));
        this.addSlot(new SlotTakeOnly(tile, 15, 136, 18));
        this.addSlot(new SlotTakeOnly(tile, 16, 154, 18));
        this.addSlot(new SlotTakeOnly(tile, 17, 136, 36));
        this.addSlot(new SlotTakeOnly(tile, 18, 154, 36));
        this.addSlot(new SlotTakeOnly(tile, 19, 136, 54));
        this.addSlot(new SlotTakeOnly(tile, 20, 154, 54));

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
