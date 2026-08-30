package com.hbm.items.weapon.legacy;

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
 * Port of CE's {@code com.hbm.items.weapon.GunB93} - the same hold-to-charge state machine as
 * {@link ItemGunB92} (see that class's javadoc for the shared simplifications), except release fires
 * exactly <b>one</b> round whose power scales with the accumulated charge level ({@code mode = power
 * - 1}) rather than one round per charge. No self-detonation branch on CE's own {@code GunB93} (only
 * {@code GunB92} carries that mechanic).
 */
public class ItemGunB93 extends Item {

    public ItemGunB93(Properties properties) {
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

        int anim = ItemGunB92.getAnimation(stack) + 1;
        if (anim >= 30) anim = 0;
        ItemGunB92.setAnimation(stack, anim);

        if (anim == 15) {
            int energy = Math.min(ItemGunB92.getEnergy(stack) + 1, 10);
            ItemGunB92.setEnergy(stack, energy);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || entity.isShiftKeyDown()) return;

        int power = ItemGunB92.getEnergy(stack);
        if (power <= 0) return;

        int mode = power - 1;
        LegacyChargeWeapons.fireBeam(entity, LegacyChargeWeapons.b93_beam, 16F + mode * 2F, 0F);
        stack.hurtAndBreak(1, entity, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.AMBIENT, 5.0F, 1.0F);
        ItemGunB92.setEnergy(stack, 0);
        ItemGunB92.setAnimation(stack, 1);
    }
}
