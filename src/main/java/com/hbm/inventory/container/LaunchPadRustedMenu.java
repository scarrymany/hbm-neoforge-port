package com.hbm.inventory.container;

import com.hbm.blockentity.bomb.LaunchPadRustedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported from CE's {@code com.hbm.inventory.container.ContainerLaunchPadRusted} (83 lines,
 * signature + slot-layout level). Slot 0 (result) uses a plain slot rather than CE's
 * {@code SlotCraftingOutput} (that slot class exists in this port too, but CE's rusted-pad usage of
 * it is purely cosmetic - the result item is placed by {@link LaunchPadRustedBlockEntity#receiveControl},
 * not crafted, so no XP-award semantics apply here).
 */
public class LaunchPadRustedMenu extends MenuBase<LaunchPadRustedBlockEntity> {

    public LaunchPadRustedMenu(int id, Inventory playerInventory, LaunchPadRustedBlockEntity be) {
        super(ModMenuTypes.LAUNCH_PAD_RUSTED.get(), id, be);

        addSlots(be.inventory, 0, 26, 72, 1, 1);   // Missile result
        addSlots(be.inventory, 1, 116, 45, 1, 1);  // launch_code
        addSlots(be.inventory, 2, 134, 45, 1, 1);  // launch_key
        addSlots(be.inventory, 3, 26, 99, 1, 1);   // Designator

        playerInv(playerInventory, 8, 154);
    }

    public static LaunchPadRustedMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof LaunchPadRustedBlockEntity be) {
            return new LaunchPadRustedMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No LaunchPadRustedBlockEntity at " + pos);
    }
}
