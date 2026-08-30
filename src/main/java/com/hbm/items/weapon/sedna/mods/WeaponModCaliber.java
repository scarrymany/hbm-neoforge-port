package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * Port of CE's {@code WeaponModCaliber} (68 lines) - swaps a gun's whole ammo family (e.g. a .45 ACP
 * conversion kit for a 9mm gun) and its base damage.
 * <p>
 * <b>Adaptation from CE, not a behavior change.</b> CE mutates 3 shared static "dummy" magazine
 * instances in place ({@code DUMMY_SINGLE.acceptedBullets = ...; DUMMY_SINGLE.capacity = ...}) since
 * CE's own {@code MagazineSingleReload}/{@code MagazineFullReload} fields are plain mutable Java
 * fields. This port's equivalent classes ({@code MagazineSingleTypeBase#capacity}/{@code #index})
 * are {@code final} (an earlier, already-landed package's deliberate immutability choice - see that
 * class's own javadoc) - mutating them in place is not possible. This class instead constructs a
 * fresh magazine instance per {@link #eval} call with the swapped capacity/ammo list, which is
 * observably identical (a magazine object is never cached or identity-compared anywhere in this
 * state machine - {@code Lego.doStandardFire} only ever reads through {@code Receiver.getMagazine},
 * itself called fresh every time) at the cost of one small allocation per evaluated getter call
 * instead of zero.
 */
public class WeaponModCaliber extends WeaponModBase {

    protected final List<BulletConfig> cfg;
    protected final int count;
    protected final float baseDamage;

    public WeaponModCaliber(String id, int count, float baseDamage, BulletConfig... cfg) {
        super(id, "CALIBER");
        this.setPriority(PRIORITY_SET);
        this.cfg = List.of(cfg);
        this.count = count;
        this.baseDamage = baseDamage;
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.O_MAGAZINE)) {
            if (base instanceof MagazineSingleReload original) {
                return cast(new MagazineSingleReload(original.index, count).addConfigs(cfg.toArray(new BulletConfig[0])), base);
            }
            if (base instanceof MagazineFullReload original) {
                return cast(new MagazineFullReload(original.index, count).addConfigs(cfg.toArray(new BulletConfig[0])), base);
            }
            if (base instanceof MagazineBelt) {
                return cast(new MagazineBelt().addConfigs(cfg.toArray(new BulletConfig[0])), base);
            }
        }
        if (Objects.equals(key, Receiver.F_BASEDAMAGE)) {
            return cast(baseDamage, base);
        }
        return base;
    }

    /* adding or removing a caliber mod annihilates the loaded rounds */
    @Override
    public void onInstall(ItemStack gun, ItemStack mod, int index) {
        clearMag(gun, index);
    }

    @Override
    public void onUninstall(ItemStack gun, ItemStack mod, int index) {
        clearMag(gun, index);
    }

    public void clearMag(ItemStack stack, int index) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> mag = gun.getConfig(stack, index).getReceivers(stack)[0].getMagazine(stack);
        if (mag instanceof MagazineSingleTypeBase mstb) {
            mstb.setAmount(stack, 0);
        }
    }
}
