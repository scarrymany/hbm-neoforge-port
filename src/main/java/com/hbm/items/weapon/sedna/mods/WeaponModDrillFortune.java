package com.hbm.items.weapon.sedna.mods;

import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code WeaponModDrillFortune} (38 lines) - grants a Fortune enchantment bonus while
 * installed (used for the mining drill's "magnet"/"sifter" attachments).
 * <p>
 * <b>Stubbed.</b> CE's {@code onInstall}/{@code onUninstall} read/write the held gun's Fortune level
 * via {@code com.hbm.util.EnchantmentUtil.getEnchantmentLevel}/{@code add}/{@code removeEnchantment}
 * against 1.12's mutable {@code NBTTagList} enchantment format - that utility class does not exist in
 * this port, and 1.21's enchantment storage ({@code ItemEnchantments}/{@code Holder<Enchantment>},
 * registry-backed rather than a bare NBT list) is a structurally different API this Package C task's
 * scope does not cover. {@link #eval} is a no-op in CE too (this mod has no eval-time effect at all -
 * its entire behavior is the enchantment-level mutation), so this class is a complete no-op until a
 * real {@code EnchantmentUtil}-equivalent (or direct {@code ItemEnchantments} mutation) lands - it
 * still registers under its own id so the attachment item/slot mechanic works end-to-end, it just
 * doesn't grant the Fortune bonus yet.
 */
public class WeaponModDrillFortune extends WeaponModBase {

    protected final int addFortune;

    public WeaponModDrillFortune(String id, String slot, int fortune) {
        super(id, slot);
        this.setPriority(PRIORITY_ADDITIVE);
        this.addFortune = fortune;
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return base;
    }

    @Override
    public void onInstall(ItemStack gun, ItemStack mod, int index) {
        // TODO(items-tool): apply +addFortune to the gun's Fortune enchantment level once a real
        // EnchantmentUtil-equivalent (or direct ItemEnchantments mutation) exists - see class javadoc.
    }

    @Override
    public void onUninstall(ItemStack gun, ItemStack mod, int index) {
        // TODO(items-tool): reverse the above once it exists.
    }
}
