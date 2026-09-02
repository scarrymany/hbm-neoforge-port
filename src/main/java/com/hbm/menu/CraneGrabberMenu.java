package com.hbm.menu;

import com.hbm.blockentity.network.CraneGrabberBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * NeoForge port of CE's {@code ContainerCraneGrabber} - 9 filter + 2 upgrade slots.
 */
public class CraneGrabberMenu extends AbstractContainerMenu {

    private final CraneGrabberBlockEntity blockEntity;

    public CraneGrabberMenu(int containerId, Inventory playerInventory, CraneGrabberBlockEntity blockEntity) {
        super(com.hbm.inventory.container.ModMenuTypes.CRANE_GRABBER.get(), containerId);
        this.blockEntity = blockEntity;

        // 9 filter slots (3x3) at (40, 17)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new SlotItemHandler(blockEntity.getInventory(), col + row * 3, 40 + col * 18, 17 + row * 18));
            }
        }

        // 2 upgrade slots at (121, 23) and (121, 47)
        this.addSlot(new SlotItemHandler(blockEntity.getInventory(), 9, 121, 23));
        this.addSlot(new SlotItemHandler(blockEntity.getInventory(), 10, 121, 47));

        // Player inventory (3x9) at (8, 103)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }

        // Player hotbar at (8, 161)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161));
        }
    }

    // Client constructor
    public CraneGrabberMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (CraneGrabberBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // Slots 0-8: filter (don't shift)
            if (index < 9) {
                return ItemStack.EMPTY;
            }

            int inventoryEnd = 11; // 9 filter + 2 upgrade

            if (index < inventoryEnd) {
                // From crane to player
                if (!this.moveItemStackTo(stack, inventoryEnd, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player - upgrades only
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5
        ) <= 64.0;
    }

    public CraneGrabberBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
