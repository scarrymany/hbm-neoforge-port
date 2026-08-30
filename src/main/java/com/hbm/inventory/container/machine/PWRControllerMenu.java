package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for {@link PWRControllerBlockEntity}, ported from CE's {@code ContainerPWR}. Slots 0 (fresh
 * fuel in) and 1 (hot fuel out) are exposed; slot 2 is deliberately left off this Menu - see
 * {@link PWRControllerBlockEntity}'s own class javadoc "3-slot inventory, slot 2 unused" and
 * {@code docs/phase2/reactors_breeding_pwr.md}'s open question on it (CE never reads/writes it either,
 * and no confirmed purpose was found for this pass - safer to leave it inventory-only than guess a
 * GUI slot position for it).
 *
 * <p><b>Rod-level control</b>: CE's {@code GUIPWR} used a draggable scrollbar-style slider sending
 * {@code IControlReceiver.receiveControl} NBT packets - no generic client-&gt;server
 * {@code CompoundTag} control-packet channel exists in this port yet (see
 * {@code com.hbm.interfaces.IControlReceiver}'s own doc and {@code MachineBaseBlockEntity#handleButtonPacket}'s
 * "future GUI button packets" placeholder). Substituted with the same stepped-button-pair convention
 * this port already established for an identical slider-replacement problem
 * ({@code MachineCombustionEngineMenu}'s throttle buttons, see that class's own javadoc): two vanilla
 * {@link net.minecraft.world.inventory.AbstractContainerMenu#clickMenuButton} buttons nudge
 * {@link PWRControllerBlockEntity#rodTarget} by {@link #ROD_STEP} instead of a free-drag slider.
 */
public class PWRControllerMenu extends MenuBase<PWRControllerBlockEntity> {

    public static final int BUTTON_ROD_DOWN = 0;
    public static final int BUTTON_ROD_UP = 1;
    public static final double ROD_STEP = 10D;

    /**
     * Real GUI slot count (2), deliberately smaller than {@code tile.getSlots()} (3 - see
     * {@link PWRControllerBlockEntity}'s own "slot 2 unused" javadoc). {@link MenuBase#quickMoveStack}'s
     * shared implementation assumes every {@code tile.getSlots()} index has a matching {@link Slot} at
     * the front of {@link #slots}, which is not true here; {@link #quickMoveStack} below is overridden
     * with that same reference logic but keyed off this constant instead, so shift-clicking the
     * player's own first inventory slot (index 2 in {@link #slots}, immediately after this Menu's two
     * real machine slots) is not miscategorized as a third machine slot.
     */
    private static final int MACHINE_SLOTS = 2;

    public PWRControllerMenu(int id, Inventory playerInv, PWRControllerBlockEntity be) {
        super(PWRMenus.PWR_CONTROLLER.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 62, 41));
        this.addSlot(new SlotTakeOnly(tile, 1, 98, 41));

        playerInv(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            newStack = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == newStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }

        return newStack;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case BUTTON_ROD_DOWN -> {
                be.setRodTarget(be.rodTarget - ROD_STEP);
                return true;
            }
            case BUTTON_ROD_UP -> {
                be.setRodTarget(be.rodTarget + ROD_STEP);
                return true;
            }
            default -> {
                return super.clickMenuButton(player, id);
            }
        }
    }
}
