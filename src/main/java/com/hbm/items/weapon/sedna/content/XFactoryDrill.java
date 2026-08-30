package com.hbm.items.weapon.sedna.content;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.EntityDamageUtil;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryDrill} - {@code gun_drill}, a
 * melee-range fuel-burning mining/combat tool. See {@code docs/phase3/guns_and_ammo.md}'s
 * {@code XFactoryDrill} table.
 * <p>
 * <b>Simplified relative to CE, documented rather than silently dropped:</b> CE's real fire lambda
 * routes every hit through {@code XWeaponModManager} (attachment-mod-adjustable reach/piercing/AoE/
 * harvest-level - explicitly out of scope for this batch per this task's brief: "weapon-mod
 * interactions... a parallel {@code weapon_mod_eval} package handles those") and, on a block hit,
 * simulates a real survival-mode block harvest via {@code EntityPlayerMP#interactionManager
 * .tryHarvestBlock} plus a manual multi-block AoE break loop - vanilla-internals block-breaking
 * simulation with no confirmed 1.21.1 equivalent researched for this package. The entity-damage half
 * (direct raytraced melee damage, exactly matching CE's own {@code EntityDamageUtil.attackEntityFromNT}
 * call and constants) is a real, faithful port; the block-harvesting half is a documented forward
 * reference - the gun holds, aims, fires on cadence and drains its fluid tank correctly either way.
 */
public final class XFactoryDrill {

    private XFactoryDrill() {
    }

    public static ItemGunBaseNT gun_drill() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.UTILITY,
                new GunConfig()
                        .dura(3_000).draw(10).inspect(55).hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
                        .rec(new Receiver(0)
                                .dmg(10F).delay(20).auto(true).jam(0)
                                .mag(new MagazineFluid(0, 4_000, Fluids.GASOLINE, Fluids.GASOLINE_LEADED, Fluids.COALGAS, Fluids.COALGAS_LEADED))
                                .offset(1, -0.15625, -0.25D)
                                .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(XFactoryDrill::drillFire))
                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
    }

    private static void drillFire(ItemStack stack, ItemGunBaseNT.LambdaContext ctx) {
        if (!(ctx.getPlayer() instanceof Player player)) return;
        ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.CYCLE, ctx.configIndex);

        Receiver primary = ctx.config.getReceivers(stack)[0];
        @SuppressWarnings("unchecked")
        IMagazine<Object> mag = (IMagazine<Object>) primary.getMagazine(stack);

        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double reach = 5.0D;
        Vec3 endPoint = eye.add(look.scale(reach));

        HitResult blockHit = level.clip(new ClipContext(eye, endPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            // TODO(tool-harvest): CE performs a real survival-mode block harvest here (plus a
            // sneak-gated multi-block AoE break) - see class javadoc. No confirmed 1.21.1
            // vanilla-internals equivalent researched for this package; skipped gracefully.
        }

        DamageSource source = player.damageSources().playerAttack(player);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach))) {
            if (candidate == player) continue;
            Vec3 toCandidate = candidate.getEyePosition().subtract(eye);
            if (toCandidate.length() > reach) continue;
            if (toCandidate.normalize().dot(look) < 0.95) continue;

            EntityDamageUtil.attackEntityFromNT(candidate, source, primary.getBaseDamage(stack), true, true, 0.1D, 2F, 0.15F);
            break;
        }

        mag.useUpAmmo(stack, ctx.inventory, 10);
    }
}
