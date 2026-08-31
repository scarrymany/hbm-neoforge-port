package com.hbm.items.weapon.sedna.content;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import com.hbm.util.EntityDamageUtil;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryFolly} - {@code gun_folly}, the
 * SECRET-tier joke/prestige weapon: a charge-up ({@code SPINUP}, held aim + timer) beam that erases a
 * long tunnel of blocks and area-damages everything along it, plus a chunk-loading fake-nuke shell.
 * See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryFolly} table.
 * <p>
 * {@code folly_sm}'s beam-tunneling {@link #smUpdate} and {@code folly_nuke}'s
 * {@link #nukeImpact} are both real, fully-wired ports (this port's {@link ContaminationUtil},
 * {@link EntityNukeExplosionMK5}, {@link EntityNukeTorex} and {@link EntityDamageUtil} all exist and
 * match CE's call shapes 1:1). Only the client-only {@code AuxParticlePacketNT} "growing plasma
 * sphere" tracer effect along the beam's path is dropped (pure VFX, Phase 5 scope, zero gameplay
 * effect) - the actual block clearing, damage and radiation contamination all happen.
 */
public final class XFactoryFolly {

    private XFactoryFolly() {
    }

    // ==================== ammo ====================
    // Both rounds back CE's hidden EnumAmmoSecret (ammo_secret, no creative tab) - registered as real,
    // holdable items per this port's established secret-ammo convention (see GunPistolItems/
    // GunRifleItems' registerAmmoHidden precedent for M44_EQUESTRIAN/BMG50_BLACK etc).

    public static final Item ITEM_FOLLY_SM = new Item(new Item.Properties());
    public static final Item ITEM_FOLLY_NUKE = new Item(new Item.Properties());

    public static final BulletConfig folly_sm = new BulletConfig("folly_sm").setItem(ITEM_FOLLY_SM)
            .setupDamageClass(DamageClass.SUBATOMIC).setBeam().setLife(100).setVel(2F).setGrav(0.015).setRenderRotations(false)
            .setSpectral(true).setDoesPenetrate(true).setOnUpdate(XFactoryFolly::smUpdate);
    /** The only ammo in the whole roster using {@link BulletConfig.ProjectileType#BULLET_CHUNKLOADING} - matches the report's flag exactly. */
    public static final BulletConfig folly_nuke = new BulletConfig("folly_nuke").setItem(ITEM_FOLLY_NUKE)
            .setChunkloading().setLife(600).setVel(4F).setGrav(0.015).setOnImpact(XFactoryFolly::nukeImpact);

    // ==================== gun ====================

    public static ItemGunBaseNT gun_folly() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.SECRET,
                new GunConfig()
                        .dura(0).draw(40).crosshair(Crosshair.NONE)
                        .rec(new Receiver(0)
                                .dmg(1_000F).delay(26).dryfire(false).reload(160).jam(0).sound(HBMSoundHandler.loudestNoiseOnEarth.get(), 100.0F, 1.0F)
                                .mag(new MagazineSingleReload(0, 1).addConfigs(folly_sm, folly_nuke))
                                .offset(0.75, -0.0625, -0.1875D).offsetScoped(0.75, -0.0625, -0.125D)
                                .canFire(LAMBDA_CAN_FIRE).fire(LAMBDA_FIRE))
                        .setupStandardConfiguration().pt(LAMBDA_TOGGLE_AIM));
    }

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_TOGGLE_AIM = (stack, ctx) -> {
        if (ItemGunBaseNT.getState(stack, ctx.configIndex) == ItemGunBaseNT.GunState.IDLE) {
            boolean wasAiming = ItemGunBaseNT.getIsAiming(stack);
            ItemGunBaseNT.setIsAiming(stack, !wasAiming);
            if (!wasAiming) ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimationType.SPINUP, ctx.configIndex);
        }
    };

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_FIRE =
            (stack, ctx) -> com.hbm.items.weapon.sedna.factory.Lego.doStandardFire(stack, ctx, GunAnimationType.CYCLE, 0, false);

    /** Players must hold aim through a 100-tick spin-up before the charge round can fire at all; mobs (no player) fire immediately once loaded, matching CE's {@code instanceof EntityPlayer} branch. */
    private static final BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> LAMBDA_CAN_FIRE = (stack, ctx) -> {
        if (ctx.getPlayer() instanceof Player) {
            if (!ItemGunBaseNT.getIsAiming(stack)) return false;
            if (ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) != GunAnimationType.SPINUP.ordinal()) return false;
            if (ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex) < 100) return false;
        }
        return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0;
    };

    // ==================== lambdas ====================

    /**
     * Port of CE's {@code LAMBDA_SM_UPDATE} - on tick 2 (server-side only), contaminates the shooter
     * with 150 rads and carves a 3x3-block tunnel along the beam's path (up to its resolved
     * {@code beamLength}), area-damaging every entity along the way.
     */
    private static void smUpdate(Entity entity) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof EntityBulletBeamBase beam)) return;
        if (entity.tickCount != 2) return;

        Vec3 dir = new Vec3(beam.headingX, beam.headingY, beam.headingZ).normalize();

        if (beam.getThrower() instanceof LivingEntity shooter) {
            ContaminationUtil.contaminate(shooter, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, 150D);
        }

        Level level = beam.level();
        AABB region = beam.getBoundingBox().expandTowards(beam.headingX, beam.headingY, beam.headingZ).inflate(1.0D);
        var entities = level.getEntities(beam, region, e -> true);

        for (int i = 1; i < beam.beamLength; i += 2) {
            int x = (int) Math.floor(beam.getX() + dir.x * i);
            int y = (int) Math.floor(beam.getY() + dir.y * i);
            int z = (int) Math.floor(beam.getZ() + dir.z * i);

            for (int ix = x - 1; ix <= x + 1; ix++) for (int iy = y - 1; iy <= y + 1; iy++) for (int iz = z - 1; iz <= z + 1; iz++) {
                BlockPos pos = new BlockPos(ix, iy, iz);
                if (iy > level.getMinBuildHeight() && iy < level.getMaxBuildHeight()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

                AABB aabb = new AABB(ix - 1, iy - 1, iz - 1, ix + 2, iy + 2, iz + 2);
                for (Entity e : entities) {
                    if (e == beam.getThrower() || !e.getBoundingBox().intersects(aabb)) continue;
                    DamageSource source = BulletConfig.getDamage(beam, beam.getThrower(), beam.config.dmgClass);
                    if (e instanceof LivingEntity living) {
                        EntityDamageUtil.attackEntityFromNT(living, source, beam.damage, true, false, 0D, 100F, 0.99F);
                    } else {
                        EntityDamageUtil.attackEntityFromIgnoreIFrame(e, source, beam.damage);
                    }
                }
            }
        }
    }

    /** Port of CE's {@code LAMBDA_NUKE_IMPACT} - a full nuclear detonation plus mushroom-cloud spawn on impact. */
    private static void nukeImpact(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (hit instanceof net.minecraft.world.phys.EntityHitResult ehr && bullet.tickCount < 2 && ehr.getEntity() == bullet.getThrower()) return;
        bullet.discard();
        Vec3 loc = hit.getLocation();
        bullet.level().addFreshEntity(EntityNukeExplosionMK5.statFac(bullet.level(), 100, loc.x, loc.y, loc.z));
        EntityNukeTorex.statFac(bullet.level(), loc.x, loc.y, loc.z, 100F);
    }
}
