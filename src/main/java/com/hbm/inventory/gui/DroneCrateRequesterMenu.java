package com.hbm.inventory.gui;

import com.hbm.blockentity.network.DroneCrateRequesterBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal CE {@code ContainerDroneRequester} - 18-slot requester inventory GUI (9 filter + 9 stock).
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/container/ContainerDroneRequester.java
 * <p>
 * Partial port: filter slots (0-8) accept items but do NOT implement ModulePatternMatcher mode cycling (CE :76-98).
 * TODO(CE): Port ModulePatternMatcher GUI interaction (right-click filter to cycle EXACT/WILDCARD/OreDict).
 */
public class DroneCrateRequesterMenu extends AbstractContainerMenu {

    private final DroneCrateRequesterBlockEntity requester;

    public DroneCrateRequesterMenu(int containerId, Inventory playerInv, DroneCrateRequesterBlockEntity requester) {
        super(ModMenuTypes.DRONE_CRATE_REQUESTER.get(), containerId);
        this.requester = requester;

        // 9 filter slots (3x3 grid, right side) - CE :20-23 (slots 0-8, pos 98+j*18, 17+i*18)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(requester, j + i * 3, 98 + j * 18, 17 + i * 18));
            }
        }

        // 9 stock slots (3x3 grid, left side) - CE :25-29 (slots 9-17, pos 26+j*18, 17+i*18)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(requester, j + i * 3 + 9, 26 + j * 18, 17 + i * 18));
            }
        }

        // Player inventory (3x9) - CE :31-35
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 103 + i * 18));
            }
        }

        // Player hotbar (1x9) - CE :37-39
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInv, i, 8 + i * 18, 161));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            // CE :52 - ignore filter slots (0-8) for shift-click
            if (index < 9) return ItemStack.EMPTY;

            // From stock slots (9-17) to player inventory
            if (index <= 17) {
                if (!this.moveItemStackTo(stack, 18, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory to stock slots
            else {
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

        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.requester.stillValid(player);
    }

    public static DroneCrateRequesterMenu fromNetwork(int containerId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        DroneCrateRequesterBlockEntity requester = (DroneCrateRequesterBlockEntity) playerInv.player.level().getBlockEntity(pos);
        return new DroneCrateRequesterMenu(containerId, playerInv, requester);
    }
}
