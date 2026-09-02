package com.hbm.inventory.container;

import com.hbm.blockentity.machine.TapeDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Port of CE {@code com.hbm.inventory.container.ContainerTapeDrive}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/container/ContainerTapeDrive.java
 */
public class TapeDriveMenu extends MenuBase<TapeDriveBlockEntity> {

    public TapeDriveMenu(int windowId, Inventory playerInventory, TapeDriveBlockEntity blockEntity) {
        super(ModMenuTypes.TAPE_DRIVE.get(), windowId, blockEntity);

        // 12 drive slots in 2 rows of 6: CE ContainerTapeDrive.java:11
        // this.addSlots(drive.inventory, 0, 35, 27, 2, 6);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new SlotItemHandler(blockEntity.inventory, col + row * 6, 35 + col * 18, 27 + row * 18));
            }
        }

        // Player inventory: CE ContainerTapeDrive.java:12
        // this.playerInv(invPlayer, 8, 104);
        playerInv(playerInventory, 8, 104);
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
