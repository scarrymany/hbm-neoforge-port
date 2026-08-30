package com.hbm.inventory.container;

import com.hbm.blockentity.bomb.LaunchPadBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported from CE's {@code com.hbm.inventory.container.ContainerLaunchPadLarge} (109 lines,
 * signature + slot-layout level per {@code docs/phase3/missile_launch_infra.md}) - shared by both
 * {@link com.hbm.blockentity.bomb.LaunchPadBlockEntity} (small pad) and
 * {@link com.hbm.blockentity.bomb.LaunchPadLargeBlockEntity} (large pad), exactly matching CE's own
 * {@code TileEntityLaunchPadBase.provideContainer} always returning {@code ContainerLaunchPadLarge}
 * regardless of which concrete pad opened it. Pixel positions copied verbatim from CE's own
 * constructor. Slot 2 ("Battery") uses {@link com.hbm.inventory.slot.SlotNonRetarded} rather than
 * CE's dedicated {@code SlotBattery} - this port has no equivalent battery-only filtered slot class
 * yet, so any item can sit there client-side; server-side charging still only accepts
 * {@link com.hbm.api.energymk2.IBatteryItem}s via {@link com.hbm.lib.Library#chargeTEFromItems}.
 */
public class LaunchPadMenu extends MenuBase<LaunchPadBaseBlockEntity> {

    public LaunchPadMenu(int id, Inventory playerInventory, LaunchPadBaseBlockEntity be) {
        super(ModMenuTypes.LAUNCH_PAD.get(), id, be);

        addSlots(be.inventory, 0, 26, 36, 1, 1);       // Missile
        addSlots(be.inventory, 1, 26, 72, 1, 1);       // Designator
        addSlots(be.inventory, 2, 107, 90, 1, 1);      // Battery
        addSlots(be.inventory, 3, 125, 90, 1, 1);      // Fuel in
        addTakeOnlySlots(be.inventory, 4, 125, 108, 1, 1); // Fuel out
        addSlots(be.inventory, 5, 143, 90, 1, 1);      // Oxidizer in
        addTakeOnlySlots(be.inventory, 6, 143, 108, 1, 1); // Oxidizer out

        playerInv(playerInventory, 8, 154);
    }

    public static LaunchPadMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof LaunchPadBaseBlockEntity be) {
            return new LaunchPadMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No LaunchPadBaseBlockEntity at " + pos);
    }
}
