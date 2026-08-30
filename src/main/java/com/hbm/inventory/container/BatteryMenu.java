package com.hbm.inventory.container;

import com.hbm.blockentity.machine.BatteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Single-block battery menu, ported from CE's {@code com.hbm.inventory.container.ContainerMachineBattery}
 * (read in full, per {@code docs/phase2/machines_storage.md}): slot 0 (discharge-in) + slot 1
 * (discharge-out, take-only) on the left, slot 2 (charge-in) + slot 3 (charge-out, take-only) on the
 * right, plus the standard player inventory. Pixel positions copied verbatim from CE's own
 * {@code ContainerMachineBattery} constructor.
 *
 * <p>Opened the same way as {@link CrateMenu} - see that class's javadoc for the confirmed
 * {@code player.openMenu(MenuProvider, BlockPos)} wiring shape.
 */
public class BatteryMenu extends MenuBase<BatteryBlockEntity> {

    public BatteryMenu(int id, Inventory playerInventory, BatteryBlockEntity be) {
        super(ModMenuTypes.BATTERY.get(), id, be);

        addSlots(be.inventory, 0, 35, 17, 1, 1);
        addTakeOnlySlots(be.inventory, 1, 35, 53, 1, 1);
        addSlots(be.inventory, 2, 125, 17, 1, 1);
        addTakeOnlySlots(be.inventory, 3, 125, 53, 1, 1);

        playerInv(playerInventory, 8, 84);
    }

    public static BatteryMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof BatteryBlockEntity be) {
            return new BatteryMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No BatteryBlockEntity at " + pos);
    }
}
