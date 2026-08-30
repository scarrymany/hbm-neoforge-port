package com.hbm.items.weapon.sedna.mags;

import com.hbm.util.BobMathUtil;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.mags.MagazineEnergy} (54 lines) - backs the tesla
 * cannon/laser-weapon family. Read in full per this task's instruction: like {@link MagazineFluid},
 * confirmed to <b>not</b> read from this port's {@code com.hbm.api.energymk2}/{@code IBatteryItem}
 * capabilities - it is a fixed-capacity charge pool tracked purely as a flat NBT int on the gun stack
 * (this port: {@link com.hbm.items.weapon.sedna.MagState}, {@code type} left permanently empty since
 * this mag has no type concept at all), permanently non-reloadable via the standard reload cycle
 * (matching {@link MagazineFluid}). Whatever refills this pool (an {@code onPressReload} lambda
 * reading a held/worn battery item, most likely) is Package D content, not this mag class's concern.
 */
public class MagazineEnergy implements IMagazine<Void> {

    /** A number so the gun can tell multiple mags apart - see {@link MagazineSingleTypeBase}'s javadoc for the same contract. */
    public final int index;
    /** How much energy (HE) this mag can hold. */
    public final int capacity;

    public MagazineEnergy(int index, int capacity) {
        this.index = index;
        this.capacity = capacity;
    }

    @Override public Void getType(ItemStack stack, @Nullable Container inventory) { return null; }
    @Override public void setType(ItemStack stack, Void type) { }
    @Override public int getCapacity(ItemStack stack) { return capacity; }

    @Override
    public void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount) {
        this.setAmount(stack, Math.max(this.getAmount(stack, inventory) - amount, 0));
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
        // CE returns new ItemStack(ModItems.battery_creative) - no such item exists in this port yet.
        return ItemStack.EMPTY;
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return BobMathUtil.getShortNumber(getAmount(stack, player.getInventory())) + "/" + BobMathUtil.getShortNumber(this.capacity) + "HE";
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
