package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.SoyuzCapsuleBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerSoyuzCapsule}: 3×6 grid + slot 18. */
public class SoyuzCapsuleMenu extends MenuBase<SoyuzCapsuleBlockEntity> {

    public SoyuzCapsuleMenu(int id, Inventory playerInv, SoyuzCapsuleBlockEntity be) {
        super(DummyableProcessMenus.SOYUZ_CAPSULE.get(), id, be);
        this.addSlots(tile, 0, 62, 18, 3, 6);
        this.addSlot(new SlotNonRetarded(tile, 18, 17, 36));
        playerInv(playerInv, 8, 104, 162);
    }
}
