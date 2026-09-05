package com.hbm.inventory.container.network;

import com.hbm.blockentity.network.RadioTorchCounterBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

public class RadioTorchCounterMenu extends MenuBase<RadioTorchCounterBlockEntity> {

    public RadioTorchCounterMenu(int id, Inventory playerInv, RadioTorchCounterBlockEntity be) {
        super(RadioNetworkMenus.RADIO_TORCH_COUNTER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 36));
        this.addSlot(new SlotNonRetarded(tile, 1, 80, 36));
        this.addSlot(new SlotNonRetarded(tile, 2, 134, 36));
        playerInv(playerInv, 8, 84);
    }
}
