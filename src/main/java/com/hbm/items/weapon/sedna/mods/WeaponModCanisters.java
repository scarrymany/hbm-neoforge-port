package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineEnergy;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code WeaponModCanisters} (40 lines) - triples a fluid/energy-backed weapon's tank
 * capacity (mining drill's extra fuel canisters).
 * <p>
 * Constructs a fresh {@link MagazineFluid}/{@link MagazineEnergy} instance per {@link #eval} call
 * rather than mutating a shared dummy - see {@link WeaponModCaliber}'s class javadoc for why (this
 * port's {@code capacity}/{@code index} fields are {@code final}, unlike CE's mutable equivalents).
 */
public class WeaponModCanisters extends WeaponModBase {

    public WeaponModCanisters(String id) {
        super(id, "CANISTERS");
        this.setPriority(PRIORITY_MULT_FINAL);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.O_MAGAZINE) {
            if (base instanceof MagazineFluid original) {
                return cast(new MagazineFluid(original.index, original.capacity * 3, original.acceptedTypes), base);
            }
            if (base instanceof MagazineEnergy original) {
                return cast(new MagazineEnergy(original.index, original.capacity * 3), base);
            }
        }
        return base;
    }

    @Override
    public void onInstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }

    @Override
    public void onUninstall(ItemStack gun, ItemStack mod, int index) {
        XWeaponModManager.changedMagState();
    }
}
