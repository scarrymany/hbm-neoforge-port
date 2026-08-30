package com.hbm.inventory.container;

import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.slot.SlotCraftingOutput;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Shared {@link AbstractContainerMenu} base for every Phase 2 machine, ported from CE's
 * {@code com.hbm.inventory.container.ContainerBase} (122 lines, read in full) - {@code stillValid}
 * delegating to the block entity, a {@code quickMoveStack} reference implementation identical in
 * shape to CE's {@code transferStackInSlot}, and CE's slot-batch helper methods
 * ({@code playerInv}/{@code addSlots}/{@code addOutputSlots}/{@code addTakeOnlySlots}). See
 * {@code docs/phase2/gui_framework.md} for the full survey this is modeled on.
 *
 * <h2>Type bound: why {@code MachineBaseBlockEntity}, not vanilla {@code Container}</h2>
 * Neo Edition's own real {@code MenuBase<T extends Container>} binds against vanilla
 * {@code net.minecraft.world.Container} because its block entities implement that interface
 * directly. This port's block-entity base framework (landed just before this package, see
 * {@code com.hbm.blockentity.MachineBaseBlockEntity}, read in full) made the opposite, already-final
 * choice: machine inventories are a plain {@link net.neoforged.neoforge.items.ItemStackHandler}
 * exposed as an {@link IItemHandler} capability, and the block entity itself does <b>not</b>
 * implement vanilla {@code Container}. Every slot class in {@code com.hbm.inventory.slot} is
 * therefore {@link net.neoforged.neoforge.items.SlotItemHandler}-backed (matching CE's own
 * Forge-1.12-era {@code ContainerBase}, which is itself {@code IItemHandler}-based, not Neo
 * Edition's vanilla-{@code Container} rewrite) and this class binds directly to
 * {@link MachineBaseBlockEntity} - the one base class that both owns an inventory
 * ({@link MachineBaseBlockEntity#getCheckedInventory()}) and already provides the
 * player-distance/still-valid check ({@link MachineBaseBlockEntity#isUseableByPlayer(Player)}) this
 * class's {@link #stillValid(Player)} delegates to. This resolves the "open question" flagged in
 * {@code docs/phase2/gui_framework.md} ("IItemHandler-backed slots vs. block entity directly
 * implementing Container") in favor of the IItemHandler side, because that is the shape the
 * block-entity package actually shipped with, not a choice made independently here.
 *
 * <h2>HE power / fluid-tank sync - read this before wiring a machine Screen to this Menu</h2>
 * This class intentionally has <b>no</b> {@code ContainerData}/int-array sync plumbing, on purpose,
 * not as an oversight. Per {@code docs/phase2/gui_framework.md} decision 3 (cross-checked against
 * CE's {@code TileEntityMachineElectricFurnace.power} being a bare {@code long} field and against
 * Neo Edition's confirmed-real {@code MachineCentrifugeScreen} reading
 * {@code this.be.getPower()}/{@code getMaxPower()} straight off the client-side block entity, no
 * {@code ContainerData} anywhere), HE power/heat/progress/tank-fill values sync to the client via
 * the block entity's own full-NBT update packet, not via this Menu at all. Concretely, for any
 * {@code MachineBaseBlockEntity} subclass: {@link com.hbm.blockentity.LoadedBaseBlockEntity}
 * (the class {@code MachineBaseBlockEntity} itself extends) already implements
 * {@code getUpdateTag}/{@code handleUpdateTag}/{@code getUpdatePacket} to push
 * {@link com.hbm.blockentity.LoadedBaseBlockEntity#serializeInitial}/the {@code saveAdditional} NBT
 * tree to every client in view on chunk load, and {@code networkPackNT}/{@code networkPackMK2} push
 * incremental updates afterward - this is already-shipped, confirmed machinery (read in full as
 * part of this package's own prerequisite check), not something this Menu/Screen package needs to
 * add. <b>Consequence for a future machine's Menu+Screen pair</b>: a machine's {@code power}/
 * {@code progress}/tank fields should be plain fields on the block entity, written by
 * {@code saveAdditional}/read by {@code loadAdditional} and layered into
 * {@code serialize}/{@code deserialize} for the live sync path, exactly like CE and Neo Edition do;
 * the matching {@code Screen} (see {@link com.hbm.inventory.gui.GuiInfoContainer}) then reads
 * {@code this.getMenu().be.<field or getter>} directly in its render override - it does
 * <i>not</i> go through a {@code ContainerData} slot, and this class deliberately does not offer
 * one. Reach for {@code ContainerData} only for a genuinely cosmetic, purely client-side int that
 * does not belong in the block entity's own persisted/synced state; none of the machines surveyed
 * for this package need that escape hatch.
 *
 * @param <T> the concrete machine block entity this menu is opened against.
 */
public abstract class MenuBase<T extends MachineBaseBlockEntity> extends AbstractContainerMenu {

    /** The block entity this menu was opened for. */
    public final T be;

    /**
     * The checked (validated through {@link MachineBaseBlockEntity#isItemValidForSlot}) view of
     * {@code be}'s inventory, matching CE's {@code ContainerBase.tile} field - used as the
     * shift-click slot-count boundary in {@link #quickMoveStack(Player, int)}. Individual
     * {@code addSlots}/{@code addOutputSlots}/{@code addTakeOnlySlots} calls below still take their
     * own explicit {@link IItemHandler} parameter rather than always reading this field, exactly
     * like CE's own helpers do, in case a future machine ever needs to add slots against a second,
     * separate handler (e.g. a standalone battery/upgrade {@code ItemStackHandler}) - see CE's
     * {@code ContainerMachineElectricFurnace} for a real example of that mixed pattern.
     */
    protected final IItemHandlerModifiable tile;

    protected MenuBase(MenuType<?> menuType, int id, T be) {
        super(menuType, id);
        this.be = be;
        this.tile = be.getCheckedInventory();
    }

    @Override
    public boolean stillValid(Player player) {
        return be.isUseableByPlayer(player);
    }

    /**
     * Generic shift-click reference implementation, identical in shape to CE's
     * {@code ContainerBase.transferStackInSlot}/{@code InventoryUtil.transferStack} for the common
     * case (one contiguous block of machine-owned slots at the front, player inventory + hotbar
     * after). CE's actual per-machine behavior instead runs through
     * {@code com.hbm.inventory.TransferStrategy} so a shift-click can route into a specific
     * "input"/"output"/"battery"/"upgrade" sub-range rather than the first free slot; porting that
     * per-machine configuration is explicitly deferred (see {@code docs/phase2/gui_framework.md}'s
     * Deferred scope - "the actual per-machine TransferStrategy configurations ... are inherently
     * per-machine"). A concrete machine Menu with more than one logical slot range should override
     * this method rather than trying to configure a nonexistent {@code TransferStrategy} here.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        int beSlotCount = tile.getSlots();

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            newStack = stack.copy();

            if (index < beSlotCount) {
                if (!this.moveItemStackTo(stack, beSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, beSlotCount, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            // Both this early-return guard and the slot.onTake call below are absent from Neo
            // Edition's own generic MenuBase.quickMoveStack, but restored here to match vanilla's own
            // AbstractContainerMenu convention (every stock quickMoveStack, e.g. CraftingMenu, shapes
            // it exactly this way): bail without notifying the slot if the merge above didn't actually
            // move anything (stack.getCount() unchanged - e.g. the target range was full), and
            // otherwise call onTake so SlotCraftingOutput's XP-awarding hook fires on shift-click too,
            // not just on a manual pickup.
            if (stack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return newStack;
    }

    /** Standard player inventory with default hotbar offset (58px below {@code playerInvY}). */
    public void playerInv(Inventory inventory, int playerInvX, int playerInvY) {
        this.playerInv(inventory, playerInvX, playerInvY, playerInvY + 58);
    }

    /**
     * Used to quickly set up the player inventory. Plain vanilla {@link Slot}, not
     * {@link SlotNonRetarded}, matching CE's own {@code ContainerBase.playerInv} exactly - the
     * player's {@link Inventory} is a vanilla {@code Container}, not an {@link IItemHandler}, so the
     * {@link IItemHandler}-backed slot classes in {@code com.hbm.inventory.slot} do not apply to it.
     */
    public void playerInv(Inventory inventory, int playerInvX, int playerInvY, int playerHotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, playerInvX + col * 18, playerInvY + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, playerInvX + col * 18, playerHotbarY));
        }
    }

    /** Used to add several conventional inventory slots at a time. */
    public void addSlots(IItemHandler inv, int from, int x, int y, int rows, int cols) {
        addSlots(inv, from, x, y, rows, cols, 18);
    }

    public void addSlots(IItemHandler inv, int from, int x, int y, int rows, int cols, int slotSize) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.addSlot(new SlotNonRetarded(inv, col + row * cols + from, x + col * slotSize, y + row * slotSize));
            }
        }
    }

    public void addOutputSlots(Player player, IItemHandler inv, int from, int x, int y, int rows, int cols) {
        addOutputSlots(player, inv, from, x, y, rows, cols, 18);
    }

    public void addOutputSlots(Player player, IItemHandler inv, int from, int x, int y, int rows, int cols, int slotSize) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.addSlot(new SlotCraftingOutput(player, inv, col + row * cols + from, x + col * slotSize, y + row * slotSize));
            }
        }
    }

    public void addTakeOnlySlots(IItemHandler inv, int from, int x, int y, int rows, int cols) {
        addTakeOnlySlots(inv, from, x, y, rows, cols, 18);
    }

    public void addTakeOnlySlots(IItemHandler inv, int from, int x, int y, int rows, int cols, int slotSize) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.addSlot(new SlotTakeOnly(inv, col + row * cols + from, x + col * slotSize, y + row * slotSize));
            }
        }
    }
}
