package com.hbm.inventory.container;

import com.hbm.blockentity.machine.LaunchpadSoyuzBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported from CE's {@code com.hbm.inventory.container.ContainerLaunchpadSoyuz} (47 lines, signature
 * + slot-layout level). Pixel positions copied verbatim from CE's own constructor.
 */
public class LaunchpadSoyuzMenu extends MenuBase<LaunchpadSoyuzBlockEntity> {

    public LaunchpadSoyuzMenu(int id, Inventory playerInventory, LaunchpadSoyuzBlockEntity pad) {
        super(ModMenuTypes.LAUNCHPAD_SOYUZ.get(), id, pad);

        addSlots(pad.inventory, 0, 98, 80, 1, 1);  // Soyuz
        addSlots(pad.inventory, 1, 80, 80, 1, 1);  // Designator
        addSlots(pad.inventory, 2, 98, 26, 1, 1);  // Satellite
        addSlots(pad.inventory, 3, 80, 26, 1, 1);  // Landing module
        addSlots(pad.inventory, 4, 152, 98, 1, 1); // Kerosene in
        addSlots(pad.inventory, 5, 152, 116, 1, 1); // Kerosene out
        addSlots(pad.inventory, 6, 170, 98, 1, 1); // Oxygen in
        addSlots(pad.inventory, 7, 170, 116, 1, 1); // Oxygen out
        addSlots(pad.inventory, 8, 134, 98, 1, 1); // Battery

        // Cargo bay, 3 columns x 6 rows, columns stepping backwards (44, 26, 8) matching CE's own layout
        for (int col = 0; col < 3; col++) {
            addSlots(pad.inventory, 9 + col * 6, 44 - col * 18, 26, 6, 1);
        }

        playerInv(playerInventory, 17, 162);
    }

    public static LaunchpadSoyuzMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof LaunchpadSoyuzBlockEntity be) {
            return new LaunchpadSoyuzMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No LaunchpadSoyuzBlockEntity at " + pos);
    }
}
