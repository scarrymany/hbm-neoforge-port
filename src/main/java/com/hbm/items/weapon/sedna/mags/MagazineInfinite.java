package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.weapon.sedna.BulletConfig;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Port of CE's {@code MagazineInfinite} - a no-op {@link IMagazine}: fixed 9999 capacity/amount,
 * every mutator a no-op, {@link #canReload} always {@code false}. Used for debug/creative-only guns.
 * Carries no {@link com.hbm.items.weapon.sedna.MagState} at all (there is nothing to persist).
 */
public class MagazineInfinite implements IMagazine<BulletConfig> {

    public final BulletConfig type;

    public MagazineInfinite(BulletConfig type) {
        this.type = type;
    }

    @Override
    public BulletConfig getType(ItemStack stack, @Nullable Container inventory) {
        return this.type;
    }

    @Override
    public void setType(ItemStack stack, BulletConfig type) {
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return 9999;
    }

    @Override
    public int getAmount(ItemStack stack, @Nullable Container inventory) {
        return 9999;
    }

    @Override
    public void setAmount(ItemStack stack, int amount) {
    }

    @Override
    public void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount) {
    }

    @Override
    public boolean canReload(ItemStack stack, @Nullable Container inventory) {
        return false;
    }

    @Override
    public void initNewType(ItemStack stack, @Nullable Container inventory) {
    }

    @Override
    public void reloadAction(ItemStack stack, @Nullable Container inventory) {
    }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, Player player) {
        // CE returns new ItemStack(ModItems.nothing) - no such placeholder item exists in this port
        // yet; empty stack is the safe fallback (matches RecipesCommon.AStack's own established
        // fallback for the same missing placeholder, see that class's javadoc).
        return ItemStack.EMPTY;
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return "∞"; // infinity symbol
    }

    @Override
    public void setAmountBeforeReload(ItemStack stack, int amount) {
    }

    @Override
    public int getAmountBeforeReload(ItemStack stack) {
        return 9999;
    }

    @Override
    public void setAmountAfterReload(ItemStack stack, int amount) {
    }

    @Override
    public int getAmountAfterReload(ItemStack stack) {
        return 9999;
    }
}
