package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Port of CE's {@code WeaponModLiberatorSpeedloader} (54 lines) - converts the Liberator's
 * one-shell-at-a-time reload into a 4-round full-reload speedloader.
 * <p>
 * Unlike {@link WeaponModCaliber}/{@link WeaponModStackMag}, no fresh-instance adaptation is needed:
 * CE's own pattern here already only mutates {@link #MAG}'s mutable {@code acceptedBullets} list
 * contents (lazily filling it from whichever gun installs it first) - it never reassigns
 * {@code capacity}/{@code index}, both fixed at construction to {@code (0, 4)}, so this port's
 * {@code final} fields are no obstacle. CE's {@code GunConfig.FUN_ANIMNATIONS} branch (a speedloader
 * reload animation) is dropped - see {@link WeaponModSawedOff}'s class javadoc for why.
 */
public class WeaponModLiberatorSpeedloader extends WeaponModBase {

    public static final MagazineFullReload MAG = new MagazineFullReload(0, 4);

    public WeaponModLiberatorSpeedloader(String id) {
        super(id, "SPEEDLOADER");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (parent instanceof Receiver && Objects.equals(key, Receiver.O_MAGAZINE)
                && base instanceof MagazineSingleReload originalMag) {
            if (MAG.acceptedBullets.isEmpty()) MAG.acceptedBullets.addAll(originalMag.acceptedBullets);
            return cast(MAG, base);
        }
        return base;
    }
}
