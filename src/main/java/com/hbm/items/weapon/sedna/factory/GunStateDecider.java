package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.GunStateDecider} (156 lines) - the state-
 * machine engine. {@link #LAMBDA_STANDARD_DECIDER} composes 4 sub-transitions every gun surveyed by
 * {@code docs/phase3/gun_framework.md} uses unmodified; read that report's Package B table before
 * editing this class.
 * <p>
 * <b>Jamming is a reload-time risk, not a firing-time risk</b> - {@link #getStandardJamChance} is
 * evaluated once per reload completion inside {@link #deciderStandardReload}, never per shot: a worn
 * gun does not jam mid-burst, it jams when you finish reloading it. Preserved exactly, per this
 * task's explicit instruction.
 */
public class GunStateDecider {

    /**
     * The meat and bones of the gun system's state machine. Handles guns with an automatic primary
     * receiver, as well as one receiver's reloading state. Supports draw delays as well as semi and
     * auto fire.
     */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_STANDARD_DECIDER = (stack, ctx) -> {
        int index = ctx.configIndex;
        ItemGunBaseNT.GunState lastState = ItemGunBaseNT.getState(stack, index);
        deciderStandardFinishDraw(stack, lastState, index);
        deciderStandardClearJam(stack, lastState, index);
        deciderStandardReload(stack, ctx, lastState, 0, index);
        deciderAutoRefire(stack, ctx, lastState, 0, index,
                () -> ItemGunBaseNT.getPrimary(stack, index) && ItemGunBaseNT.getMode(stack, ctx.configIndex) == 0);
    };

    /** Transitions the gun from DRAWING to IDLE. */
    public static void deciderStandardFinishDraw(ItemStack stack, ItemGunBaseNT.GunState lastState, int index) {
        if (lastState == ItemGunBaseNT.GunState.DRAWING) {
            ItemGunBaseNT.setState(stack, index, ItemGunBaseNT.GunState.IDLE);
            ItemGunBaseNT.setTimer(stack, index, 0);
        }
    }

    /** Transitions the gun from JAMMED to IDLE - jams self-clear once their duration elapses, no player action needed. */
    public static void deciderStandardClearJam(ItemStack stack, ItemGunBaseNT.GunState lastState, int index) {
        if (lastState == ItemGunBaseNT.GunState.JAMMED) {
            ItemGunBaseNT.setState(stack, index, ItemGunBaseNT.GunState.IDLE);
            ItemGunBaseNT.setTimer(stack, index, 0);
        }
    }

    /**
     * Triggers a reload action on the first receiver. If the mag is not full and reloading is still
     * possible, sets RELOADING again (this is CE's implicit tube-magazine support: a
     * {@code MagazineSingleReload}/{@code MagazineBelt} mag simply keeps reporting {@code canReload()
     * == true} across multiple cycles, so the same code loops without a type check), otherwise IDLE
     * (or JAMMED, per the wear-driven jam roll).
     */
    public static void deciderStandardReload(ItemStack stack, ItemGunBaseNT.LambdaContext ctx, ItemGunBaseNT.GunState lastState, int recIndex, int gunIndex) {

        if (lastState == ItemGunBaseNT.GunState.RELOADING) {

            LivingEntity entity = ctx.entity;
            Player player = ctx.getPlayer();
            GunConfig cfg = ctx.config;
            Receiver rec = cfg.getReceivers(stack)[recIndex];
            IMagazine<?> mag = rec.getMagazine(stack);

            mag.reloadAction(stack, ctx.inventory);
            boolean cancel = ItemGunBaseNT.getReloadCancel(stack);

            if (!cancel && mag.canReload(stack, ctx.inventory)) {
                ItemGunBaseNT.setState(stack, gunIndex, ItemGunBaseNT.GunState.RELOADING);
                ItemGunBaseNT.setTimer(stack, gunIndex, rec.getReloadCycleDuration(stack));
                ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.RELOAD_CYCLE, gunIndex);
            } else {

                if (getStandardJamChance(stack, cfg, gunIndex) > entity.getRandom().nextFloat()) {
                    ItemGunBaseNT.setState(stack, gunIndex, ItemGunBaseNT.GunState.JAMMED);
                    ItemGunBaseNT.setTimer(stack, gunIndex, rec.getJamDuration(stack));
                    ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.JAMMED, gunIndex);
                } else {
                    ItemGunBaseNT.setState(stack, gunIndex, ItemGunBaseNT.GunState.DRAWING);
                    int duration = rec.getReloadEndDuration(stack) + (mag.getAmountBeforeReload(stack) <= 0 ? rec.getReloadCockOnEmptyPost(stack) : 0);
                    ItemGunBaseNT.setTimer(stack, gunIndex, duration);
                    ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.RELOAD_END, gunIndex);
                }

                ItemGunBaseNT.setReloadCancel(stack, false);
            }

            mag.setAmountAfterReload(stack, mag.getAmount(stack, ctx.inventory));
        }
    }

    /** Evaluated once per reload completion, not per shot - see class javadoc. 0 below 66% wear, ramping 0-1 from 66% to 91% wear (capped at 100% jam chance). */
    public static float getStandardJamChance(ItemStack stack, GunConfig config, int index) {
        float percent = ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack);
        if (percent < 0.66F) return 0F;
        return Math.min((percent - 0.66F) * 4F, 1F);
    }

    /** Triggers a re-fire of the primary if the fire delay has expired, the trigger condition holds and re-firing is enabled, otherwise switches to IDLE (or reloads on empty, for non-refiring guns). */
    public static void deciderAutoRefire(ItemStack stack, ItemGunBaseNT.LambdaContext ctx, ItemGunBaseNT.GunState lastState, int recIndex, int gunIndex, BooleanSupplier refireCondition) {

        if (lastState == ItemGunBaseNT.GunState.COOLDOWN) {

            LivingEntity entity = ctx.entity;
            Player player = ctx.getPlayer();
            GunConfig cfg = ctx.config;
            Receiver rec = cfg.getReceivers(stack)[recIndex];

            if (rec.getRefireOnHold(stack) && refireCondition.getAsBoolean()) {

                if (rec.getCanFire(stack).apply(stack, ctx)) {
                    rec.getOnFire(stack).accept(stack, ctx);
                    ItemGunBaseNT.setState(stack, gunIndex, ItemGunBaseNT.GunState.COOLDOWN);
                    ItemGunBaseNT.setTimer(stack, gunIndex, rec.getDelayAfterFire(stack));

                    if (rec.getFireSound(stack) != null) {
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), rec.getFireSound(stack), SoundSource.PLAYERS, rec.getFireVolume(stack), rec.getFirePitch(stack));
                    }

                    int remaining = rec.getRoundsPerCycle(stack) - 1;
                    for (int i = 0; i < remaining; i++) {
                        if (rec.getCanFire(stack).apply(stack, ctx)) rec.getOnFire(stack).accept(stack, ctx);
                    }
                } else if (rec.getDoesDryFireAfterAuto(stack)) {
                    ItemGunBaseNT.setState(stack, gunIndex, rec.getRefireAfterDry(stack) ? ItemGunBaseNT.GunState.COOLDOWN : ItemGunBaseNT.GunState.DRAWING);
                    ItemGunBaseNT.setTimer(stack, gunIndex, rec.getDelayAfterDryFire(stack));
                    ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.CYCLE_DRY, gunIndex);
                } else {
                    ItemGunBaseNT.setState(stack, gunIndex, ItemGunBaseNT.GunState.IDLE);
                    ItemGunBaseNT.setTimer(stack, gunIndex, 0);
                }
            } else {

                if (rec.getReloadOnEmpty(stack) && rec.getMagazine(stack).getAmount(stack, ctx.inventory) <= 0) {
                    ItemGunBaseNT.setIsAiming(stack, false);
                    IMagazine<?> mag = rec.getMagazine(stack);

                    if (mag.canReload(stack, ctx.inventory)) {
                        int loaded = mag.getAmount(stack, ctx.inventory);
                        mag.setAmountBeforeReload(stack, loaded);
                        ItemGunBaseNT.setState(stack, ctx.configIndex, ItemGunBaseNT.GunState.RELOADING);
                        ItemGunBaseNT.setTimer(stack, ctx.configIndex, rec.getReloadBeginDuration(stack) + (loaded <= 0 ? rec.getReloadCockOnEmptyPre(stack) : 0));
                        ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.RELOAD, ctx.configIndex);
                    } else {
                        ItemGunBaseNT.setState(stack, gunIndex, ItemGunBaseNT.GunState.IDLE);
                        ItemGunBaseNT.setTimer(stack, gunIndex, 0);
                    }

                } else {
                    ItemGunBaseNT.setState(stack, gunIndex, ItemGunBaseNT.GunState.IDLE);
                    ItemGunBaseNT.setTimer(stack, gunIndex, 0);
                }
            }
        }
    }
}
