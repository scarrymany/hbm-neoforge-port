package com.hbm.inventory.container.network;

import com.hbm.blockentity.network.RadioTorchBaseBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** Channel + polling for sender/receiver/logic/reader/controller. */
public class RadioTorchMenu extends MenuBase<RadioTorchBaseBlockEntity> {

    public RadioTorchMenu(int id, Inventory playerInv, RadioTorchBaseBlockEntity be) {
        super(RadioNetworkMenus.RADIO_TORCH.get(), id, be);
        playerInv(playerInv, 8, 84);
    }
}
