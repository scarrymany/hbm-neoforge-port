package com.hbm.items.weapon.sedna.content;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.capability.ModAttachments;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import com.hbm.util.EntityDamageUtil;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryFlamer} - the 3 flamethrower tiers
 * (cased-ammo, {@link MagazineFullReload}-backed) plus {@code gun_chemthrower} (raw-fluid-tank,
 * {@link MagazineFluid}-backed). See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryFlamer}
 * table.
 * <p>
 * Diesel/gas/napalm/balefire ricochet linger is Exact CE {@code :77-80}. Gas {@code :78} is
 * {@code igniteIfPossible} only — CE has no gas puddle. Daybreaker {@code onImpact} explode
 * Exact CE {@code :130-137} via local {@code Lego.standardExplode} copy. Direct ignite on
 * living hits is {@link HbmLivingAttachment}. {@code flame_nograv} Exact CE {@code :123}.
 * FlameCreator trail VFX skipped.
 * <p>
 * <b>{@code gun_chemthrower} is a real but simplified port, not the bespoke {@code ItemGunChemthrower}
 * subclass</b>: CE's real class evaluates the tank's currently-loaded chemical against a large
 * per-chemical effect table (acid/corrosive/toxic/etc, one bespoke {@code IEffect} per fluid) that
 * has no equivalent anywhere in this port yet (no {@code FluidType} chemical-effect trait is wired to
 * combat). This registers a plain {@link ItemGunBaseNT} whose fire lambda faithfully reproduces the
 * mechanical shell CE's own class provides regardless of loaded chemical - short-range raytrace,
 * fluid consumption gated on {@code amount > 0}, direct damage plus ignition on a living hit - so the
 * gun holds, aims, drains its tank and deals damage exactly on CE's cadence; only the "which chemical
 * does what" table is deferred to whichever future package wires real chemical-warfare effects.
 * <p>
 * <b>c12-sound-wiring:</b> confirmed against CE's real {@code XFactoryFlamer.init()} that
 * {@code gun_flamer}/{@code gun_flamer_topaz}/{@code gun_chemthrower} genuinely have no
 * {@code Receiver#sound(...)} call in CE either (they fire silently but for their reload/orchestra
 * cues, which this port doesn't reproduce - see {@code ItemGunBaseNT}'s javadoc) - not a port
 * omission. Only {@code gun_flamer_daybreaker} calls {@code .sound(HBMSoundHandler.fireBlackPowder,
 * 1.0F, 1.0F)} in CE; this port's copy was missing that one call (the only real gap in this file)
 * until this pass added it back.
 */
public final class XFactoryFlamer {

    private XFactoryFlamer() {
    }

    // ==================== ammo ====================

    public static Item ITEM_FLAME_DIESEL;
    public static Item ITEM_FLAME_GAS;
    public static Item ITEM_FLAME_NAPALM;
    public static Item ITEM_FLAME_BALEFIRE;

    public static final BulletConfig flame_diesel = new BulletConfig("flame_diesel").setItem(() -> ITEM_FLAME_DIESEL)
            .setupDamageClass(DamageClass.FIRE).setLife(100).setVel(1F).setGrav(0.02).setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
            .setOnImpact(XFactoryFlamer::igniteFire)
            .setOnRicochet(XFactoryFlamer::lingerDiesel);
    /** Exact CE {@code XFactoryFlamer.java:123}: {@code flame_diesel.clone().setGrav(0)}. Fritz fire. */
    public static final BulletConfig flame_nograv = flame_diesel.clone("flame_nograv").setGrav(0);
    public static final BulletConfig flame_gas = new BulletConfig("flame_gas").setItem(() -> ITEM_FLAME_GAS)
            .setupDamageClass(DamageClass.FIRE).setLife(10).setSpread(0.05F).setVel(1F).setGrav(0).setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
            .setOnImpact(XFactoryFlamer::igniteFire)
            .setOnRicochet(XFactoryFlamer::lingerGas);
    public static final BulletConfig flame_napalm = new BulletConfig("flame_napalm").setItem(() -> ITEM_FLAME_NAPALM)
            .setupDamageClass(DamageClass.FIRE).setLife(200).setVel(1F).setGrav(0.02).setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
            .setOnImpact(XFactoryFlamer::igniteFire)
            .setOnRicochet(XFactoryFlamer::lingerNapalm);
    public static final BulletConfig flame_balefire = new BulletConfig("flame_balefire").setItem(() -> ITEM_FLAME_BALEFIRE)
            .setupDamageClass(DamageClass.FIRE).setLife(200).setVel(1F).setGrav(0.02).setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
            .setOnImpact(XFactoryFlamer::igniteBalefire)
            .setOnRicochet(XFactoryFlamer::lingerBalefire);

