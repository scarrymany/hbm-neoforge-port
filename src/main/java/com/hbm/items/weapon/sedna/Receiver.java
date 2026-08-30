package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.Receiver} (167 lines) - per-barrel/chamber "DNA"
 * nested one level under {@link GunConfig}: base damage, rounds-per-cycle, the 4 independent spread
 * terms (see {@code docs/phase3/gun_framework.md}'s headline finding #3 - spread is a pure, memoryless
 * sum recomputed every shot, not a buildup/decay "heat" mechanic), the full reload-duration 5-tuple,
 * jam duration, fire sound, the {@link IMagazine} instance, projectile spawn offsets (hip and
 * aimed-down-sights), and the 3 core behavior lambdas ({@link #canFire_DNA}/{@link #onFire_DNA}/
 * {@link #onRecoil_DNA}).
 * <p>
 * Every getter is a raw pass-through (see {@link ItemGunBaseNT}'s class javadoc for why - CE's
 * {@code XWeaponModManager.eval(...)} wrapper is Package C, not ported yet). {@link #magazine_DNA} is
 * typed {@code IMagazine<?>} (a wildcard, not CE's raw {@code IMagazine}) since CE's own consumer
 * ({@code Lego.doStandardFire}) already explicitly casts {@code mag.getType(...)} to
 * {@link BulletConfig} rather than relying on a concrete type parameter - a wildcard expresses that
 * exact "any ammo-unit flavor, caller casts" contract without Java's raw-type warnings.
 */
public class Receiver {

    public Receiver(int index) {
        this.index = index;
    }

    protected int index;
    protected GunConfig parent;
    protected float baseDamage_DNA;
    protected int delayAfterFire_DNA;
    protected int delayAfterDryFire_DNA;
    protected int roundsPerCycle_DNA = 1;
    protected float splitProjectiles_DNA = 1;
    protected float spreadInnate_DNA = 0F;
    protected float spreadMultAmmo_DNA = 1F;
    protected float spreadPenaltyHipfire_DNA = 0.025F;
    protected float spreadDurability_DNA = 0.125F;
    protected boolean refireOnHold_DNA = false;
    protected boolean refireAfterDry_DNA = false;
    protected boolean doesDryFire_DNA = true;
    protected boolean doesDryFireAfterAuto_DNA = false;
    protected boolean ejectOnFire_DNA = true;
    protected boolean reloadOnEmpty_DNA = false;
    protected int reloadBeginDuration_DNA;
    protected int reloadCycleDuration_DNA;
    protected int reloadEndDuration_DNA;
    protected int reloadCockOnEmptyPre_DNA;
    protected int reloadCockOnEmptyPost_DNA;
    protected int jamDuration_DNA = 0;
    protected SoundEvent fireSound_DNA;
    protected float fireVolume_DNA = 1.0F;
    protected float firePitch_DNA = 1.0F;
    protected IMagazine<?> magazine_DNA;
    protected Vec3 projectileOffset_DNA = new Vec3(0, 0, 0);
    protected Vec3 projectileOffsetScoped_DNA = new Vec3(0, 0, 0);
    protected BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> canFire_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onFire_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onRecoil_DNA;

    /* GETTERS - raw pass-throughs, see class javadoc */
    public float getBaseDamage(ItemStack stack) { return baseDamage_DNA; }
    public int getDelayAfterFire(ItemStack stack) { return delayAfterFire_DNA; }
    public int getDelayAfterDryFire(ItemStack stack) { return delayAfterDryFire_DNA; }
    public int getRoundsPerCycle(ItemStack stack) { return roundsPerCycle_DNA; }
    public float getSplitProjectiles(ItemStack stack) { return splitProjectiles_DNA; }
    public float getInnateSpread(ItemStack stack) { return spreadInnate_DNA; }
    public float getAmmoSpread(ItemStack stack) { return spreadMultAmmo_DNA; }
    public float getHipfireSpread(ItemStack stack) { return spreadPenaltyHipfire_DNA; }
    public float getDurabilitySpread(ItemStack stack) { return spreadDurability_DNA; }
    public boolean getRefireOnHold(ItemStack stack) { return refireOnHold_DNA; }
    public boolean getRefireAfterDry(ItemStack stack) { return refireAfterDry_DNA; }
    public boolean getDoesDryFire(ItemStack stack) { return doesDryFire_DNA; }
    public boolean getDoesDryFireAfterAuto(ItemStack stack) { return doesDryFireAfterAuto_DNA; }
    public boolean getEjectOnFire(ItemStack stack) { return ejectOnFire_DNA; }
    public boolean getReloadOnEmpty(ItemStack stack) { return reloadOnEmpty_DNA; }
    public int getReloadBeginDuration(ItemStack stack) { return reloadBeginDuration_DNA; }
    public int getReloadCycleDuration(ItemStack stack) { return reloadCycleDuration_DNA; }
    public int getReloadEndDuration(ItemStack stack) { return reloadEndDuration_DNA; }
    public int getReloadCockOnEmptyPre(ItemStack stack) { return reloadCockOnEmptyPre_DNA; }
    public int getReloadCockOnEmptyPost(ItemStack stack) { return reloadCockOnEmptyPost_DNA; }
    public int getJamDuration(ItemStack stack) { return jamDuration_DNA; }
    @Nullable
    public SoundEvent getFireSound(ItemStack stack) { return fireSound_DNA; }
    public float getFireVolume(ItemStack stack) { return fireVolume_DNA; }
    public float getFirePitch(ItemStack stack) { return firePitch_DNA; }
    public IMagazine<?> getMagazine(ItemStack stack) { return magazine_DNA; }
    public Vec3 getProjectileOffset(ItemStack stack) { return projectileOffset_DNA; }
    public Vec3 getProjectileOffsetScoped(ItemStack stack) { return projectileOffsetScoped_DNA; }

    public BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> getCanFire(ItemStack stack) { return canFire_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getOnFire(ItemStack stack) { return onFire_DNA; }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getRecoil(ItemStack stack) { return onRecoil_DNA; }

    /* SETTERS */
    public Receiver dmg(float dmg) { this.baseDamage_DNA = dmg; return this; }
    public Receiver delay(int delay) { this.delayAfterFire_DNA = this.delayAfterDryFire_DNA = delay; return this; }
    public Receiver dry(int delay) { this.delayAfterDryFire_DNA = delay; return this; }
    public Receiver rounds(int rounds) { this.roundsPerCycle_DNA = rounds; return this; }
    public Receiver split(float rounds) { this.splitProjectiles_DNA = rounds; return this; }
    public Receiver spread(float spread) { this.spreadInnate_DNA = spread; return this; }
    public Receiver spreadAmmo(float spread) { this.spreadMultAmmo_DNA = spread; return this; }
    public Receiver spreadHipfire(float spread) { this.spreadPenaltyHipfire_DNA = spread; return this; }
    public Receiver spreadDurability(float spread) { this.spreadDurability_DNA = spread; return this; }
    public Receiver auto(boolean auto) { this.refireOnHold_DNA = auto; return this; }
    public Receiver autoAfterDry(boolean auto) { this.refireAfterDry_DNA = auto; return this; }
    public Receiver dryfire(boolean dryfire) { this.doesDryFire_DNA = dryfire; return this; }
    public Receiver dryfireAfterAuto(boolean dryfire) { this.doesDryFireAfterAuto_DNA = dryfire; return this; }
    public Receiver ejectOnFire(boolean eject) { this.ejectOnFire_DNA = eject; return this; }
    public Receiver reloadOnEmpty(boolean reload) { this.reloadOnEmpty_DNA = reload; return this; }
    public Receiver mag(IMagazine<?> magazine) { this.magazine_DNA = magazine; return this; }
    public Receiver offset(double forward, double up, double side) {
        this.projectileOffset_DNA = new Vec3(forward, up, side);
        this.projectileOffsetScoped_DNA = new Vec3(forward, up, 0);
        return this;
    }
    public Receiver offsetScoped(double forward, double up, double side) { this.projectileOffsetScoped_DNA = new Vec3(forward, up, side); return this; }
    public Receiver jam(int jam) { this.jamDuration_DNA = jam; return this; }

    public Receiver reload(int delay) { return reload(0, delay, delay, 0, 0); }
    public Receiver reload(int begin, int cycle, int end, int cock) { return reload(0, begin, cycle, end, cock); }
    public Receiver reload(int pre, int begin, int cycle, int end, int post) {
        this.reloadBeginDuration_DNA = begin;
        this.reloadCycleDuration_DNA = cycle;
        this.reloadEndDuration_DNA = end;
        this.reloadCockOnEmptyPre_DNA = pre;
        this.reloadCockOnEmptyPost_DNA = post;
        return this;
    }

    public Receiver canFire(BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> lambda) { this.canFire_DNA = lambda; return this; }
    public Receiver fire(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onFire_DNA = lambda; return this; }
    public Receiver recoil(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onRecoil_DNA = lambda; return this; }

    public Receiver sound(SoundEvent sound, float volume, float pitch) {
        this.fireSound_DNA = sound;
        this.fireVolume_DNA = volume;
        this.firePitch_DNA = pitch;
        return this;
    }

    public Receiver setupStandardFire() { return this.canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE); }
    public Receiver setupLockonFire() { return this.canFire(Lego.LAMBDA_LOCKON_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE); }
}
