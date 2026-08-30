package com.hbm.inventory.container;

import com.hbm.blockentity.machine.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Menu for {@link FluidTankBlockEntity} - no machine-owned slots (see that class's javadoc: this
 * scoped-down single-block tank has a 0-slot inventory, filled/drained purely through the fluid
 * capability rather than item-based canister slots), just the standard player inventory.
 * {@link MenuBase#quickMoveStack} degrades gracefully with a 0-slot machine range (shift-clicking
 * from the player inventory simply has nowhere machine-owned to go, matching a tank with no slots).
 *
 * <p>Opened the same way as {@link CrateMenu} - see that class's javadoc for the confirmed
 * {@code player.openMenu(MenuProvider, BlockPos)} wiring shape.
 */
public class FluidTankMenu extends MenuBase<FluidTankBlockEntity> {

    public FluidTankMenu(int id, Inventory playerInventory, FluidTankBlockEntity be) {
        super(ModMenuTypes.FLUID_TANK.get(), id, be);
        playerInv(playerInventory, 8, 84);
    }

    public static FluidTankMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof FluidTankBlockEntity be) {
            return new FluidTankMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No FluidTankBlockEntity at " + pos);
    }
}