    public static final BulletConfig flame_topaz_diesel = flame_diesel.clone("flame_topaz_diesel").setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0);
    public static final BulletConfig flame_topaz_gas = flame_gas.clone("flame_topaz_gas").setProjectiles(2).setSpread(0.05F);
    public static final BulletConfig flame_topaz_napalm = flame_napalm.clone("flame_topaz_napalm").setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0);
    public static final BulletConfig flame_topaz_balefire = flame_balefire.clone("flame_topaz_balefire").setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0);

    /** Exact CE {@code XFactoryFlamer.java:130-137}. */
    public static final BulletConfig flame_daybreaker_diesel = flame_diesel.clone("flame_daybreaker_diesel").setLife(200).setVel(2F).setGrav(0.035)
            .setOnImpact(XFactoryFlamer::daybreakerDiesel);
    public static final BulletConfig flame_daybreaker_gas = flame_gas.clone("flame_daybreaker_gas").setLife(200).setVel(2F).setGrav(0.035)
            .setOnImpact(XFactoryFlamer::daybreakerGas);
    public static final BulletConfig flame_daybreaker_napalm = flame_napalm.clone("flame_daybreaker_napalm").setLife(200).setVel(2F).setGrav(0.035)
            .setOnImpact(XFactoryFlamer::daybreakerNapalm);
    public static final BulletConfig flame_daybreaker_balefire = flame_balefire.clone("flame_daybreaker_balefire").setLife(200).setVel(2F).setGrav(0.035)
            .setOnImpact(XFactoryFlamer::daybreakerBalefire);

    // ==================== guns ====================

    public static ItemGunBaseNT gun_flamer() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(1F).spreadHipfire(0F).delay(1).auto(true).reload(90).jam(17)
                                .mag(new MagazineFullReload(0, 300).addConfigs(flame_diesel, flame_gas, flame_napalm, flame_balefire))
                                .offset(0.75, -0.0625, -0.25D)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): FLAME_DIESEL x1
    }

    public static ItemGunBaseNT gun_flamer_topaz() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
                new GunConfig()
                        .dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(1.5F).spreadHipfire(0F).delay(1).auto(true).reload(90).jam(17)
                                .mag(new MagazineFullReload(0, 500).addConfigs(flame_topaz_diesel, flame_topaz_gas, flame_topaz_napalm, flame_topaz_balefire))
                                .offset(0.75, -0.0625, -0.25D)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): FLAME_DIESEL x1
    }

    public static ItemGunBaseNT gun_flamer_daybreaker() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
                new GunConfig()
                        .dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(25F).spreadHipfire(0F).delay(10).auto(true).reload(90).jam(17).sound(HBMSoundHandler.fireBlackPowder.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 50).addConfigs(flame_daybreaker_diesel, flame_daybreaker_gas, flame_daybreaker_napalm, flame_daybreaker_balefire))
                                .offset(0.75, -0.0625, -0.25D)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): FLAME_DIESEL x1
    }

    public static ItemGunBaseNT gun_chemthrower() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(90_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
                        .rec(new Receiver(0)
                                .delay(1).spreadHipfire(0F).auto(true)
                                .mag(new MagazineFluid(0, 3_000))
                                .offset(0.75, -0.0625, -0.25D)
                                .canFire(LAMBDA_CHEM_CAN_FIRE).fire(LAMBDA_CHEM_FIRE))
                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
    }

    // ==================== chemthrower lambdas (see class javadoc for the simplification) ====================

    private static final BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> LAMBDA_CHEM_CAN_FIRE =
            (stack, ctx) -> ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0;

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_CHEM_FIRE = (stack, ctx) -> {
        if (!(ctx.getPlayer() instanceof Player player)) return;
        ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.CYCLE, ctx.configIndex);

        Receiver primary = ctx.config.getReceivers(stack)[0];
        @SuppressWarnings("unchecked")
        IMagazine<FluidType> mag = (IMagazine<FluidType>) primary.getMagazine(stack);

        LivingEntity living = findNearestTarget(player, 6.0D);
        if (living != null) {
            var source = BulletConfig.getDamage(player, player, DamageClass.FIRE);
            EntityDamageUtil.attackEntityFromNT(living, source, primary.getBaseDamage(stack), true, true, 0.1D, 0F, 0F);
            HbmLivingAttachment props = HbmLivingAttachment.getData(living);
            if (props.getFire() < 100) {
                props.setFire(100);
                living.setData(ModAttachments.LIVING_ATTACHMENT, props);
            }
        }

        mag.useUpAmmo(stack, ctx.inventory, 10);
    };

    /** Nearest living entity along the player's look vector within {@code reach} blocks, blocked by terrain - built on {@link Level#clip}, the same confirmed-real raytrace primitive {@link com.hbm.entity.projectile.EntityBulletBeamBase#performHitscan} uses. */
    private static LivingEntity findNearestTarget(Player player, double reach) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 endPoint = eye.add(look.scale(reach));

        HitResult blockHit = level.clip(new ClipContext(eye, endPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 clippedEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : endPoint;

        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        Vec3 reachVec = look.scale(reach);
        AABB region = player.getBoundingBox().expandTowards(reachVec.x, reachVec.y, reachVec.z).inflate(1.0D);
        for (Entity candidate : level.getEntities(player, region, e -> e instanceof LivingEntity)) {
            AABB aabb = candidate.getBoundingBox().inflate(0.3D);
            var clip = aabb.clip(eye, clippedEnd);
            if (clip.isEmpty()) continue;
            double dist = eye.distanceTo(clip.get());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = (LivingEntity) candidate;
            }
        }
        return nearest;
    }

    // ==================== ammo impact lambdas ====================

    private static void igniteFire(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            HbmLivingAttachment props = HbmLivingAttachment.getData(living);
            if (props.getFire() < 100) {
                props.setFire(100);
                living.setData(ModAttachments.LIVING_ATTACHMENT, props);
            }
        }
        // Block linger is onRicochet — Exact CE {@code LAMBDA_LINGER_DIESEL} :77.
    }

    /** CE {@code XFactoryFlamer.java:77} {@code LAMBDA_LINGER_DIESEL}. */
    public static void lingerDiesel(EntityBulletBaseMK4 bullet, BlockHitResult mop) {
        if (!igniteIfPossible(bullet, mop)) spawnFire(bullet, mop, 2F, 1F, 100, EntityFireLingering.TYPE_DIESEL);
    }

    /** CE {@code XFactoryFlamer.java:78} {@code LAMBDA_LINGER_GAS} — ignite only, no puddle. */
    public static void lingerGas(EntityBulletBaseMK4 bullet, BlockHitResult mop) {
        igniteIfPossible(bullet, mop);
    }

    /** CE {@code XFactoryFlamer.java:79} {@code LAMBDA_LINGER_NAPALM}. */
    public static void lingerNapalm(EntityBulletBaseMK4 bullet, BlockHitResult mop) {
        if (!igniteIfPossible(bullet, mop)) spawnFire(bullet, mop, 2.5F, 1F, 200, EntityFireLingering.TYPE_DIESEL);
    }

    /** CE {@code XFactoryFlamer.java:80} {@code LAMBDA_LINGER_BALEFIRE}. */
    public static void lingerBalefire(EntityBulletBaseMK4 bullet, BlockHitResult mop) {
        spawnFire(bullet, mop, 3F, 1F, 300, EntityFireLingering.TYPE_BALEFIRE);
    }

    /** CE {@code XFactoryFlamer.java:82-97}. */
    public static boolean igniteIfPossible(EntityBulletBaseMK4 bullet, BlockHitResult mop) {
        Level world = bullet.level();
        BlockPos pos = mop.getBlockPos();
        Direction face = mop.getDirection();
        BlockState state = world.getBlockState(pos);
        if (state.isFlammable(world, pos, face.getOpposite())) {
            BlockPos adj = pos.relative(face);
            if (world.getBlockState(adj).isAir()) {
                world.setBlock(adj, Blocks.FIRE.defaultBlockState(), 3);
                return true;
            }
        }
        bullet.discard();
        return false;
    }

    /** CE {@code XFactoryFlamer.java:100-111}. */
    public static void spawnFire(EntityBulletBaseMK4 bullet, BlockHitResult mop, float width, float height, int duration, int type) {
        Vec3 hit = mop.getLocation();
        Level world = bullet.level();
        AABB box = new AABB(hit.x, hit.y, hit.z, hit.x, hit.y, hit.z)
                .inflate(width / 2D + 0.5D, height / 2D + 0.5D, width / 2D + 0.5D);
        if (world.getEntitiesOfClass(EntityFireLingering.class, box).isEmpty()) {
            EntityFireLingering.spawn(world, hit.x, hit.y, hit.z, width, height, type, duration);
        }
        bullet.discard();
    }

    /** CE {@code spawnFire} only acts on {@code typeOfHit.BLOCK}. */
    public static void spawnFire(EntityBulletBaseMK4 bullet, HitResult mop, float width, float height, int duration, int type) {
        if (mop instanceof BlockHitResult bhr) spawnFire(bullet, bhr, width, height, duration, type);
    }

    /** Local copy of CE {@code Lego.standardExplode(bullet, mop, range)} — same as {@code XFactory40mm}. */
    private static void standardExplode(EntityBulletBaseMK4 bullet, HitResult hr, float range) {
        Vec3 hit = hr.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, range, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    /** CE {@code XFactoryFlamer.java:130-131}. */
    public static void daybreakerDiesel(EntityBulletBaseMK4 bullet, HitResult mop) {
        standardExplode(bullet, mop, 5F);
        spawnFire(bullet, mop, 6F, 2F, 200, EntityFireLingering.TYPE_DIESEL);
        bullet.discard();
    }

    /** CE {@code XFactoryFlamer.java:132-133}. */
    public static void daybreakerGas(EntityBulletBaseMK4 bullet, HitResult mop) {
        standardExplode(bullet, mop, 5F);
        bullet.discard();
    }

    /** CE {@code XFactoryFlamer.java:134-135}. */
    public static void daybreakerNapalm(EntityBulletBaseMK4 bullet, HitResult mop) {
        standardExplode(bullet, mop, 7.5F);
        spawnFire(bullet, mop, 6F, 2F, 300, EntityFireLingering.TYPE_DIESEL);
        bullet.discard();
    }

    /** CE {@code XFactoryFlamer.java:136-137}. */
    public static void daybreakerBalefire(EntityBulletBaseMK4 bullet, HitResult mop) {
        standardExplode(bullet, mop, 5F);
        spawnFire(bullet, mop, 7.5F, 2.5F, 400, EntityFireLingering.TYPE_BALEFIRE);
        bullet.discard();
    }

    private static void igniteBalefire(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            HbmLivingAttachment props = HbmLivingAttachment.getData(living);
            if (props.getBalefire() < 200) {
                props.setBalefire(200);
                living.setData(ModAttachments.LIVING_ATTACHMENT, props);
            }
        }
        // Block linger is onRicochet — Exact CE {@code LAMBDA_LINGER_BALEFIRE} :80.
    }
}
