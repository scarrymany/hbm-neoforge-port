package com.hbm.inventory.container;

import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * CE {@code ContainerAnvil.java}:11-164. 2 smithing in + 1 out, no BE. Tier comes from the anvil block.
 */
public class AnvilMenu extends AbstractContainerMenu {

    public final int tier;
    private final SimpleContainer input = new SimpleContainer(2);
    private final ResultContainer output = new ResultContainer();

    public AnvilMenu(int id, Inventory playerInv, int tier) {
        super(AnvilMenus.ANVIL.get(), id);
        this.tier = tier;

        this.addSlot(new SmithingSlot(input, 0, 17, 27));
        this.addSlot(new SmithingSlot(input, 1, 53, 27));
        this.addSlot(new Slot(output, 0, 89, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                ItemStack left = input.getItem(0);
                ItemStack right = input.getItem(1);
                if (left.isEmpty() || right.isEmpty()) {
                    super.onTake(player, stack);
                    return;
                }
                for (AnvilSmithingRecipe rec : AnvilRecipes.getSmithing()) {
                    int i = rec.matchesInt(left, right);
                    if (i != -1) {
                        input.removeItem(0, rec.amountConsumed(0, i == 1));
                        input.removeItem(1, rec.amountConsumed(1, i == 1));
                        updateSmithing();
                        super.onTake(player, stack);
                        return;
                    }
                }
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + 56));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142 + 56));
        }
        updateSmithing();
    }

    public static AnvilMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new AnvilMenu(id, inv, buf.readVarInt());
    }

    void updateSmithing() {
        ItemStack left = input.getItem(0);
        ItemStack right = input.getItem(1);
        if (left.isEmpty() || right.isEmpty()) {
            output.setItem(0, ItemStack.EMPTY);
            return;
        }
        for (AnvilSmithingRecipe rec : AnvilRecipes.getSmithing()) {
            if (rec.matches(left, right) && rec.tier <= this.tier) {
                output.setItem(0, rec.getOutput(left, right));
                return;
            }
        }
        output.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.input);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index == 2) {
                if (!this.moveItemStackTo(stack, 3, this.slots.size(), true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, copy);
            } else if (index <= 1) {
                if (!this.moveItemStackTo(stack, 3, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return copy;
    }

    private final class SmithingSlot extends Slot {
        SmithingSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            AnvilMenu.this.updateSmithing();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack stack = super.remove(amount);
            AnvilMenu.this.updateSmithing();
            return stack;
        }
    }
}
