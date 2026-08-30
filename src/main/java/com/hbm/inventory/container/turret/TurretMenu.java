package com.hbm.inventory.container.turret;

import com.hbm.blockentity.turret.TurretBaseBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.items.machine.ItemTurretChip;
import com.hbm.lib.Library;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Ported from CE's {@code ContainerTurretBase} - one shared {@link MenuBase} for all 11 in-scope
 * concrete turret block entities, matching CE's own single shared {@code Container} across all 13
 * (see {@code docs/phase3/turret_system.md} decision 4). Slot 0 is restricted to
 * {@link ItemTurretChip} instances, 1-9 are the general ammo slots, 10 is battery-restricted (a
 * plain {@link SlotItemHandler} predicate stands in for CE's un-ported {@code SlotBattery} - see the
 * report's Deferred #8, this is not turret-specific enough to warrant a new shared slot class yet).
 * The 4 targeting toggles + on/off + blacklist/whitelist buttons route through
 * {@link #clickMenuButton}, matching the confirmed real
 * {@code AbstractContainerMenu#clickMenuButton}/{@code handleInventoryButtonClick} plumbing already
 * used by {@code PWRControllerMenu}/{@code MachineDieselMenu} in this port.
 */
public class TurretMenu extends MenuBase<TurretBaseBlockEntity> {

    public static final int BUTTON_TOGGLE_ON = 0;
    public static final int BUTTON_TARGET_PLAYERS = 1;
    public static final int BUTTON_TARGET_ANIMALS = 2;
    public static final int BUTTON_TARGET_MOBS = 3;
    public static final int BUTTON_TARGET_MACHINES = 4;
    public static final int BUTTON_TOGGLE_BLACKLIST = 6;

    public TurretMenu(int id, Inventory playerInv, TurretBaseBlockEntity be) {
        super(TurretMenus.TURRET.get(), id, be);

        be.manualSetup();

        this.addSlot(new SlotItemHandler(tile, 0, 98, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemTurretChip;
            }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new SlotNonRetarded(tile, 1 + i * 3 + j, 80 + j * 18, 63 + i * 18));
            }
        }

        this.addSlot(new SlotItemHandler(tile, 10, 152, 99) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Library.isBattery(stack);
            }
        });

        playerInv(playerInv, 8, 142);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case BUTTON_TOGGLE_ON, BUTTON_TARGET_PLAYERS, BUTTON_TARGET_ANIMALS, BUTTON_TARGET_MOBS, BUTTON_TARGET_MACHINES, BUTTON_TOGGLE_BLACKLIST -> {
                be.handleButtonPacket(0, id);
                return true;
            }
            default -> {
                return super.clickMenuButton(player, id);
            }
        }
    }
}
