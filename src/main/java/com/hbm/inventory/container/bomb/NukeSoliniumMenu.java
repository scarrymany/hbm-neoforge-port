package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeSoliniumBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerNukeSolinium} slot coords. 176×222. */
public class NukeSoliniumMenu extends MenuBase<NukeSoliniumBlockEntity> {

    public NukeSoliniumMenu(int id, Inventory playerInv, NukeSoliniumBlockEntity be) {
        super(NukeCasingMenus.NUKE_SOLINIUM.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 26, 18));
        this.addSlot(new SlotNonRetarded(tile, 1, 53, 18));
        this.addSlot(new SlotNonRetarded(tile, 2, 107, 18));
        this.addSlot(new SlotNonRetarded(tile, 3, 134, 18));
        this.addSlot(new SlotNonRetarded(tile, 4, 80, 36));
        this.addSlot(new SlotNonRetarded(tile, 5, 26, 54));
        this.addSlot(new SlotNonRetarded(tile, 6, 53, 54));
        this.addSlot(new SlotNonRetarded(tile, 7, 107, 54));
        this.addSlot(new SlotNonRetarded(tile, 8, 134, 54));

        playerInv(playerInv, 8, 140);
    }
}
