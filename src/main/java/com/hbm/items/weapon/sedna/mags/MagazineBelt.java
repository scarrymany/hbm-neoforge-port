package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.weapon.sedna.BulletConfig;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.mags.MagazineBelt} (171 lines) - belt-fed weapons
 * (miniguns). No persisted "loaded count" at all: {@link #getAmount}/{@link #useUpAmmo} scan the
 * firer's inventory live every call, falling through to a held ammo bag the same way
 * {@link MagazineSingleTypeBase} does (not ported here either, see this class's own TODOs - same
 * missing {@code ItemAmmoBag} sub-inventory dependency).
 * <p>
 * {@link #getAmount(ItemStack, Container)} returning {@code 1} for a {@code null} inventory is a
 * deliberate CE special case: "mobs always have exactly 1 round available," sidestepping the
 * inventory scan entirely for non-player firers (CE's own comment on this exact line).
 * <p>
 * <b>Simplification from CE, documented rather than silent</b>: CE's own {@code getType} writes a
 * cached {@code magtype} value on every call (comparing it to the freshly-scanned config's id) but
 * never actually reads that cache back to influence any decision - {@code getType}'s return value is
 * always the freshly-scanned config, and {@code getFirstConfig}'s own cache-fallback branch (used
 * only when the live inventory scan finds nothing) resolves to the exact same
 * {@code acceptedBullets.get(0)} the cache would have degenerated to anyway once this port's
 * {@link BulletConfig} switched to id-keyed lookup (see that class's javadoc). This port omits the
 * write-only cache field entirely rather than porting dead state - {@link #getFirstConfig}'s no-match
 * fallback goes straight to {@code acceptedBullets.get(0)}, which is observably identical.
 */
public class MagazineBelt implements IMagazine<BulletConfig> {

    public final List<BulletConfig> acceptedBullets = new ArrayList<>();

    public MagazineBelt addConfigs(BulletConfig... cfgs) {
        Collections.addAll(acceptedBullets, cfgs);
        return this;
    }

    @Override
    public BulletConfig getType(ItemStack stack, @Nullable Container inventory) {
        return getFirstConfig(stack, inventory);
    }

    @Override
    public void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount) {
        if (inventory == null) return;

        BulletConfig first = this.getFirstConfig(stack, inventory);
        if (first == null || first.getAmmo() == null) return;

        for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) continue;

            if (first.getAmmo().matchesRecipe(slot, true)) {
                int toRemove = Math.min(slot.getCount(), amount);
                amount -= toRemove;
                inventory.removeItem(i, toRemove);
                IMagazine.handleAmmoBag(inventory, first, toRemove);
            }

            // TODO(items-tool): CE also recurses into a held ItemAmmoBag/ammo_bag_infinite slot here -
            // see MagazineSingleTypeBase's own equivalent TODO for why it's deferred.
        }
    }

    @Override public void setType(ItemStack stack, BulletConfig type) { }
    @Override public int getCapacity(ItemStack stack) { return 0; }
    @Override public void setAmount(ItemStack stack, int amount) { }
    @Override public boolean canReload(ItemStack stack, @Nullable Container inventory) { return false; }
    @Override public void initNewType(ItemStack stack, @Nullable Container inventory) { }
    @Override public void reloadAction(ItemStack stack, @Nullable Container inventory) { }
    @Override public void setAmountBeforeReload(ItemStack stack, int amount) { }
    @Override public int getAmountBeforeReload(ItemStack stack) { return 0; }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) { }
    @Override public int getAmountAfterReload(ItemStack stack) { return 0; }

    @Override
    public int getAmount(ItemStack stack, @Nullable Container inventory) {
        if (inventory == null) return 1; // for mob-held guns, see class javadoc

        BulletConfig first = this.getFirstConfig(stack, inventory);
        if (first == null || first.getAmmo() == null) return 0;

        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && first.getAmmo().matchesRecipe(slot, true)) count += slot.getCount();

            // TODO(items-tool): ammo-bag recursion, see useUpAmmo's own TODO.
        }
        return count;
    }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, Player player) {
        BulletConfig first = this.getFirstConfig(stack, player.getInventory());
        return first != null && first.getAmmo() != null ? first.getAmmo().toStack() : ItemStack.EMPTY;
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return "x" + getAmount(stack, player.getInventory());
    }

    public BulletConfig getFirstConfig(ItemStack stack, @Nullable Container inventory) {

        if (inventory == null) return acceptedBullets.isEmpty() ? null : acceptedBullets.get(0);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) continue;

            for (BulletConfig config : this.acceptedBullets) {
                if (config.getAmmo() != null && config.getAmmo().matchesRecipe(slot, true)) return config;
            }

            // TODO(items-tool): ammo-bag recursion, see class javadoc's other TODOs.
        }

        // No match found live in the inventory - see class javadoc for why this skips CE's own
        // write-only magtype cache and falls straight through to the same effective default.
        return acceptedBullets.isEmpty() ? null : acceptedBullets.get(0);
    }
}
