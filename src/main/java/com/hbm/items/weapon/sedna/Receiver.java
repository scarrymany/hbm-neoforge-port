package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
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
 * <b>Every getter routes through {@link XWeaponModManager#eval}</b> (Package C, now landed), keyed on
 * {@code parent.index} (the owning {@link GunConfig}'s config index - CE's own {@code Receiver}
 * getters key on {@code parent.index} too, not this receiver's own {@link #index}, since the
 * installed-mod list is stored per {@code GunConfig} slot, not per receiver) - matches CE 1:1.
 * {@link #magazine_DNA} is typed {@code IMagazine<?>} (a wildcard, not CE's raw {@code IMagazine})
 * since CE's own consumer ({@code Lego.doStandardFire}) already explicitly casts
 * {@code mag.getType(...)} to {@link BulletConfig} rather than relying on a concrete type parameter -
 * a wildcard expresses that exact "any ammo-unit flavor, caller casts" contract without Java's
 * raw-type warnings.
 */
public class Receiver {

    /* MOD-EVAL KEYS - mirrors CE's own {@code Receiver} constant names 1:1, see {@link GunConfig}'s javadoc for why this matters. */
    public static final String F_BASEDAMAGE = "F_BASEDAMAGE";
    public static final String I_DELAYAFTERFIRE = "I_DELAYAFTERFIRE";
    public static final String I_DELAYAFTERDRYFIRE = "I_DELAYAFTERDRYFIRE";
    public static final String I_ROUNDSPERCYCLE = "I_ROUNDSPERCYCLE";
    public static final String F_SPLITPROJECTILES = "F_SPLITPROJECTILES";
    public static final String F_SPREADINNATE = "F_SPREADINNATE";
    public static final String F_SPREADAMMO = "F_SPREADAMMO";
    public static final String F_SPREADHIPFIRE = "F_SPREADHIPFIRE";
    public static final String F_SPREADDURABILITY = "F_SPREADDURABILITY";
    public static final String B_REFIREONHOLD = "B_REFIREONHOLD";
    public static final String B_REFIREAFTERDRY = "B_REFIREAFTERDRY";
    public static final String B_DOESDRYFIRE = "B_DOESDRYFIRE";
    public static final String B_DOESDRYFIREAFTERAUTO = "B_DOESDRYFIREAFTERAUTO";
    public static final String B_EJECTONFIRE = "B_EJECTONFIRE";
    public static final String B_RELOADONEMPTY = "B_RELOADONEMPTY";
    public static final String I_RELOADBEGINDURATION = "I_RELOADBEGINDURATION";
    public static final String I_RELOADCYCLEDURATION = "I_RELOADCYCLEDURATION";
    public static final String I_RELOADENDDURATION = "I_RELOADENDDURATION";
    public static final String I_RELOADCOCKONEMPTYPRE = "I_RELOADCOCKONEMPTYPRE";
    public static final String I_RELOADCOCKONEMPTYPOST = "I_RELOADCOCKONEMPTYPOST";
    public static final String I_JAMDURATION = "I_JAMDURATION";
    public static final String S_FIRESOUND = "S_FIRESOUND";
    public static final String F_FIREVOLUME = "F_FIREVOLUME";
    public static final String F_FIREPITCH = "F_FIREPITCH";
    public static final String O_MAGAZINE = "O_MAGAZINE";
    public static final String O_PROJECTILEOFFSET = "O_PROJECTILEOFFSET";
    public static final String O_PROJECTILEOFFSETSCOPED = "O_PROJECTILEOFFSETSCOPED";
    public static final String FUN_CANFIRE = "FUN_CANFIRE";
    public static final String CON_ONFIRE = "CON_ONFIRE";
    public static final String CON_ONRECOIL = "CON_ONRECOIL";

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

    /* GETTERS - every value routes through XWeaponModManager.eval, keyed on parent.index, see class javadoc */
    public float getBaseDamage(ItemStack stack) { return XWeaponModManager.eval(baseDamage_DNA, stack, F_BASEDAMAGE, this, parent.index); }
    public int getDelayAfterFire(ItemStack stack) { return XWeaponModManager.eval(delayAfterFire_DNA, stack, I_DELAYAFTERFIRE, this, parent.index); }
    public int getDelayAfterDryFire(ItemStack stack) { return XWeaponModManager.eval(delayAfterDryFire_DNA, stack, I_DELAYAFTERDRYFIRE, this, parent.index); }
    public int getRoundsPerCycle(ItemStack stack) { return XWeaponModManager.eval(roundsPerCycle_DNA, stack, I_ROUNDSPERCYCLE, this, parent.index); }
    public float getSplitProjectiles(ItemStack stack) { return XWeaponModManager.eval(splitProjectiles_DNA, stack, F_SPLITPROJECTILES, this, parent.index); }
    public float getInnateSpread(ItemStack stack) { return XWeaponModManager.eval(spreadInnate_DNA, stack, F_SPREADINNATE, this, parent.index); }
    public float getAmmoSpread(ItemStack stack) { return XWeaponModManager.eval(spreadMultAmmo_DNA, stack, F_SPREADAMMO, this, parent.index); }
    public float getHipfireSpread(ItemStack stack) { return XWeaponModManager.eval(spreadPenaltyHipfire_DNA, stack, F_SPREADHIPFIRE, this, parent.index); }
    public float getDurabilitySpread(ItemStack stack) { return XWeaponModManager.eval(spreadDurability_DNA, stack, F_SPREADDURABILITY, this, parent.index); }
    public boolean getRefireOnHold(ItemStack stack) { return XWeaponModManager.eval(refireOnHold_DNA, stack, B_REFIREONHOLD, this, parent.index); }
    public boolean getRefireAfterDry(ItemStack stack) { return XWeaponModManager.eval(refireAfterDry_DNA, stack, B_REFIREAFTERDRY, this, parent.index); }
    public boolean getDoesDryFire(ItemStack stack) { return XWeaponModManager.eval(doesDryFire_DNA, stack, B_DOESDRYFIRE, this, parent.index); }
    public boolean getDoesDryFireAfterAuto(ItemStack stack) { return XWeaponModManager.eval(doesDryFireAfterAuto_DNA, stack, B_DOESDRYFIREAFTERAUTO, this, parent.index); }
    public boolean getEjectOnFire(ItemStack stack) { return XWeaponModManager.eval(ejectOnFire_DNA, stack, B_EJECTONFIRE, this, parent.index); }
    public boolean getReloadOnEmpty(ItemStack stack) { return XWeaponModManager.eval(reloadOnEmpty_DNA, stack, B_RELOADONEMPTY, this, parent.index); }
    public int getReloadBeginDuration(ItemStack stack) { return XWeaponModManager.eval(reloadBeginDuration_DNA, stack, I_RELOADBEGINDURATION, this, parent.index); }
    public int getReloadCycleDuration(ItemStack stack) { return XWeaponModManager.eval(reloadCycleDuration_DNA, stack, I_RELOADCYCLEDURATION, this, parent.index); }
    public int getReloadEndDuration(ItemStack stack) { return XWeaponModManager.eval(reloadEndDuration_DNA, stack, I_RELOADENDDURATION, this, parent.index); }
    public int getReloadCockOnEmptyPre(ItemStack stack) { return XWeaponModManager.eval(reloadCockOnEmptyPre_DNA, stack, I_RELOADCOCKONEMPTYPRE, this, parent.index); }
    public int getReloadCockOnEmptyPost(ItemStack stack) { return XWeaponModManager.eval(reloadCockOnEmptyPost_DNA, stack, I_RELOADCOCKONEMPTYPOST, this, parent.index); }
    public int getJamDuration(ItemStack stack) { return XWeaponModManager.eval(jamDuration_DNA, stack, I_JAMDURATION, this, parent.index); }
    @Nullable
    public SoundEvent getFireSound(ItemStack stack) { return XWeaponModManager.eval(fireSound_DNA, stack, S_FIRESOUND, this, parent.index); }
    public float getFireVolume(ItemStack stack) { return XWeaponModManager.eval(fireVolume_DNA, stack, F_FIREVOLUME, this, parent.index); }
    public float getFirePitch(ItemStack stack) { return XWeaponModManager.eval(firePitch_DNA, stack, F_FIREPITCH, this, parent.index); }
    public IMagazine<?> getMagazine(ItemStack stack) { return XWeaponModManager.eval(magazine_DNA, stack, O_MAGAZINE, this, parent.index); }
    public Vec3 getProjectileOffset(ItemStack stack) { return XWeaponModManager.eval(projectileOffset_DNA, stack, O_PROJECTILEOFFSET, this, parent.index); }
    public Vec3 getProjectileOffsetScoped(ItemStack stack) { return XWeaponModManager.eval(projectileOffsetScoped_DNA, stack, O_PROJECTILEOFFSETSCOPED, this, parent.index); }

    public BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> getCanFire(ItemStack stack) { return XWeaponModManager.eval(canFire_DNA, stack, FUN_CANFIRE, this, parent.index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getOnFire(ItemStack stack) { return XWeaponModManager.eval(onFire_DNA, stack, CON_ONFIRE, this, parent.index); }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getRecoil(ItemStack stack) { return XWeaponModManager.eval(onRecoil_DNA, stack, CON_ONRECOIL, this, parent.index); }

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
