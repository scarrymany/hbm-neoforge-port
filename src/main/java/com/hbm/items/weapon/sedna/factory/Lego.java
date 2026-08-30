package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBaseMK4CL;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfig.ProjectileType;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.weapon.anim.GunAnimationType;
import com.hbm.weapon.anim.HbmAnimationType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * "LEGO" - standardized building blocks used to set up gun configs easily. Port of CE's
 * {@code com.hbm.items.weapon.sedna.factory.Lego} (the server-relevant ~290 of 387 lines - the
 * remainder is a client-only smoke-particle helper, see {@code docs/phase3/gun_framework.md}'s
 * Package B table and this class's own javadoc for exactly what is and isn't ported).
 * <p>
 * <b>{@link #calcDamage(float, float)}/{@link #calcSpread(float, float, float, float, boolean, float, float)}
 * are the pure ballistics-math core</b> PORT_SPEC explicitly asks to be unit-testable - plain
 * {@code float}-in/{@code float}-out functions with no {@link ItemStack}/ballistics-context
 * dependency at all, extracted from CE's stack/context-shaped {@code calcDamage}/{@code calcSpread}
 * (kept below, unchanged in name and call shape, now thin wrappers over the pure versions).
 * <p>
 * <b>Fire dispatch is synchronous</b> - CE's {@code doStandardFire} defers its projectile-spawn
 * {@link Runnable} through {@code WorldServer#addScheduledTask}, which has no 1.21.1 equivalent.
 * Per {@code docs/phase3/gun_framework.md}'s "Key design decisions" (CE's own use of
 * {@code addScheduledTask} here reads as defensive 1.12-era caution against re-entrant world
 * mutation during an item-tick callback, not a load-bearing ordering requirement - flagged, not
 * proven, by that report), this port spawns every projectile directly and synchronously from the
 * keybind/decider call, exactly where CE's deferred task would have run moments later in the same
 * tick. If a future package ever finds a real re-entrancy bug traceable to this, that is the
 * open question this decision resolves in favor of "assume safe" for now.
 * <p>
 * <b>Not ported</b> (see {@code docs/phase3/gun_framework.md}'s own recommendation): {@code standardExplode}/
 * {@code tinyExplode}/{@code resolveImpactFacing} (explosive-ammo impact handlers - the report
 * explicitly scopes these to whichever package/content wires an ammo's {@code onImpact} to an
 * explosion, i.e. Package D content built on top of the already-ported
 * {@code com.hbm.explosion.vanillant.*} engine, not this state-machine package); {@code LAMBDA_DEBUG_ANIMS}
 * (needs {@code BusAnimationSedna}, unported Phase 5 keyframe data, see {@code GunConfig}'s javadoc);
 * the client-only smoke-particle helper (cosmetic-only, zero gameplay effect, see
 * {@code ItemGunBaseNT}'s javadoc).
 */
public class Lego {

    /** If IDLE and the mag of receiver 0 can be loaded, set state to RELOADING. Used by keybinds. */
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_RELOAD = (stack, ctx) -> {

        Player player = ctx.getPlayer();
        Receiver rec = ctx.config.getReceivers(stack)[0];
        GunState state = ItemGunBaseNT.getState(stack, ctx.configIndex);

        if (state == GunState.IDLE) {

            ItemGunBaseNT.setIsAiming(stack, false);
            IMagazine<?> mag = rec.getMagazine(stack);

            if (mag.canReload(stack, ctx.inventory)) {
                int loaded = mag.getAmount(stack, ctx.inventory);
                mag.setAmountBeforeReload(stack, loaded);
                ItemGunBaseNT.setState(stack, ctx.configIndex, GunState.RELOADING);
                ItemGunBaseNT.setTimer(stack, ctx.configIndex, rec.getReloadBeginDuration(stack) + (loaded <= 0 ? rec.getReloadCockOnEmptyPre(stack) : 0));
                ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.RELOAD, ctx.configIndex);
                if (ctx.config.getReloadChangesType(stack)) mag.initNewType(stack, ctx.inventory);
            } else {
                ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.INSPECT, ctx.configIndex);
                if (!ctx.config.getInspectCancel(stack)) {
                    ItemGunBaseNT.setState(stack, ctx.configIndex, GunState.DRAWING);
                    ItemGunBaseNT.setTimer(stack, ctx.configIndex, ctx.config.getInspectDuration(stack));
                }
            }
        }
    };

    /** If IDLE and ammo is loaded, fire and set to COOLDOWN. */
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_PRIMARY = (stack, ctx) -> clickReceiver(stack, ctx, 0);

    public static void clickReceiver(ItemStack stack, LambdaContext ctx, int receiver) {

        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        Receiver rec = ctx.config.getReceivers(stack)[receiver];
        int index = ctx.configIndex;
        GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == GunState.IDLE) {

            if (rec.getCanFire(stack).apply(stack, ctx)) {
                rec.getOnFire(stack).accept(stack, ctx);

                if (rec.getFireSound(stack) != null) {
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), rec.getFireSound(stack), SoundSource.PLAYERS, rec.getFireVolume(stack), rec.getFirePitch(stack));
                }

                int remaining = rec.getRoundsPerCycle(stack) - 1;
                for (int i = 0; i < remaining; i++) {
                    if (rec.getCanFire(stack).apply(stack, ctx)) rec.getOnFire(stack).accept(stack, ctx);
                }

                ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
            } else {

                if (rec.getDoesDryFire(stack)) {
                    ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.CYCLE_DRY, index);
                    ItemGunBaseNT.setState(stack, index, rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
                    ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
                }
            }
        }

        if (state == GunState.RELOADING) {
            ItemGunBaseNT.setReloadCancel(stack, true);
        }
    }

    /** If IDLE, switch mode between 0 and 1. */
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_SECONDARY = (stack, ctx) -> {

        LivingEntity entity = ctx.entity;
        int index = ctx.configIndex;
        GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == GunState.IDLE) {
            int mode = ItemGunBaseNT.getMode(stack, 0);
            ItemGunBaseNT.setMode(stack, index, 1 - mode);
            if (mode == 0) {
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), HBMSoundHandler.switchmode1.get(), SoundSource.PLAYERS, 1F, 1F);
            } else {
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), HBMSoundHandler.switchmode2.get(), SoundSource.PLAYERS, 1F, 1F);
            }
        }
    };

    /** Toggles isAiming. Used by keybinds. */
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_TOGGLE_AIM = (stack, ctx) -> ItemGunBaseNT.setIsAiming(stack, !ItemGunBaseNT.getIsAiming(stack));

    /** Returns true if the mag has ammo in it. Used by keybind functions on whether to fire, and deciders on whether to trigger a refire. */
    public static final BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_STANDARD_CAN_FIRE =
            (stack, ctx) -> ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0;

    /** Returns true if the mag has ammo in it, and the gun is in the locked-on state. */
    public static final BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_LOCKON_CAN_FIRE =
            (stack, ctx) -> ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0 && ItemGunBaseNT.getIsLockedOn(stack);

    /** JUMPER - bypasses mag testing and just allows constant fire. */
    public static final BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_DEBUG_CAN_FIRE = (stack, ctx) -> true;

    /** Spawns an {@link EntityBulletBaseMK4} (or beam/chunk-loading variant) with the loaded {@link BulletConfig}. */
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_FIRE = (stack, ctx) -> doStandardFire(stack, ctx, GunAnimationType.CYCLE, 0, true);
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_FIRE = (stack, ctx) -> doStandardFire(stack, ctx, GunAnimationType.CYCLE, 1, true);
    /** Same as {@link #LAMBDA_STANDARD_FIRE}, ignores wear. */
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_NOWEAR_FIRE = (stack, ctx) -> doStandardFire(stack, ctx, GunAnimationType.CYCLE, 0, false);
    /** Same as {@link #LAMBDA_STANDARD_FIRE}, then resets lockon progress. */
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_LOCKON_FIRE = (stack, ctx) -> {
        doStandardFire(stack, ctx, GunAnimationType.CYCLE, 0, true);
        ItemGunBaseNT.setIsLockedOn(stack, false);
    };

    public static void doStandardFire(ItemStack stack, LambdaContext ctx, HbmAnimationType anim, int receiver, boolean calcWear) {
        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        if (anim != null) ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex);

        boolean aim = ItemGunBaseNT.getIsAiming(stack);
        Receiver primary = ctx.config.getReceivers(stack)[receiver];
        IMagazine<?> mag = primary.getMagazine(stack);
        BulletConfig config = (BulletConfig) mag.getType(stack, ctx.inventory);

        Vec3 offset = aim ? primary.getProjectileOffsetScoped(stack) : primary.getProjectileOffset(stack);
        double forwardOffset = offset.x;
        double heightOffset = offset.y;
        double sideOffset = offset.z;

        float split = primary.getSplitProjectiles(stack);
        int projMin = (int) (config.projectilesMin * split);
        int projMax = (int) (config.projectilesMax * split);
        int projectiles = projMin;
        if (projMax > projMin) projectiles += entity.getRandom().nextInt(projMax - projMin + 1);
        Level level = entity.level();

        // Spawned synchronously, not via CE's WorldServer#addScheduledTask - see class javadoc.
        for (int i = 0; i < projectiles; i++) {
            float damage = calcDamage(ctx, stack, primary, calcWear, index);
            float spread = calcSpread(ctx, stack, primary, config, calcWear, index, aim);

            if (config.pType == ProjectileType.BULLET) {
                EntityBulletBaseMK4 mk4 = new EntityBulletBaseMK4(entity, config, damage, spread, sideOffset, heightOffset, forwardOffset);
                if (ItemGunBaseNT.getIsLockedOn(stack) && level.getEntity(ItemGunBaseNT.getLockonTarget(stack)) instanceof Entity target) mk4.lockonTarget = target;
                // TODO(phase5-particles): CE spawns a BlackPowderCreator smoke-composition effect here
                // for config.blackPowder ammo on the first projectile - BlackPowderCreator is unported
                // client-rendering particle code, out of this package's scope.
                level.addFreshEntity(mk4);
            } else if (config.pType == ProjectileType.BULLET_CHUNKLOADING) {
                EntityBulletBaseMK4CL mk4 = new EntityBulletBaseMK4CL(entity, config, damage, spread, sideOffset, heightOffset, forwardOffset);
                if (ItemGunBaseNT.getIsLockedOn(stack) && level.getEntity(ItemGunBaseNT.getLockonTarget(stack)) instanceof Entity target) mk4.lockonTarget = target;
                level.addFreshEntity(mk4);
            } else if (config.pType == ProjectileType.BEAM) {
                EntityBulletBeamBase beam = new EntityBulletBeamBase(entity, config, damage, spread, sideOffset, heightOffset, forwardOffset);
                level.addFreshEntity(beam);
            }
        }

        // TODO(stats): CE calls player.addStat(MainRegistry.statBullets, 1) here - no equivalent stat
        // exists in this port yet; skipped, no gameplay effect.
        mag.useUpAmmo(stack, ctx.inventory, 1);
        if (calcWear) {
            ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + config.wear, ctx.config.getDurability(stack)));
        }
    }

    /** 0 below 50% wear, ramping 0-1 from 50% to 100% wear - the wear-driven spread term's raw fraction. */
    public static float wearSpreadFraction(float wearPercent) {
        if (wearPercent < 0.5F) return 0F;
        return (wearPercent - 0.5F) * 2F;
    }

    /** 1 below 75% wear, ramping down (and eventually negative) from 75% to 100%+ wear - the wear-driven damage multiplier's raw fraction. */
    public static float wearDamageFraction(float wearPercent) {
        if (wearPercent < 0.75F) return 1F;
        return 1F - (wearPercent - 0.75F) * 2F;
    }

    /** Returns the standard multiplier for spread caused by wear, taking the stack's live wear/durability. */
    public static float getStandardWearSpread(ItemStack stack, GunConfig config, int index) {
        return wearSpreadFraction(ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack));
    }

    /** Returns the standard multiplier for damage based on wear, taking the stack's live wear/durability. */
    public static float getStandardWearDamage(ItemStack stack, GunConfig config, int index) {
        return wearDamageFraction(ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack));
    }

    /**
     * Pure ballistics-math core - the actual "unit-testable" function PORT_SPEC asks for. No
     * {@link ItemStack}/context dependency at all: given a base damage and a raw wear fraction
     * (0 = brand new, 1 = fully worn), returns the final damage.
     */
    public static float calcDamage(float baseDamage, float wearPercent) {
        return baseDamage * wearDamageFraction(wearPercent);
    }

    /**
     * Pure ballistics-math core, see {@link #calcDamage(float, float)}. The 4 independent spread
     * terms summed (see {@link Receiver}'s javadoc for why this is a memoryless sum recomputed fresh
     * every shot, not a buildup/decay "heat" mechanic): the gun's innate spread, the loaded ammo's
     * spread scaled by the gun's ammo-spread modifier, a flat hip-fire penalty (zeroed while aiming),
     * and a wear-driven term (raw wear fraction, scaled by the gun's own durability-spread multiplier).
     */
    public static float calcSpread(float innateSpread, float ammoSpread, float ammoSpreadMult, float hipfirePenalty, boolean aiming, float durabilitySpreadMult, float wearPercent) {
        float spreadAmmo = ammoSpread * ammoSpreadMult;
        float spreadHipfire = aiming ? 0F : hipfirePenalty;
        float spreadWear = wearSpreadFraction(wearPercent) * durabilitySpreadMult;
        return innateSpread + spreadAmmo + spreadHipfire + spreadWear;
    }

    /** CE-shaped glue: pulls every input {@link #calcDamage(float, float)} needs out of the live stack/config/receiver state. */
    public static float calcDamage(LambdaContext ctx, ItemStack stack, Receiver primary, boolean calcWear, int index) {
        float wearPercent = calcWear ? (ItemGunBaseNT.getWear(stack, index) / ctx.config.getDurability(stack)) : 0F;
        return calcDamage(primary.getBaseDamage(stack), wearPercent);
    }

    /** CE-shaped glue: pulls every input {@link #calcSpread(float, float, float, float, boolean, float, float)} needs out of the live stack/config/receiver/ammo state. */
    public static float calcSpread(LambdaContext ctx, ItemStack stack, Receiver primary, BulletConfig config, boolean calcWear, int index, boolean aim) {
        float wearPercent = calcWear ? (ItemGunBaseNT.getWear(stack, index) / ctx.config.getDurability(stack)) : 0F;
        return calcSpread(primary.getInnateSpread(stack), config.spread, primary.getAmmoSpread(stack), primary.getHipfireSpread(stack), aim, primary.getDurabilitySpread(stack), wearPercent);
    }
}
