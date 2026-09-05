package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.content.XFactoryPA;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.EntityDamageUtil;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Exact CE {@code ArmorNCRPAMelee} click + orchestra ({@code ArmorNCRPAMelee.java:25-66}).
 * {@code ConfettiUtil.gib} / first-person arm buses stay skipped.
 */
public class ArmorNCRPAMelee implements IPAMelee {

    @Override
    public void clickPrimary(ItemStack stack, LambdaContext ctx) {
        XFactoryPA.doSwing(stack, ctx, GunAnimationType.CYCLE, 25);
    }

    @Override
    public void clickSecondary(ItemStack stack, LambdaContext ctx) {
        XFactoryPA.doSwing(stack, ctx, GunAnimationType.ALT_CYCLE, 30);
    }

    @Override
    public void orchestra(ItemStack stack, LambdaContext ctx) {
        LivingEntity entity = ctx.entity;
        if (entity.level().isClientSide()) return;
        int type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
        int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

        boolean swings = type == GunAnimationType.CYCLE.ordinal() && (timer == 5 || timer == 15);
        boolean sweep = type == GunAnimationType.ALT_CYCLE.ordinal() && timer == 5;

        if ((swings || sweep) && ctx.getPlayer() != null) {
            HitResult mop = EntityDamageUtil.getMouseOver(ctx.getPlayer(), 3.0D, 0.5D);

            if (mop != null && mop.getType() != HitResult.Type.MISS) {
                if (mop.getType() == HitResult.Type.ENTITY && mop instanceof EntityHitResult ehr && ehr.getEntity().isAlive()) {
                    float damage = swings ? 15F : 35F;
                    float knockback = swings ? 0F : 1.5F;
                    float dt = swings ? 5F : 15F;
                    float pierce = swings ? 0.1F : 0.25F;
                    Entity hit = ehr.getEntity();
                    DamageSource source = ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer());

                    if (hit instanceof LivingEntity living) {
                        if (living.getMaxHealth() >= 100) damage *= 2.5F;
                        EntityDamageUtil.attackEntityFromNT(living, source, damage, true, false, knockback, dt, pierce);
                        // TODO(CE:ArmorNCRPAMelee.java:51): ConfettiUtil.gib — VFX not ported.
                    } else {
                        hit.hurt(source, damage);
                    }

                    entity.level().playSound(null, hit.blockPosition(), HBMSoundHandler.fireStab.get(),
                            SoundSource.PLAYERS, 1F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
                }
                if (mop.getType() == HitResult.Type.BLOCK && mop instanceof BlockHitResult bhr) {
                    BlockPos pos = bhr.getBlockPos();
                    BlockState state = entity.level().getBlockState(pos);
                    entity.level().playSound(null, mop.getLocation().x, mop.getLocation().y, mop.getLocation().z,
                            state.getSoundType(entity.level(), pos, entity).getStepSound(),
                            SoundSource.BLOCKS, 2F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
                }
            }
        }
    }
}
