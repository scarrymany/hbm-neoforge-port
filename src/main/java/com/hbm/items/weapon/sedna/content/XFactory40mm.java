package com.hbm.items.weapon.sedna.content;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.capability.ModAttachments;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
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
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory40mm} - the 40mm grenade/flare-launcher
 * ammo family (3 flare {@link BulletConfig}s + 5 explosive-shell configs cloned from a shared
 * {@code g40_base} template) and its 3 guns ({@code gun_flaregun}, {@code gun_congolake},
 * {@code gun_mk108}). See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory40mm} table;
 * cross-checked against a full read of CE's real {@code XFactory40mm.java}.
 * <p>
 * See {@link XFactory556mm}'s class javadoc for why every ammo/{@code BulletConfig} field here is a
 * plain eager {@code static final} and why {@code .setCasing(...)}/{@code .smoke(...)}/
 * {@code .anim(...)}/{@code .orchestra(...)}/{@code setDefaultAmmo(...)} are all omitted, and why the
 * 3 guns below are static METHODS rather than fields (deferring the {@code Receiver.sound(...).get()}
 * SoundEvent {@code DeferredHolder} resolution until {@code RegisterEvent(ITEM)} time, via
 * {@link GunLauncherItems}'s method-reference {@code Supplier}s).
 * <p>
 * <b>{@code g40_base} - collapsed from a clone-template into per-config repetition.</b> CE builds a
 * single {@code BulletConfig g40_base = new BulletConfig().setLife(200).setVel(2F).setGrav(0.035F)}
 * and derives all 5 {@code g40_*} shell configs via {@code g40_base.clone()}. This port's
 * {@link BulletConfig} constructor unconditionally registers into a real id-keyed registry (see that
 * class's javadoc) - there is no unregistered "template" instance concept - so {@code g40_base}'s 3
 * fields ({@code setLife(200).setVel(2F).setGrav(0.035D)}) are simply repeated on each of the 5 real
 * configs below instead of introducing an extra, never-fired {@code "g40_base"} registry entry.
 * Observably identical; only the code shape differs.
 * <p>
 * {@code g40_inc}/{@code g40_phosphorus} linger is Exact CE {@code XFactory40mm.java:91-116}
 * via registered {@link EntityFireLingering} (5×2, 200t DIESEL / 400t PHOSPHORUS) plus the
 * 3×3×3 adjacent-flammable ignite loop. {@code g26_flare_supply}/{@code _weapon} C130 airdrop
 * is already wired via {@link #spawnPlane}.
 */
public final class XFactory40mm {

    private XFactory40mm() {
    }

    // ==================== ammo ====================
    // .setCasing(...) intentionally omitted - see class javadoc. CE's exact casing (LARGE x4 for every
    // round in this family) is preserved below for whoever wires that family + Ammo Press.

    /** casing: LARGE x4 */
    public static Item ITEM_G26_FLARE;
    /** casing: LARGE x4 */
    public static Item ITEM_G26_FLARE_SUPPLY;
    /** casing: LARGE x4 */
    public static Item ITEM_G26_FLARE_WEAPON;
    /** casing: LARGE x4 */
    public static Item ITEM_G40_HE;
    /** casing: LARGE x4 */
    public static Item ITEM_G40_HEAT;
    /** casing: LARGE x4 */
    public static Item ITEM_G40_DEMO;
    /** casing: LARGE x4 */
    public static Item ITEM_G40_INC;
    /** casing: LARGE x4 */
    public static Item ITEM_G40_PHOSPHORUS;

    /**
     * Port of CE's {@code g26_flare}'s {@code LAMBDA_STANDARD_IGNITE} onImpact - adds 200 to the hit
     * living entity's fire-status stack, using {@link HbmLivingAttachment#getFire()}/
     * {@link HbmLivingAttachment#setFire(int)} directly (no {@code HbmLivingProps} facade wraps fire
     * yet), re-syncing via {@code entity.setData(...)} per that class's documented mutation contract -
     * same direct-attachment-access precedent {@link XFactory12ga#g12_phosphorus}'s hit lambda uses.
     */
    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_IGNITE = (bullet, hr) -> {
        if (hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            HbmLivingAttachment data = HbmLivingAttachment.getData(living);
            data.setFire(data.getFire() + 200);
            living.setData(ModAttachments.LIVING_ATTACHMENT, data);
        }
    };

    /**
     * Reimplementation of CE's {@code Lego.standardExplode(bullet, mop, range)} - see
     * {@link XFactory12ga}'s identical local reimplementation for why this is duplicated per
     * package rather than added to the shared {@code Lego.java}.
     */
    private static void standardExplode(EntityBulletBaseMK4 bullet, HitResult hr, float range) {
        Vec3 hit = hr.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, range, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    /** {@code g40_he} onImpact - CE genuinely has no self-hit-tick guard here (confirmed by reading the source directly), unlike every other explosive lambda in this file. */
    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE = (bullet, hr) -> {
        standardExplode(bullet, hr, 5F);
        bullet.discard();
    };

    /** {@code g40_heat} onImpact - standardExplode(3.5F) plus an extra flat 3x-damage point-blank hit on whatever entity was actually struck (HEAT shaped-charge behavior). */
    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_HEAT = (bullet, hr) -> {
        if (hr instanceof EntityHitResult selfCheck && bullet.tickCount < 3 && selfCheck.getEntity() == bullet.getThrower()) return;

        standardExplode(bullet, hr, 3.5F);
        bullet.discard();

        if (hr instanceof EntityHitResult ehr) {
            DamageSource source = BulletConfig.getDamage(bullet, bullet.getThrower(), DamageClass.EXPLOSIVE);
            Entity hitEntity = ehr.getEntity();
            if (hitEntity instanceof LivingEntity living) {
                EntityDamageUtil.attackEntityFromNT(living, source, bullet.damage * 3F, true, true, 0.5D, 3F, 0.15F);
            } else {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(hitEntity, source, bullet.damage * 3F);
            }
        }
    };

    /** {@code g40_demo} onImpact - a real block-destroying explosion (BlockAllocatorStandard/BlockProcessorStandard, unlike every other config in this file which is entity-damage-only), matching CE's demolition-charge behavior exactly. Note CE's own EntityProcessorCrossSmooth here has no {@code .setupPiercing(...)} call, unlike {@link #standardExplode} - preserved verbatim. */
    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_DEMO = (bullet, hr) -> {
        if (hr instanceof EntityHitResult selfCheck && bullet.tickCount < 3 && selfCheck.getEntity() == bullet.getThrower()) return;

        Vec3 hit = hr.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 5F, bullet.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
        bullet.discard();
    };

    /** Exact CE {@code XFactory40mm.java:91-116}. */
    private static void spawnFire(EntityBulletBaseMK4 bullet, HitResult hr, boolean phosphorus, int duration) {
        if (hr instanceof EntityHitResult selfCheck && bullet.tickCount < 3 && selfCheck.getEntity() == bullet.getThrower()) return;

        standardExplode(bullet, hr, 3F);
        Vec3 hit = hr.getLocation();
        Level world = bullet.level();
        EntityFireLingering.spawn(world, hit.x, hit.y, hit.z, 5F, 2F,
                phosphorus ? EntityFireLingering.TYPE_PHOSPHORUS : EntityFireLingering.TYPE_DIESEL, duration);
        bullet.discard();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = new BlockPos((int) Math.floor(hit.x) + dx, (int) Math.floor(hit.y) + dy, (int) Math.floor(hit.z) + dz);
                    if (!world.getBlockState(pos).isAir()) continue;
                    for (Direction dir : Direction.values()) {
                        BlockPos adj = pos.relative(dir);
                        BlockState neighbor = world.getBlockState(adj);
                        if (neighbor.isFlammable(world, adj, dir.getOpposite())) {
                            world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_INC = (bullet, hr) -> spawnFire(bullet, hr, false, 200);
    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_PHOSPHORUS = (bullet, hr) -> spawnFire(bullet, hr, true, 400);

    public static final BulletConfig g26_flare = new BulletConfig("g26_flare").setItem(() -> ITEM_G26_FLARE)
            .setLife(100).setVel(2F).setGrav(0.015D).setRenderRotations(false).setOnImpact(LAMBDA_STANDARD_IGNITE);
    /** Phase 4 (entities_vehicles_aircraft/entities_orbital_and_beam_payloads): closes this class's own
     *  previously-documented forward reference now that {@code EntityC130} is real - see {@link
     *  #spawnPlane}. */
    public static final BulletConfig g26_flare_supply = new BulletConfig("g26_flare_supply").setItem(() -> ITEM_G26_FLARE_SUPPLY)
            .setLife(100).setVel(2F).setGrav(0.015D).setRenderRotations(false).setOnImpact(LAMBDA_STANDARD_IGNITE)
            .setOnUpdate(entity -> spawnPlane(entity, com.hbm.entity.logic.EntityC130.C130PayloadType.SUPPLIES));
    /** Same as {@link #g26_flare_supply} (weapons-payload variant). */
    public static final BulletConfig g26_flare_weapon = new BulletConfig("g26_flare_weapon").setItem(() -> ITEM_G26_FLARE_WEAPON)
            .setLife(100).setVel(2F).setGrav(0.015D).setRenderRotations(false).setOnImpact(LAMBDA_STANDARD_IGNITE)
            .setOnUpdate(entity -> spawnPlane(entity, com.hbm.entity.logic.EntityC130.C130PayloadType.WEAPONS));

    /**
     * CE: {@code spawnPlane(Entity, C130PayloadType)} - at tick 40, calls in an {@code EntityC130}
     * supply-drop plane targeting the flare's current ground position (top-of-world height at this
     * x/z). Spawned via plain {@code level.addFreshEntity(...)}, matching {@code EntityC130}'s own
     * established chunk-loading/spawn-plumbing substitution (see that class's javadoc) - CE's {@code
     * WorldUtil.loadAndSpawnEntityInWorld}/{@code TrackerUtil.setTrackingRange} are dropped entirely.
     */
    private static void spawnPlane(Entity entity, com.hbm.entity.logic.EntityC130.C130PayloadType payload) {
        if (entity.level().isClientSide() || entity.tickCount != 40) return;
        if (!(entity instanceof EntityBulletBaseMK4 bullet)) return;

        net.minecraft.world.level.Level level = bullet.level();
        int x = (int) Math.floor(bullet.getX());
        int z = (int) Math.floor(bullet.getZ());
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);

        if (bullet.getThrower() != null) {
            level.playSound(null, bullet.getThrower().blockPosition(), HBMSoundHandler.techBleep.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        com.hbm.entity.logic.EntityC130 c130 = new com.hbm.entity.logic.EntityC130(level);
        c130.fac(level, x, y, z, payload);
        level.addFreshEntity(c130);
    }

    public static final BulletConfig g40_he = new BulletConfig("g40_he").setItem(() -> ITEM_G40_HE)
            .setLife(200).setVel(2F).setGrav(0.035D).setOnImpact(LAMBDA_STANDARD_EXPLODE);
    public static final BulletConfig g40_heat = new BulletConfig("g40_heat").setItem(() -> ITEM_G40_HEAT)
            .setLife(200).setVel(2F).setGrav(0.035D).setDamage(0.5F).setOnImpact(LAMBDA_STANDARD_EXPLODE_HEAT);
    public static final BulletConfig g40_demo = new BulletConfig("g40_demo").setItem(() -> ITEM_G40_DEMO)
            .setLife(200).setVel(2F).setGrav(0.035D).setDamage(0.75F).setOnImpact(LAMBDA_STANDARD_EXPLODE_DEMO);
    public static final BulletConfig g40_inc = new BulletConfig("g40_inc").setItem(() -> ITEM_G40_INC)
            .setLife(200).setVel(2F).setGrav(0.035D).setDamage(0.75F).setOnImpact(LAMBDA_STANDARD_EXPLODE_INC);
    public static final BulletConfig g40_phosphorus = new BulletConfig("g40_phosphorus").setItem(() -> ITEM_G40_PHOSPHORUS)
            .setLife(200).setVel(2F).setGrav(0.035D).setDamage(0.75F).setOnImpact(LAMBDA_STANDARD_EXPLODE_PHOSPHORUS);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_GL =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_MK108 =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 1.0) + 1F, (float) (ctx.getPlayer().getRandom().nextGaussian()));

    // ==================== guns (3) ====================

    public static ItemGunBaseNT gun_flaregun() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(100).draw(7).inspect(39).crosshair(Crosshair.L_CIRCUMFLEX)
                    .rec(new Receiver(0)
                            .dmg(15F).delay(20).reload(28).jam(33).sound(HBMSoundHandler.hkShoot.get(), 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(0, 1).addConfigs(g26_flare, g26_flare_supply, g26_flare_weapon))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_GL))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G26_FLARE x3
        );
    }

    public static ItemGunBaseNT gun_congolake() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(400).draw(7).inspect(39).reloadSequential(true).reloadChangeType(true).crosshair(Crosshair.L_CIRCUMFLEX)
                    .rec(new Receiver(0)
                            .dmg(20F).delay(24).reload(16, 16, 16, 0).jam(0).sound(HBMSoundHandler.glShoot.get(), 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(0, 4).addConfigs(g40_he, g40_heat, g40_demo, g40_inc, g40_phosphorus))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_GL))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G40_HE x8
        );
    }

    public static ItemGunBaseNT gun_mk108() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(5_000).draw(20).inspect(65).crosshair(Crosshair.L_CIRCUMFLEX).hideCrosshair(false)
                    .rec(new Receiver(0)
                            .dmg(25F).delay(10).auto(true).dryfireAfterAuto(true).reload(135).jam(25).sound(HBMSoundHandler.mk108Shoot.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(g40_he, g40_heat, g40_demo, g40_inc, g40_phosphorus))
                            .offset(0.75, -0.125, -0.125D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_MK108))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G40_HE x50
        );
    }
}
