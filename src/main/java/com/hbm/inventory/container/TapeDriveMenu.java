package com.hbm.inventory.container;

import com.hbm.blockentity.machine.TapeDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Port of CE {@code com.hbm.inventory.container.ContainerTapeDrive}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/container/ContainerTapeDrive.java
 */
public class TapeDriveMenu extends AbstractContainerMenu {

    private final TapeDriveBlockEntity blockEntity;

    public TapeDriveMenu(int windowId, Inventory playerInventory, TapeDriveBlockEntity blockEntity) {
        super(com.hbm.inventory.ModMenuTypes.TAPE_DRIVE.get(), windowId);
        this.blockEntity = blockEntity;

        // 12 drive slots in 2 rows of 6: CE ContainerTapeDrive.java:11
        // this.addSlots(drive.inventory, 0, 35, 27, 2, 6);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                this.addSlot(new Slot(blockEntity.inventory, col + row * 6, 35 + col * 18, 27 + row * 18));
            }
        }

        // Player inventory: CE ContainerTapeDrive.java:12
        // this.playerInv(invPlayer, 8, 104);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // 12 drive slots, then 36 player slots
            if (index < 12) {
                // Drive slot -> player inv
                if (!this.moveItemStackTo(stack, 12, 12 + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inv -> drive slots
                if (!this.moveItemStackTo(stack, 0, 12, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.blockEntity.stillValid(player);
    }

    public TapeDriveBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public static TapeDriveMenu fromNetwork(int windowId, Inventory playerInventory, net.minecraft.network.RegistryFriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (!(be instanceof TapeDriveBlockEntity tapeDrive)) {
            throw new IllegalStateException("Block entity at " + pos + " is not a TapeDriveBlockEntity");
        }
        return new TapeDriveMenu(windowId, playerInventory, tapeDrive);
    }
}
