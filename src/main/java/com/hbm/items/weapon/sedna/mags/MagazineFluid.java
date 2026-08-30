package com.hbm.items.weapon.sedna.mags;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.mags.MagazineFluid} (73 lines) - backs the
 * chemthrower/flamethrower family. Read in full per this task's instruction (the gun-framework
 * report flagged it as sized/skimmed only, not read - confirmed here to <b>not</b> source its "ammo"
 * from this port's {@code com.hbm.api.fluidmk2} tank-network capabilities at all): CE's own
 * {@code MagazineFluid} is exactly as self-contained as {@link MagazineSingleTypeBase} - a fixed-
 * capacity pool tracked purely as flat NBT ints on the gun stack itself (this port:
 * {@link com.hbm.items.weapon.sedna.MagState}), with {@link #canReload}/{@link #reloadAction} both
 * permanently no-ops. There is no CE code path that ever fills this pool from an inventory scan or a
 * capability network - whatever gun content (Package D) actually refills a fluid-backed gun (e.g. an
 * {@code onPressReload} lambda reading from a held/adjacent tank) is that content's own concern, not
 * this mag class's. Flagging this explicitly since the task description anticipated a capability-
 * backed design that CE's real source does not have.
 */
public class MagazineFluid implements IMagazine<FluidType> {

    /** A number so the gun can tell multiple mags apart - see {@link MagazineSingleTypeBase}'s javadoc for the same contract. */
    public final int index;
    /** How much fluid (mB) this mag can hold. */
    public final int capacity;
    /** Whichever fluids can be poured into this mag. */
    public final FluidType[] acceptedTypes;

    public MagazineFluid(int index, int capacity, FluidType... acceptedTypes) {
        this.index = index;
        this.capacity = capacity;
        this.acceptedTypes = acceptedTypes;
    }

    @Override
    public FluidType getType(ItemStack stack, @Nullable Container inventory) {
        String type = IMagazine.magState(stack, index).type();
        if (type.isEmpty()) return acceptedTypes.length > 0 ? acceptedTypes[0] : null;
        try {
            return Fluids.fromID(Integer.parseInt(type));
        } catch (NumberFormatException e) {
            return acceptedTypes.length > 0 ? acceptedTypes[0] : null;
        }
    }

    @Override
    public void setType(ItemStack stack, FluidType type) {
        if (type != null) IMagazine.updateMagState(stack, index, s -> s.withType(String.valueOf(type.getID())));
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount) {
        this.setAmount(stack, this.getAmount(stack, inventory) - amount);
    }

    @Override
    public int getAmount(ItemStack stack, @Nullable Container inventory) {
        return IMagazine.magState(stack, index).amount();
    }

    @Override
    public void setAmount(ItemStack stack, int amount) {
        IMagazine.updateMagState(stack, index, s -> s.withAmount(amount));
    }

    @Override public boolean canReload(ItemStack stack, @Nullable Container inventory) { return false; }
    @Override public void initNewType(ItemStack stack, @Nullable Container inventory) { }
    @Override public void reloadAction(ItemStack stack, @Nullable Container inventory) { }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, Player player) {
        // CE returns new ItemStack(ModItems.fluid_icon, 1, <fluid id>) - no such flattened fluid-icon
        // item exists in this port yet (Phase 2/content scope). Empty stack until it lands.
        return ItemStack.EMPTY;
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        FluidType type = getType(stack, player.getInventory());
        return type != null ? type.getLocalizedName().getString() : "";
    }

    @Override
    public void setAmountBeforeReload(ItemStack stack, int amount) {
        IMagazine.updateMagState(stack, index, s -> s.withBefore(amount));
    }

    @Override
    public int getAmountBeforeReload(ItemStack stack) {
        return IMagazine.magState(stack, index).before();
    }

    @Override
    public void setAmountAfterReload(ItemStack stack, int amount) {
        IMagazine.updateMagState(stack, index, s -> s.withAfter(amount));
    }

    @Override
    public int getAmountAfterReload(ItemStack stack) {
        return IMagazine.magState(stack, index).after();
    }
}
