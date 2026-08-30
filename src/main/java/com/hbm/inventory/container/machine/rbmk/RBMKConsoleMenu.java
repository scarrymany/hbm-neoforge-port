package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * RBMK console menu - no slots. Unlike every column TE, {@link RBMKConsoleBlockEntity} extends the
 * shared {@code MachineBaseBlockEntity} (it is not itself an RBMK grid column, see that class's
 * javadoc), so this reuses the shared {@link MenuBase} rather than {@link RBMKSlottedMenuBase}.
 */
public class RBMKConsoleMenu extends MenuBase<RBMKConsoleBlockEntity> {

    public RBMKConsoleMenu(int id, Inventory playerInventory, RBMKConsoleBlockEntity be) {
        super(RBMKMenuTypes.CONSOLE.get(), id, be);
        playerInv(playerInventory, 8, 84);
    }

    public static RBMKConsoleMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKConsoleBlockEntity be) {
            return new RBMKConsoleMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKConsoleBlockEntity at " + pos);
    }
}
