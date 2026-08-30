package com.hbm.items.weapon.legacy;

import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.items.special.weapon.GunB92} (misfiled out of {@code items/weapon}
 * even in CE itself, same {@code DIGEST_REMAINDER.md}-flagged pattern Phase 1 already noted for other
 * misfiled classes) - a legendary bow-style hold-to-charge weapon: holding right-click accumulates up
 * to 10 charges (1 every 15 ticks of a 30-tick animation cycle); releasing while not sneaking fires
 * one beam per accumulated charge with increasing divergence and resets to 0; reaching an 11th charge
 * instead self-detonates the wielder. See {@link LegacyChargeWeapons}'s class javadoc for why the
 * fired projectile is a directly-constructed {@link com.hbm.entity.projectile.EntityBulletBaseMK4}
 * rather than a new {@code EntityExplosiveBeam} class.
 * <p>
 * <b>Simplified relative to CE</b> (documented, not silently dropped): CE's sneak-while-charged branch
 * (right-click while sneaking with {@code power > 0} does nothing special beyond starting the same
 * use-item hold) collapses cleanly since this port always starts the hold on right-click regardless of
 * sneak state - the sneak check that matters (whether <i>releasing</i> should fire or not) is preserved
 * exactly. The {@code ArrowLooseEvent} charge-adjustment hook (a Forge event with no direct 1.21.1
 * analogue researched for this package) is dropped - the full held duration always counts.
 */
public class ItemGunB92 extends Item {

    public ItemGunB92(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide()) return;

        int anim = getAnimation(stack) + 1;
        if (anim >= 30) anim = 0;
        setAnimation(stack, anim);

        if (anim == 15) {
            int energy = getEnergy(stack) + 1;
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), com.hbm.lib.HBMSoundHandler.b92Reload.get(), SoundSource.AMBIENT, 2.0F, 0.9F);

            if (energy > 10) {
                setEnergy(stack, 0);
                selfDetonate(level, entity);
            } else {
                setEnergy(stack, energy);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return stack;
    }

    /** CE {@code onPlayerStoppedUsing} - fires one beam per accumulated charge (unless sneaking) and resets the charge counter. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || entity.isShiftKeyDown()) return;

        int power = getEnergy(stack);
        if (power <= 0) return;

        for (int i = 0; i < power; i++) {
            float divergence = Math.min(i * 0.2F, 1F);
            LegacyChargeWeapons.fireBeam(entity, LegacyChargeWeapons.b92_beam, 16F + level.random.nextInt(13), divergence);
            stack.hurtAndBreak(1, entity, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
        }

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.AMBIENT, 5.0F, 1.0F);
        setEnergy(stack, 0);
        setAnimation(stack, 1);
    }

    private static void selfDetonate(Level level, LivingEntity entity) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 100.0F, level.random.nextFloat() * 0.1F + 0.9F);

        var nuke = EntityNukeExplosionMK3.statFacFleija(level, entity.getX(), entity.getY(), entity.getZ(), 50);
        level.addFreshEntity(nuke);

        EntityCloudFleija cloud = EntityCloudFleija.create(level, entity.getX(), entity.getY(), entity.getZ(), 100);
        level.addFreshEntity(cloud);
    }

    public static int getAnimation(ItemStack stack) {
        return stack.getOrDefault(LegacyWeaponDataComponents.ANIMATION.get(), 0);
    }

    public static void setAnimation(ItemStack stack, int value) {
        stack.set(LegacyWeaponDataComponents.ANIMATION.get(), value);
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(LegacyWeaponDataComponents.ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int value) {
        stack.set(LegacyWeaponDataComponents.ENERGY.get(), value);
    }
}
