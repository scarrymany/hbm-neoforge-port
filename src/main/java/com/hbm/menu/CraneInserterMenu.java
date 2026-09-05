package com.hbm.menu;

import com.hbm.blockentity.network.CraneInserterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * NeoForge port of CE's {@code ContainerCraneInserter} - 21-slot (3x7) crane buffer inventory.
 */
public class CraneInserterMenu extends AbstractContainerMenu {

    private final CraneInserterBlockEntity blockEntity;

    public CraneInserterMenu(int containerId, Inventory playerInventory, CraneInserterBlockEntity blockEntity) {
        super(com.hbm.inventory.container.ModMenuTypes.CRANE_INSERTER.get(), containerId);
        this.blockEntity = blockEntity;

        // 21 crane slots (3 rows x 7 columns)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 7; col++) {
                this.addSlot(new SlotItemHandler(blockEntity.getInventory(), col + row * 7, 8 + col * 18, 17 + row * 18));
            }
        }

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161));
        }
    }

    // Client constructor
    public CraneInserterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (CraneInserterBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int craneSlotCount = CraneInserterBlockEntity.INVENTORY_SIZE;

            if (index < craneSlotCount) {
                // From crane to player inventory
                if (!this.moveItemStackTo(stack, craneSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to crane
                if (!this.moveItemStackTo(stack, 0, craneSlotCount, false)) {
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

    public CraneInserterBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
