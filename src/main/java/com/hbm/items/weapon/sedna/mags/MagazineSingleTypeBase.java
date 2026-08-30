package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.BobMathUtil;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase} (246 lines) - the
 * ammo-scanning reload implementation shared by {@link MagazineFullReload}/{@link MagazineSingleReload}.
 * {@link #standardReload} walks the firer's inventory slot-by-slot matching each {@link BulletConfig}'s
 * ammo item against {@link #acceptedBullets}, loading up to {@code loadLimit} rounds converted via
 * each config's own {@code ammoReloadCount}.
 * <p>
 * {@code index} disambiguates multiple mags on the same gun for {@link com.hbm.items.weapon.sedna.MagState}
 * storage purposes (see that class's javadoc) - a content author assigns it explicitly at construction
 * (e.g. {@code new MagazineFullReload(0, 6)}), and must keep it unique across every mag on the same
 * item stack (including mags belonging to a different {@code GunConfig} on a multi-config gun) to
 * avoid two mags silently sharing one {@link com.hbm.items.weapon.sedna.MagState} slot - exactly the
 * same contract CE's own {@code index}-keyed flat NBT keys had.
 * <p>
 * <b>Not ported</b>: the {@code ItemAmmoBag}/{@code ammo_bag_infinite} sub-inventory recursion CE's
 * {@link #standardReload}/{@link #getFirstConfig} both perform (loading from a held ammo bag, not
 * just the firer's main inventory slots) - {@code ItemAmmoBag}'s own javadoc documents its casing/ammo
 * sub-inventory as deferred pending a generic item-owned-inventory Menu/Screen framework this port
 * doesn't have yet. Main-inventory reloading (the overwhelming common case) works fully without it;
 * see the {@code TODO}s inline for the exact spot to add it back once that framework lands.
 */
public abstract class MagazineSingleTypeBase implements IMagazine<BulletConfig> {

    public final List<BulletConfig> acceptedBullets = new ArrayList<>();

    /** A number so the gun can tell multiple mags apart - see class javadoc. */
    public final int index;
    /** How much ammo this mag can hold. */
    public final int capacity;

    public MagazineSingleTypeBase(int index, int capacity) {
        this.index = index;
        this.capacity = capacity;
    }

    public MagazineSingleTypeBase addConfigs(BulletConfig... cfgs) {
        acceptedBullets.addAll(Arrays.asList(cfgs));
        return this;
    }

    /**
     * Returns the currently-loaded ammo type, falling back to {@link #acceptedBullets}'s first entry
     * when unset or invalid - see {@link com.hbm.items.weapon.sedna.MagState}'s javadoc for why this
     * differs slightly from CE's own {@code type >= 0 && type < BulletConfig.configs.size()} guard
     * (a global-list-index scheme this port's {@link BulletConfig} deliberately replaced).
     */
    @Override
    public BulletConfig getType(ItemStack stack, @Nullable Container inventory) {
        String type = IMagazine.magState(stack, index).type();
        if (!type.isEmpty()) {
            BulletConfig cfg = BulletConfig.byId(type);
            if (cfg != null && acceptedBullets.contains(cfg)) return cfg;
        }
        return acceptedBullets.isEmpty() ? null : acceptedBullets.get(0);
    }

    @Override
    public void setType(ItemStack stack, BulletConfig type) {
        if (type != null) IMagazine.updateMagState(stack, index, s -> s.withType(type.id.toString()));
    }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, Player player) {
        BulletConfig config = this.getType(stack, player.getInventory());
        return config != null && config.getAmmo() != null ? config.getAmmo().toStack() : ItemStack.EMPTY;
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return getAmount(stack, player.getInventory()) + " / " + getCapacity(stack);
    }

    @Override
    public void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount) {
        this.setAmount(stack, this.getAmount(stack, inventory) - amount);
        IMagazine.handleAmmoBag(inventory, this.getType(stack, inventory), amount);
    }

    /** Returns true if the player has the same ammo (if partially loaded) or any valid ammo (if not). */
    @Override
    public boolean canReload(ItemStack stack, @Nullable Container inventory) {
        if (this.getAmount(stack, inventory) >= this.getCapacity(stack)) return false;
        if (inventory == null) return true;
        return getFirstConfig(stack, inventory) != null;
    }

    public void standardReload(ItemStack stack, @Nullable Container inventory, int loadLimit) {

        if (inventory == null) {
            BulletConfig config = this.getType(stack, null);
            if (config == null) {
                if (acceptedBullets.isEmpty()) return;
                config = this.acceptedBullets.get(0);
                this.setType(stack, config);
            }
            this.setAmount(stack, this.capacity);
            return;
        }

        for (int i = 0; i < inventory.getContainerSize() && loadLimit > 0; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) continue;

            // mag is empty, assume next best type
            if (this.getAmount(stack, null) == 0) {

                for (BulletConfig config : this.acceptedBullets) {
                    if (config.getAmmo() != null && config.getAmmo().matchesRecipe(slot, true)) {
                        this.setType(stack, config);
                        int wantsToLoad = (int) Math.ceil((double) this.getCapacity(stack) / (double) config.ammoReloadCount);
                        int toLoad = BobMathUtil.min(wantsToLoad, slot.getCount(), loadLimit);
                        this.setAmount(stack, Math.min(toLoad * config.ammoReloadCount, this.capacity));
                        inventory.removeItem(i, toLoad);
                        loadLimit -= toLoad;
                        break;
                    }
                }
                // mag has a type set, only load that
            } else {
                BulletConfig config = this.getType(stack, null);
                if (config == null) {
                    if (acceptedBullets.isEmpty()) continue;
                    config = this.acceptedBullets.get(0);
                    this.setType(stack, config);
                }

                if (config.getAmmo() != null && config.getAmmo().matchesRecipe(slot, true)) {
                    int alreadyLoaded = this.getAmount(stack, null);
                    int wantsToLoad = (int) Math.ceil((double) (this.getCapacity(stack) - alreadyLoaded) / (double) config.ammoReloadCount);
                    int toLoad = BobMathUtil.min(wantsToLoad, slot.getCount(), loadLimit);
                    this.setAmount(stack, Math.min((toLoad * config.ammoReloadCount) + alreadyLoaded, this.capacity));
                    inventory.removeItem(i, toLoad);
                    loadLimit -= toLoad;
                }
            }

            // TODO(items-tool): CE also recurses into a held ItemAmmoBag/ammo_bag_infinite slot here -
            // see class javadoc. Skipped gracefully; main-inventory reloading is unaffected.
        }
    }

    /** Returns the config of the first potential loadable round, either what's already chambered or the first valid one if empty. */
    public BulletConfig getFirstConfig(ItemStack stack, @Nullable Container inventory) {
        if (inventory == null) return null;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) continue;

            if (this.getAmount(stack, null) == 0) {
                for (BulletConfig config : this.acceptedBullets) {
                    if (config.getAmmo() != null && config.getAmmo().matchesRecipe(slot, true)) return config;
                }
            } else {
                BulletConfig config = this.getType(stack, null);
                if (config == null) {
                    if (acceptedBullets.isEmpty()) continue;
                    config = this.acceptedBullets.get(0);
                    this.setType(stack, config);
                }
                if (config.getAmmo() != null && config.getAmmo().matchesRecipe(slot, true)) return config;
            }

            // TODO(items-tool): ammo-bag recursion, see standardReload's own TODO.
        }

        return null;
    }

    @Override
    public void initNewType(ItemStack stack, @Nullable Container inventory) {
        if (inventory == null) return;
        BulletConfig nextConfig = getFirstConfig(stack, inventory);
        if (nextConfig != null) this.setType(stack, nextConfig);
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public int getAmount(ItemStack stack, @Nullable Container inventory) {
        return IMagazine.magState(stack, index).amount();
    }

    @Override
    public void setAmount(ItemStack stack, int amount) {
        IMagazine.updateMagState(stack, index, s -> s.withAmount(amount));
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
