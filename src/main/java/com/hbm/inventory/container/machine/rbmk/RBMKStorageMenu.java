package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKStorageBlockEntity;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Storage menu. Exact CE {@code ContainerRBMKStorage.java:24-38}: 4×3 column-major
 * ({@code i + j * 3} at {@code 32+32j, 29+16i}), player inv at 8,104.
 */
public class RBMKStorageMenu extends RBMKSlottedMenuBase<RBMKStorageBlockEntity> {

    public RBMKStorageMenu(int id, Inventory playerInventory, RBMKStorageBlockEntity be) {
        super(RBMKMenuTypes.STORAGE.get(), id, be);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                this.addSlot(new SlotNonRetarded(be.inventory, i + j * 3, 32 + 32 * j, 29 + 16 * i));
            }
        }
        playerInv(playerInventory, 8, 104);
    }

    public static RBMKStorageMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKStorageBlockEntity be) {
            return new RBMKStorageMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKStorageBlockEntity at " + pos);
    }
}
