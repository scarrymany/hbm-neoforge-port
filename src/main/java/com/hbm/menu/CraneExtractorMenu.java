package com.hbm.menu;

import com.hbm.blockentity.network.CraneExtractorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * NeoForge port of CE's {@code ContainerCraneExtractor} - 9 filter + 9 buffer + 2 upgrade slots.
 */
public class CraneExtractorMenu extends AbstractContainerMenu {

    private final CraneExtractorBlockEntity blockEntity;

    public CraneExtractorMenu(int containerId, Inventory playerInventory, CraneExtractorBlockEntity blockEntity) {
        super(com.hbm.inventory.container.ModMenuTypes.CRANE_EXTRACTOR.get(), containerId);
        this.blockEntity = blockEntity;

        // 9 filter slots (3x3) at (71, 17)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new SlotItemHandler(blockEntity.getInventory(), col + row * 3, 71 + col * 18, 17 + row * 18));
            }
        }

        // 9 buffer slots (3x3) at (8, 17)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new SlotItemHandler(blockEntity.getInventory(), 9 + col + row * 3, 8 + col * 18, 17 + row * 18));
            }
        }

        // 2 upgrade slots at (152, 23) and (152, 47)
        this.addSlot(new SlotItemHandler(blockEntity.getInventory(), 18, 152, 23));
        this.addSlot(new SlotItemHandler(blockEntity.getInventory(), 19, 152, 47));

        // Player inventory (3x9) at (26, 103)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 26 + col * 18, 103 + row * 18));
            }
        }

        // Player hotbar at (26, 161)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 26 + col * 18, 161));
        }
    }

    // Client constructor
    public CraneExtractorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (CraneExtractorBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
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

            int inventoryEnd = 20; // 9 filter + 9 buffer + 2 upgrade

            if (index < inventoryEnd) {
                // From crane to player
                if (!this.moveItemStackTo(stack, inventoryEnd, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to crane buffer
                if (!this.moveItemStackTo(stack, 9, 18, false)) {
                    return ItemStack.EMPTY;
                }
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

    public CraneExtractorBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
