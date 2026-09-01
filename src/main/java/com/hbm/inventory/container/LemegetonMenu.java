package com.hbm.inventory.container;

import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.items.tool.ToolItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * CE {@code ContainerLemegeton}: 1-slot craft matrix + result. Opens from {@code book_lemegeton}.
 */
public class LemegetonMenu extends AbstractContainerMenu {

    private final TransientCraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final Player player;

    public LemegetonMenu(int id, Inventory playerInventory) {
        super(ModMenuTypes.LEMEGETON.get(), id);
        this.player = playerInventory.player;
        this.craftSlots = new TransientCraftingContainer(this, 1, 1);
        this.resultSlots = new ResultContainer();

        this.addSlot(new Slot(this.resultSlots, 0, 107, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                ItemStack in = craftSlots.getItem(0);
                if (!in.isEmpty()) {
                    in.shrink(1);
                    craftSlots.setChanged();
                }
                slotsChanged(craftSlots);
                super.onTake(player, stack);
            }
        });
        this.addSlot(new Slot(this.craftSlots, 0, 49, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        slotsChanged(this.craftSlots);
    }

    public static LemegetonMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new LemegetonMenu(id, playerInventory);
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        this.resultSlots.setItem(0, LemegetonRecipes.getRecipe(this.craftSlots.getItem(0)));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.craftSlots);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().hasAnyMatching(stack -> stack.is(ToolItems.BOOK_LEMEGETON.get()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index <= 1) {
                if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, copy);
            } else if (!this.moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }
}
