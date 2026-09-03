package com.hbm.menu;

import com.hbm.blockentity.network.CraneRouterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * NeoForge port of CE's {@code ContainerCraneRouter} - 30 filter slots (6 sides × 5 filters).
 * CE layout: 2 columns of 15 slots each (3 sides per column, 5 filters per side).
 * TODO(CE): Port full GUI screen with mode toggle buttons + pattern mode tooltips.
 */
public class CraneRouterMenu extends AbstractContainerMenu {

    private final CraneRouterBlockEntity blockEntity;

    public CraneRouterMenu(int containerId, Inventory playerInventory, CraneRouterBlockEntity blockEntity) {
        super(com.hbm.inventory.container.ModMenuTypes.CRANE_ROUTER.get(), containerId);
        this.blockEntity = blockEntity;

        // 30 filter slots: 2 columns, 3 sides each, 5 filters per side
        // CE layout: left column (sides 0-2), right column (sides 3-5)
        for (int j = 0; j < 2; j++) { // columns
            for (int i = 0; i < 3; i++) { // sides per column
                for (int k = 0; k < 5; k++) { // filters per side
                    int slotIndex = k + j * 15 + i * 5;
                    int xPos = 34 + k * 18 + j * 98;
                    int yPos = 17 + i * 26;
                    this.addSlot(new SlotItemHandler(blockEntity.inventory, slotIndex, xPos, yPos));
                }
            }
        }

        // Player inventory (3x9) at (47, 119)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 47 + col * 18, 119 + row * 18));
            }
        }

        // Player hotbar at (47, 177)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 47 + col * 18, 177));
        }
    }

    // Client constructor
    public CraneRouterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (CraneRouterBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // CE: shift-clicking disabled for filter slots
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5
        ) <= 64.0;
    }

    public CraneRouterBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
