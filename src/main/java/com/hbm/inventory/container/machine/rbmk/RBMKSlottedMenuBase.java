package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKSlottedBlockEntity;
import com.hbm.inventory.slot.SlotCraftingOutput;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * {@link AbstractContainerMenu} base for RBMK column TEs, mirroring
 * {@link com.hbm.inventory.container.MenuBase} field-for-field but bound to
 * {@link RBMKSlottedBlockEntity} instead of {@code MachineBaseBlockEntity} - the two block-entity
 * hierarchies are siblings, not related by inheritance (see {@code RBMKSlottedBlockEntity}'s own
 * javadoc on why), so the shared Menu base cannot be reused directly. Every method here is a direct
 * copy of {@code MenuBase}'s, retyped.
 *
 * @param <T> the concrete RBMK column block entity this menu is opened against.
 */
public abstract class RBMKSlottedMenuBase<T extends RBMKSlottedBlockEntity> extends AbstractContainerMenu {

    public final T be;
    protected final IItemHandlerModifiable tile;

    protected RBMKSlottedMenuBase(MenuType<?> menuType, int id, T be) {
        super(menuType, id);
        this.be = be;
        this.tile = be.getCheckedInventory();
    }

    @Override
    public boolean stillValid(Player player) {
        return be.isUseableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        int beSlotCount = tile.getSlots();

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            newStack = stack.copy();

            if (index < beSlotCount) {
                if (!this.moveItemStackTo(stack, beSlotCount, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, beSlotCount, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == newStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }

        return newStack;
    }

    public void playerInv(Inventory inventory, int playerInvX, int playerInvY) {
        this.playerInv(inventory, playerInvX, playerInvY, playerInvY + 58);
    }

    public void playerInv(Inventory inventory, int playerInvX, int playerInvY, int playerHotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, playerInvX + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, playerInvX + col * 18, playerHotbarY));
        }
    }

    public void addSlots(IItemHandler inv, int from, int x, int y, int rows, int cols) {
        addSlots(inv, from, x, y, rows, cols, 18);
    }

    public void addSlots(IItemHandler inv, int from, int x, int y, int rows, int cols, int slotSize) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.addSlot(new SlotNonRetarded(inv, col + row * cols + from, x + col * slotSize, y + row * slotSize));
            }
        }
    }

    public void addOutputSlots(Player player, IItemHandler inv, int from, int x, int y, int rows, int cols) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.addSlot(new SlotCraftingOutput(player, inv, col + row * cols + from, x + col * 18, y + row * 18));
            }
        }
    }

    public void addTakeOnlySlots(IItemHandler inv, int from, int x, int y, int rows, int cols) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.addSlot(new SlotTakeOnly(inv, col + row * cols + from, x + col * 18, y + row * 18));
            }
        }
    }
}
