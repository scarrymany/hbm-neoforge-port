package com.hbm.items.weapon.sedna.content;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryTool} - the fire extinguisher and
 * grapple/mortar charge-thrower. See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryTool}
 * table.
 * <p>
 * <b>Forward references (documented, not silently dropped):</b>
 * <ul>
 *     <li>{@code fext_water}/{@code _foam}/{@code _sand}'s terrain-modifying block-hit branches
 *     (clearing fire/foam, laying down {@code foam_layer}/{@code sand_boron_layer}, and the
 *     {@code CompatExternal.getCoreFromPos}/{@code IRepairable} machine-fire-suppression hook) are
 *     dropped: none of {@code ModBlocks.foam_layer}/{@code block_foam}/{@code sand_boron}/
 *     {@code sand_boron_layer}, {@code CompatExternal}, or {@code IRepairable} exist anywhere in this
 *     port (confirmed by grep against {@code ModBlocks.java}) - Phase 2 fire-suppression content, not
 *     this ammo package's scope. The direct "clear fire off the entity hit" behavior <b>is</b> ported
 *     (real vanilla {@link LivingEntity#clearFire()}), and every round still discards itself on block
 *     impact, matching CE's own unconditional {@code bullet.setDead()}.</li>
 *     <li>{@code ct_mortar_charge}'s {@code BlockMutatorDebris(ModBlocks.block_slag, 1)} block-litter
 *     effect is dropped - {@code block_slag} is not a registered block anywhere in this port yet
 *     (confirmed by grep); the blast itself (crater, damage, knockback) is unaffected.</li>
 *     <li>{@code ItemGunChargeThrower.setLastHook}/the reel-in mechanic (a per-stack "which hook
 *     entity is mine" NBT link plus grapple pull) is not ported - the hook still embeds itself in the
 *     world via the real, already-ported {@code EntityThrowableNT#getStuck(BlockPos, int)} (confirmed
 *     present on {@link EntityBulletBaseMK4}'s own ancestor - {@code EnumGrenadeExtra} already calls
 *     the same method on a grenade entity elsewhere in this tree); only the reel-in follow-up itself
 *     is a documented forward reference.</li>
 * </ul>
 */
public final class XFactoryTool {

    private XFactoryTool() {
    }

    // ==================== fire extinguisher ammo ====================
    // CE backs all 3 with one ammo_fireext ItemEnumMulti (3 metadata variants) - flattened per this
    // port's metadata-flattening convention into 3 distinct items, matching every other family.

    public static final Item ITEM_FEXT_WATER = new Item(new Item.Properties());
    public static final Item ITEM_FEXT_FOAM = new Item(new Item.Properties());
    public static final Item ITEM_FEXT_SAND = new Item(new Item.Properties());

    public static final BulletConfig fext_water = new BulletConfig("fext_water").setItem(ITEM_FEXT_WATER)
            .setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04).setSpread(0.025F)
            .setOnEntityHit(XFactoryTool::extinguishHit).setOnRicochet((b, bhr) -> b.discard());
    public static final BulletConfig fext_foam = new BulletConfig("fext_foam").setItem(ITEM_FEXT_FOAM)
            .setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04).setSpread(0.05F)
            .setOnEntityHit(XFactoryTool::extinguishHit).setOnRicochet((b, bhr) -> b.discard());
    public static final BulletConfig fext_sand = new BulletConfig("fext_sand").setItem(ITEM_FEXT_SAND)
            .setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04).setSpread(0.05F)
            .setOnEntityHit(XFactoryTool::extinguishHit).setOnRicochet((b, bhr) -> b.discard());

    // ==================== charge-thrower ammo ====================

    public static final Item ITEM_CT_HOOK = new Item(new Item.Properties());
    public static final Item ITEM_CT_MORTAR = new Item(new Item.Properties());
    public static final Item ITEM_CT_MORTAR_CHARGE = new Item(new Item.Properties());

    public static final BulletConfig ct_hook = new BulletConfig("ct_hook").setItem(ITEM_CT_HOOK)
            .setRenderRotations(false).setLife(6_000).setVel(3F).setGrav(0.035).setDoesPenetrate(true).setDamageFalloffByPen(false)
            .setOnImpact(XFactoryTool::hookImpact);
    public static final BulletConfig ct_mortar = new BulletConfig("ct_mortar").setItem(ITEM_CT_MORTAR)
            .setDamage(2.5F).setLife(200).setVel(3F).setGrav(0.035).setOnImpact(XFactoryTool::mortarImpact);
    public static final BulletConfig ct_mortar_charge = new BulletConfig("ct_mortar_charge").setItem(ITEM_CT_MORTAR_CHARGE)
            .setDamage(5F).setLife(200).setVel(3F).setGrav(0.035).setOnImpact(XFactoryTool::mortarChargeImpact);

    // ==================== guns ====================

    public static ItemGunBaseNT gun_fireext() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.UTILITY,
                new GunConfig()
                        .dura(5_000).draw(10).inspect(55).reloadChangeType(true).hideCrosshair(false).crosshair(Crosshair.L_CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(0F).delay(1).dry(0).auto(true).spread(0F).spreadHipfire(0F).reload(20).jam(0).sound(HBMSoundHandler.fireExtinguisher.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 300).addConfigs(fext_water, fext_foam, fext_sand))
                                .offset(1, -0.15625, -0.25D)
                                .setupStandardFire())
                        .setupStandardConfiguration());
    }

    public static ItemGunBaseNT gun_charge_thrower() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.UTILITY,
                new GunConfig()
                        .dura(3_000).draw(10).inspect(55).reloadChangeType(true).hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
                        .rec(new Receiver(0)
                                .dmg(10F).delay(4).dry(10).auto(true).spread(0F).spreadHipfire(0F).reload(60).jam(0).sound(HBMSoundHandler.fireGrenade.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 1).addConfigs(ct_hook, ct_mortar, ct_mortar_charge))
                                .offset(1, -0.15625, -0.25D)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): CT_MORTAR x3
    }

    // ==================== lambdas ====================

    private static void extinguishHit(EntityBulletBaseMK4 bullet, EntityHitResult hit) {
        if (hit.getEntity() instanceof LivingEntity living) living.clearFire();
    }

    private static void hookImpact(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (!(hit instanceof BlockHitResult bhr)) return;
        Vec3 back = bullet.getDeltaMovement().scale(-1).normalize().scale(0.05);
        bullet.setPos(bhr.getLocation().add(back));
        bullet.getStuck(bhr.getBlockPos(), bhr.getDirection().ordinal());
    }

    private static void mortarImpact(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (hit instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.getThrower()) return;
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, 5, bullet.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorBulkie(60, 8));
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage).setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
        bullet.discard();
    }

    private static void mortarChargeImpact(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (hit instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.getThrower()) return;
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, 15, bullet.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage).setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        bullet.discard();
        // TODO(blocks-generic): CE's BlockMutatorDebris(ModBlocks.block_slag, 1) litter effect and the
        // client-only ExplosionCreator.composeEffectSmall VFX are dropped - see class javadoc.
    }
}
