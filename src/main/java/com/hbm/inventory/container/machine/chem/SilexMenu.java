package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.SilexBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Exact CE {@code ContainerSILEX.java:30-45}: input 80,12 / ID 8,24 / canister 26,24 + 44,24 takeOnly /
 * output 116,90 / queue 134,72 152,72 134,90 152,90 134,108 152,108.
 * {@code setType(1,1)} / {@code loadTank(2,3)} Exact CE {@code TileEntitySILEX.java:73-74}.
 * Void button Exact CE {@code GUISILEX.java:60-64} / {@code handleButtonPacket} :137-141.
 */
public class SilexMenu extends MenuBase<SilexBlockEntity> {

    public static final int BUTTON_VOID = 0;

    public SilexMenu(int id, Inventory playerInv, SilexBlockEntity be) {
        super(ChemIsotopeMenus.SILEX.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, SilexBlockEntity.INPUT_SLOT, 80, 12));
        this.addSlot(new SlotNonRetarded(tile, SilexBlockEntity.SLOT_ID, 8, 24));
        this.addSlot(new SlotNonRetarded(tile, SilexBlockEntity.SLOT_CANISTER, 26, 24));
        this.addSlot(new SlotTakeOnly(tile, SilexBlockEntity.SLOT_EMPTY, 44, 24));
        this.addSlot(new SlotTakeOnly(tile, SilexBlockEntity.OUTPUT_SLOT, 116, 90));
        this.addSlot(new SlotTakeOnly(tile, 5, 134, 72));
        this.addSlot(new SlotTakeOnly(tile, 6, 152, 72));
        this.addSlot(new SlotTakeOnly(tile, 7, 134, 90));
        this.addSlot(new SlotTakeOnly(tile, 8, 152, 90));
        this.addSlot(new SlotTakeOnly(tile, 9, 134, 108));
        this.addSlot(new SlotTakeOnly(tile, 10, 152, 108));

        playerInv(playerInv, 8, 140);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_VOID) {
            be.voidContents();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
