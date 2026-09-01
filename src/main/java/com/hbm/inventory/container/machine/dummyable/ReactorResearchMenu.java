package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.ReactorResearchBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerReactorResearch}: 12 plate slots + player inv at y+56. */
public class ReactorResearchMenu extends MenuBase<ReactorResearchBlockEntity> {

    public ReactorResearchMenu(int id, Inventory playerInv, ReactorResearchBlockEntity be) {
        super(DummyableProcessMenus.REACTOR_RESEARCH.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 95, 22));
        this.addSlot(new SlotNonRetarded(tile, 1, 131, 22));
        this.addSlot(new SlotNonRetarded(tile, 2, 77, 40));
        this.addSlot(new SlotNonRetarded(tile, 3, 113, 40));
        this.addSlot(new SlotNonRetarded(tile, 4, 149, 40));
        this.addSlot(new SlotNonRetarded(tile, 5, 95, 58));
        this.addSlot(new SlotNonRetarded(tile, 6, 131, 58));
        this.addSlot(new SlotNonRetarded(tile, 7, 77, 76));
        this.addSlot(new SlotNonRetarded(tile, 8, 113, 76));
        this.addSlot(new SlotNonRetarded(tile, 9, 149, 76));
        this.addSlot(new SlotNonRetarded(tile, 10, 95, 94));
        this.addSlot(new SlotNonRetarded(tile, 11, 131, 94));
        playerInv(playerInv, 8, 140);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 100) {
            be.setTarget(Mth.clamp(id, 0, 100) * 0.01D);
            be.setChanged();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
