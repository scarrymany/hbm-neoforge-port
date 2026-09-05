package com.hbm.inventory.gui;

import com.hbm.blockentity.network.DroneCrateProviderBlockEntity;
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
 * Minimal CE {@code ContainerDroneProvider} - 9-slot provider inventory GUI.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/container/ContainerDroneProvider.java
 */
public class DroneCrateProviderMenu extends AbstractContainerMenu {

    private final DroneCrateProviderBlockEntity provider;

    public DroneCrateProviderMenu(int containerId, Inventory playerInv, DroneCrateProviderBlockEntity provider) {
        super(ModMenuTypes.DRONE_CRATE_PROVIDER.get(), containerId);
        this.provider = provider;

        // 9 provider slots (3x3 grid) - CE :24-27
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(provider, j + i * 3, 62 + j * 18, 17 + i * 18));
            }
        }

        // Player inventory (3x9) - CE :29-33
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 103 + i * 18));
            }
        }

        // Player hotbar (1x9) - CE :35-37
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

            // From provider slots (0-8) to player inventory
            if (index < 9) {
                if (!this.moveItemStackTo(stack, 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory to provider slots
            else {
                if (!this.moveItemStackTo(stack, 0, 9, false)) {
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
        return this.provider.stillValid(player);
    }

    public static DroneCrateProviderMenu fromNetwork(int containerId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        DroneCrateProviderBlockEntity provider = (DroneCrateProviderBlockEntity) playerInv.player.level().getBlockEntity(pos);
        return new DroneCrateProviderMenu(containerId, playerInv, provider);
    }
}
