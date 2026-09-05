package com.hbm.inventory.container;

import com.hbm.blockentity.bomb.LaunchPadBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerLaunchPadLarge.java:25-38}: missile 26,36 / designator 26,72 /
 * battery 107,90 / fuel 125,90 + 125,108 takeOnly / oxidizer 143,90 + 143,108 takeOnly.
 * {@code loadTank(3,4)} / {@code loadTank(5,6)} Exact CE {@code TileEntityLaunchPadBase.java:173-174}.
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
