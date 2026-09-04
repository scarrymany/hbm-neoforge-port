package com.hbm.items.weapon.sedna.content;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.capability.ModAttachments;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.BobMathUtil;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import com.hbm.util.EntityDamageUtil;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory12ga} - the 12ga ammo family (9
 * regular {@link BulletConfig}s, 2 secret novelty rounds sharing one item, and a parallel 6-config
 * "shredder"/"sub" laser-beam submunition system built by {@link #makeShredderConfig}/
 * {@link #makeShredderSubmunition} for {@code gun_autoshotgun_shredder}'s belt) and 8 guns
 * ({@code gun_maresleg}(+akimbo/broken variants), {@code gun_liberator}, {@code gun_spas12},
 * {@code gun_autoshotgun}(+shredder/sexy variants)). See {@code docs/phase3/guns_and_ammo.md}'s
 * {@code XFactory12ga} table; every stat/lambda below is cross-checked against a full read of CE's
 * real {@code XFactory12ga.java} (697 lines), not just the report's summarized table.
 * <p>
 * See {@link XFactory556mm}'s class javadoc for why every ammo/{@code BulletConfig} field here is a
 * plain eager {@code static final} and why {@code .setCasing(...)}/{@code .smoke(...)}/
 * {@code .anim(...)}/{@code .orchestra(...)}/{@code setDefaultAmmo(...)} are all omitted (same
 * unlanded dependencies: no shared casing-item family, no {@code BusAnimationSedna}/
 * {@code Orchestras} default lambdas, no {@code ItemGunBaseNT.defaultAmmo} field yet). The 8 guns
 * below are static METHODS rather than fields for the same reason: constructing them resolves a
 * SoundEvent {@code DeferredHolder} via {@code Receiver.sound(...).get()}, which would throw
 * {@code IllegalStateException} if evaluated at class-load time; {@link GunShotgunItems} wraps each
 * in a method-reference {@code Supplier} for {@code DeferredRegister} instead.
 * <p>
 * <b>{@code makeShredderConfig}/{@code makeShredderSubmunition}.</b> Ricochet block hooks
 * ({@code BlockDetonatable}, {@code deco_crt}; no {@code red_barrel}) are Exact CE
 * {@code XFactory12ga.java:166-171} via {@link BulletConfig#applyRicochetBlockHooks}.
 * PlasmaBlast spawnPulse VFX stays skipped (client-only, {@code Lego} BlackPowderCreator precedent).
 */
public final class XFactory12ga {

    private XFactory12ga() {
    }

    // ==================== ammo (9 regular + 2 secret) ====================
    // .setCasing(...) intentionally omitted - see class javadoc. CE's exact casing (SHOTSHELL x12 for
    // the 3 black-powder shells, BUCKSHOT x6 for the 3 standard tiers, BUCKSHOT_ADVANCED x6 for the 3
    // "advanced" tiers) is preserved in each field's comment for whoever wires that family + Ammo Press.

    /** casing: SHOTSHELL x12 */
    public static Item ITEM_G12_BP;
    /** casing: SHOTSHELL x12 */
    public static Item ITEM_G12_BP_MAGNUM;
    /** casing: SHOTSHELL x12 */
    public static Item ITEM_G12_BP_SLUG;
    /** casing: BUCKSHOT x6 */
    public static Item ITEM_G12;
    /** casing: BUCKSHOT x6 */
    public static Item ITEM_G12_SLUG;
    /** casing: BUCKSHOT x6 */
    public static Item ITEM_G12_FLECHETTE;
    /** casing: BUCKSHOT_ADVANCED x6 */
    public static Item ITEM_G12_MAGNUM;
    /** casing: BUCKSHOT_ADVANCED x6 */
    public static Item ITEM_G12_EXPLOSIVE;
    /** casing: BUCKSHOT_ADVANCED x6 */
    public static Item ITEM_G12_PHOSPHORUS;
    /** CE's ammo_secret G12_EQUESTRIAN constant - one shared item, two BulletConfigs (bj/tkr impact effects). Hidden from the creative tab, matching CE. */
    public static Item ITEM_G12_EQUESTRIAN;

    private static final float BUCKSHOT_SPREAD = 0.035F;
    private static final float MAGNUM_SPREAD = 0.015F;

    /**
     * Reimplementation of CE's {@code Lego.standardExplode(bullet, mop, range)} - see
     * {@code XFactory762mm.tinyExplode}'s javadoc for why this is duplicated locally per family
     * rather than added to the shared {@code Lego.java}. Unlike {@code tinyExplode} (used by
     * {@code XFactory10ga}'s {@code g10_explosive}), CE's real {@code standardExplode} has no
     * block-facing impact offset and no self-hit-tick guard at its call site here - both preserved
     * exactly (this ammo's {@code LAMBDA_STANDARD_EXPLODE} genuinely omits the guard CE's other
     * explosive rounds have, confirmed by reading {@code XFactory12ga.java} directly).
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

    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE = (bullet, hr) -> {
        standardExplode(bullet, hr, 2F);
        bullet.discard();
    };

    /**
     * Port of CE's {@code g12_phosphorus} onImpact - sets the hit living entity's phosphorus stack to
     * 300 (only if currently lower), using the already-ported {@link HbmLivingAttachment#getPhosphorus()}/
     * {@link HbmLivingAttachment#setPhosphorus(int)} fields directly (no {@code HbmLivingProps} facade
     * wraps phosphorus yet, so this follows the same direct-attachment-access precedent as
     * {@code ItemGrenadeUniversal}/{@code ItemGeigerCounter}), re-syncing via
     * {@code entity.setData(...)} per that class's documented mutation contract.
     */
    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_PHOSPHORUS_HIT = (bullet, hr) -> {
        if (hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            HbmLivingAttachment data = HbmLivingAttachment.getData(living);
            if (data.getPhosphorus() < 300) {
                data.setPhosphorus(300);
                living.setData(ModAttachments.LIVING_ATTACHMENT, data);
            }
        }
    };

    // TODO(phase3-easter-eggs): CE's g12_equestrian_bj onImpact spawns EntityDuchessGambit (a boat,
    // "MLP" easter egg) 50 blocks above the hit point - that entity doesn't exist in this port yet
    // (grepped, confirmed absent). Left with no onImpact so the 0-damage novelty round still
    // fires/consumes correctly, matching XFactory44's identical precedent for its equestrian rounds.

    public static final BulletConfig g12_bp = new BulletConfig("g12_bp").setItem(() -> ITEM_G12_BP)
            .setBlackPowder(true).setProjectiles(8).setDamage(0.75F / 8F).setSpread(BUCKSHOT_SPREAD).setRicochetAngle(15F);
    public static final BulletConfig g12_bp_magnum = new BulletConfig("g12_bp_magnum").setItem(() -> ITEM_G12_BP_MAGNUM)
            .setBlackPowder(true).setProjectiles(4).setDamage(0.75F / 4F).setSpread(BUCKSHOT_SPREAD).setRicochetAngle(25F);
    public static final BulletConfig g12_bp_slug = new BulletConfig("g12_bp_slug").setItem(() -> ITEM_G12_BP_SLUG)
            .setBlackPowder(true).setDamage(0.75F).setSpread(0.01F).setRicochetAngle(5F);
    public static final BulletConfig g12 = new BulletConfig("g12").setItem(() -> ITEM_G12)
            .setProjectiles(8).setDamage(1F / 8F).setSpread(BUCKSHOT_SPREAD).setRicochetAngle(15F).setThresholdNegation(2F);
    public static final BulletConfig g12_slug = new BulletConfig("g12_slug").setItem(() -> ITEM_G12_SLUG)
            .setHeadshot(1.5F).setSpread(0F).setRicochetAngle(25F).setThresholdNegation(4F).setArmorPiercing(0.15F);
    /** CE's field calls {@code setThresholdNegation} twice (5F, then 3F) - the second call wins, preserved verbatim rather than "fixed". */
    public static final BulletConfig g12_flechette = new BulletConfig("g12_flechette").setItem(() -> ITEM_G12_FLECHETTE)
            .setProjectiles(8).setDamage(1F / 8F).setThresholdNegation(5F).setThresholdNegation(3F).setArmorPiercing(0.2F).setSpread(0.025F).setRicochetAngle(5F);
    public static final BulletConfig g12_magnum = new BulletConfig("g12_magnum").setItem(() -> ITEM_G12_MAGNUM)
            .setProjectiles(4).setDamage(2F / 4F).setSpread(MAGNUM_SPREAD).setRicochetAngle(15F).setThresholdNegation(4F);
    public static final BulletConfig g12_explosive = new BulletConfig("g12_explosive").setItem(() -> ITEM_G12_EXPLOSIVE)
            .setDamage(2.5F).setOnImpact(LAMBDA_STANDARD_EXPLODE).setSpread(0F).setRicochetAngle(15F);
    public static final BulletConfig g12_phosphorus = new BulletConfig("g12_phosphorus").setItem(() -> ITEM_G12_PHOSPHORUS)
            .setProjectiles(8).setDamage(1F / 8F).setSpread(MAGNUM_SPREAD).setRicochetAngle(15F).setOnImpact(LAMBDA_PHOSPHORUS_HIT);
    public static final BulletConfig g12_equestrian_bj = new BulletConfig("g12_equestrian_bj").setItem(() -> ITEM_G12_EQUESTRIAN)
            .setDamage(0F);
    public static final BulletConfig g12_equestrian_tkr = new BulletConfig("g12_equestrian_tkr").setItem(() -> ITEM_G12_EQUESTRIAN)
            .setDamage(0F);

    /** CE's {@code all[]} - every non-black-powder-exclusive... actually every regular tier (bp included), matches CE's array verbatim (used by every mag below except the belt-fed shredder/sexy/broken guns). */
    private static final BulletConfig[] ALL = new BulletConfig[] {g12_bp, g12_bp_magnum, g12_bp_slug, g12, g12_slug, g12_flechette, g12_magnum, g12_explosive, g12_phosphorus};

    // ==================== shredder/submunition system (6 pairs) ====================

    /**
     * Port of CE's shredder ricochet lambda - see class javadoc for the 2 documented simplifications
     * (VFX pulse, block-special-case branches). The glass-destroy/reflect/ricochet-count/teleport-sync
     * physics are ported 1:1 from {@link BulletConfig#LAMBDA_STANDARD_RICOCHET} (the confirmed real
     * 1.21 API shape for this exact mechanic), with CE's extra AoE PLASMA damage burst to entities
     * within 0.5 blocks of the ricochet point added on top, matching CE's real behavior.
     */
    private static final BiConsumer<EntityBulletBaseMK4, BlockHitResult> LAMBDA_SHREDDER_RICOCHET = (bullet, bhr) -> {

        Level level = bullet.level();
        BlockPos pos = bhr.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Vec3 hitLoc = bhr.getLocation();

        if (state.getSoundType() == SoundType.GLASS && state.getBlock().getExplosionResistance() < 0.6F) {
            level.destroyBlock(pos, false);
            bullet.setPos(hitLoc);
            return;
        }

        // Exact CE shredder: detonatable + deco_crt only — no red_barrel (XFactory12ga.java:166-171).
        BulletConfig.applyRicochetBlockHooks(level, pos, state, false);

        Direction dir = bhr.getDirection();
        Vec3 face = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        Vec3 vel = bullet.getDeltaMovement().normalize();
        double angle = Math.abs(BobMathUtil.getCrossAngle(vel, face) - 90);

        if (angle <= bullet.config.ricochetAngle) {

            // TODO(phase5-particles): CE also broadcasts a PlasmaBlast spawnPulse VFX packet here -
            // pure client rendering, see class javadoc.

            AABB blast = new AABB(hitLoc.x, hitLoc.y, hitLoc.z, hitLoc.x, hitLoc.y, hitLoc.z).inflate(0.5D);
            DamageSource source = BulletConfig.getDamage(bullet, bullet.getThrower(), DamageClass.PLASMA);
            for (Entity e : level.getEntities(bullet, blast)) {
                if (!e.isAlive()) continue;
                if (e instanceof LivingEntity living) {
                    EntityDamageUtil.attackEntityFromNT(living, source, bullet.damage, true, false, 0D, 0F, 0F);
                } else {
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(e, source, bullet.damage);
                }
            }

            bullet.ricochets++;
            if (bullet.ricochets > bullet.config.maxRicochetCount) {
                bullet.setPos(hitLoc);
                bullet.discard();
            }

            Vec3 motion = bullet.getDeltaMovement();
            bullet.setDeltaMovement(switch (dir) {
                case DOWN, UP -> new Vec3(motion.x, motion.y * -1, motion.z);
                case NORTH, SOUTH -> new Vec3(motion.x, motion.y, motion.z * -1);
                case WEST, EAST -> new Vec3(motion.x * -1, motion.y, motion.z);
            });

            level.playSound(null, bullet.getX(), bullet.getY(), bullet.getZ(), HBMSoundHandler.ricochet.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
            bullet.setPos(hitLoc);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(bullet, new ClientboundTeleportEntityPacket(bullet));
            }
        } else {
            bullet.setPos(hitLoc);
            bullet.discard();
        }
    };

    /** Port of CE's {@code makeShredderSubmunition} - clones the original ammo (inheriting its onImpact/onEntityHit/damage unchanged, matching CE's shallow-clone exactly) then overrides ricochet/velocity/life/damage-class for the sub-projectile pass. */
    public static BulletConfig makeShredderSubmunition(BulletConfig original) {
        return original.clone(original.id.getPath() + "_sub")
                .setRicochetAngle(90F).setRicochetCount(3).setVel(0.5F).setLife(50)
                .setupDamageClass(DamageClass.PLASMA).setOnRicochet(LAMBDA_SHREDDER_RICOCHET);
    }

    /** Port of CE's {@code makeShredderConfig} - a laser-beam carrier config whose beam-impact bursts into {@code submunition}'s projectile count, plus (on a block hit only) an AoE LASER-class splash to nearby entities, matching CE's real branch shape (entity hits get no splash, only the submunition burst). */
    public static BulletConfig makeShredderConfig(BulletConfig original, BulletConfig submunition) {
        BulletConfig cfg = new BulletConfig(original.id.getPath() + "_shredder")
                .setBeam().setRenderRotations(false).setLife(5)
                .setDamage(original.damageMult * original.projectilesMax)
                .setupDamageClass(DamageClass.LASER)
                .setItem(original.getAmmo());

        cfg.setOnBeamImpact((beam, hr) -> {

            int projectiles = submunition.projectilesMin;
            if (submunition.projectilesMax > submunition.projectilesMin) {
                projectiles += beam.level().random.nextInt(submunition.projectilesMax - submunition.projectilesMin + 1);
            }

            if (hr instanceof BlockHitResult bhr) {
                Direction dir = bhr.getDirection();
                Vec3 hit = bhr.getLocation().add(dir.getStepX() * 0.1, dir.getStepY() * 0.1, dir.getStepZ() * 0.1);

                // TODO(phase5-particles): CE also broadcasts a PlasmaBlast spawnPulse VFX packet here - see class javadoc.

                AABB blast = new AABB(hit.x, hit.y, hit.z, hit.x, hit.y, hit.z).inflate(0.75D);
                DamageSource source = BulletConfig.getDamage(beam, beam.getThrower(), DamageClass.LASER);
                for (Entity e : beam.level().getEntities(beam, blast)) {
                    if (!e.isAlive()) continue;
                    if (e instanceof LivingEntity living) {
                        EntityDamageUtil.attackEntityFromNT(living, source, beam.damage, true, false, 0D, 0F, 0F);
                    } else {
                        EntityDamageUtil.attackEntityFromIgnoreIFrame(e, source, beam.damage);
                    }
                }

                Vec3 dirVec = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
                for (int i = 0; i < projectiles; i++) {
                    EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(beam.level(), beam.getThrower(), submunition, beam.damage * submunition.damageMult, 0.2F, hit, dirVec);
                    beam.level().addFreshEntity(bullet);
                }
            } else if (hr instanceof EntityHitResult ehr) {
                Vec3 hit = ehr.getLocation();

                // TODO(phase5-particles): spawnPulse VFX - see class javadoc.

                for (int i = 0; i < projectiles; i++) {
                    Vec3 dirVec = new Vec3(beam.level().random.nextGaussian(), beam.level().random.nextGaussian(), beam.level().random.nextGaussian()).normalize();
                    EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(beam.level(), beam.getThrower(), submunition, beam.damage * submunition.damageMult, 0.2F, hit, dirVec);
                    beam.level().addFreshEntity(bullet);
                }
            }
        });

        return cfg;
    }

    public static final BulletConfig g12_sub = makeShredderSubmunition(g12);
    public static final BulletConfig g12_sub_slug = makeShredderSubmunition(g12_slug);
    public static final BulletConfig g12_sub_flechette = makeShredderSubmunition(g12_flechette);
    public static final BulletConfig g12_sub_magnum = makeShredderSubmunition(g12_magnum);
    public static final BulletConfig g12_sub_explosive = makeShredderSubmunition(g12_explosive);
    public static final BulletConfig g12_sub_phosphorus = makeShredderSubmunition(g12_phosphorus);

    public static final BulletConfig g12_shredder = makeShredderConfig(g12, g12_sub);
    public static final BulletConfig g12_shredder_slug = makeShredderConfig(g12_slug, g12_sub_slug);
    public static final BulletConfig g12_shredder_flechette = makeShredderConfig(g12_flechette, g12_sub_flechette);
    public static final BulletConfig g12_shredder_magnum = makeShredderConfig(g12_magnum, g12_sub_magnum);
    public static final BulletConfig g12_shredder_explosive = makeShredderConfig(g12_explosive, g12_sub_explosive);
    public static final BulletConfig g12_shredder_phosphorus = makeShredderConfig(g12_phosphorus, g12_sub_phosphorus);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_MARESLEG =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_LIBERATOR =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_AUTOSHOTGUN =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5) + 1.5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
    /** Package-visible (not private) - CE's own {@code XFactory10ga.gun_autoshotgun_heretic} reuses this exact same lambda cross-file, matching that real reference. */
    static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_SEXY =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));

    /**
     * Port of CE's {@code LAMBDA_SPAS_SECONDARY} - the SPAS-12's secondary (RMB) fire handler. Not a
     * copy of {@link Lego#clickReceiver}: CE's real loop fires {@code roundsPerCycle} ADDITIONAL times
     * after the first shot (not {@code roundsPerCycle - 1} like the standard primary-click path), so
     * with the default {@code roundsPerCycle() == 1} this genuinely double-fires per RMB click - a real
     * CE quirk verified by reading the source directly, not a bug introduced by this port. The fire
     * pitch is dropped slightly on the second-or-later shot, matching CE's own {@code timeFired > 1} check.
     */
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_SPAS_SECONDARY = (stack, ctx) -> {
        LivingEntity entity = ctx.entity;
        Receiver rec = ctx.config.getReceivers(stack)[0];
        int index = ctx.configIndex;
        ItemGunBaseNT.GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == ItemGunBaseNT.GunState.IDLE) {
            if (rec.getCanFire(stack).apply(stack, ctx)) {
                rec.getOnFire(stack).accept(stack, ctx);
                int remaining = rec.getRoundsPerCycle(stack);
                int timesFired = 1;
                for (int i = 0; i < remaining; i++) {
                    if (rec.getCanFire(stack).apply(stack, ctx)) {
                        rec.getOnFire(stack).accept(stack, ctx);
                        timesFired++;
                    }
                }
                if (rec.getFireSound(stack) != null) {
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), rec.getFireSound(stack), SoundSource.PLAYERS, rec.getFireVolume(stack), rec.getFirePitch(stack) * (timesFired > 1 ? 0.9F : 1F));
                }
                ItemGunBaseNT.setState(stack, index, ItemGunBaseNT.GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
            } else if (rec.getDoesDryFire(stack)) {
                ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimationType.CYCLE_DRY, index);
                ItemGunBaseNT.setState(stack, index, ItemGunBaseNT.GunState.DRAWING);
                ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
            }
        }
        if (state == ItemGunBaseNT.GunState.RELOADING) {
            ItemGunBaseNT.setReloadCancel(stack, true);
        }
    };

    // ==================== guns (8) ====================

    public static ItemGunBaseNT gun_maresleg() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(600).draw(10).inspect(39).reloadSequential(true).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(16F).delay(20).reload(22, 10, 13, 0).jam(24).sound(HBMSoundHandler.fireShotgun.get(), 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(0, 6).addConfigs(ALL))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_MARESLEG))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G12 x12
        );
    }

    public static ItemGunBaseNT gun_maresleg_akimbo() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
            new GunConfig()
                    .dura(600).draw(5).inspect(39).reloadSequential(true).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(16F).spreadHipfire(0F).spreadAmmo(1.35F).delay(20).reload(22, 10, 13, 0).jam(24).sound(HBMSoundHandler.fireShotgun.get(), 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(0, 6).addConfigs(ALL))
                            .offset(0.75, -0.0625, 0.1875D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_MARESLEG))
                    .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER),
            new GunConfig()
                    .dura(600).draw(5).inspect(39).reloadSequential(true).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(16F).spreadHipfire(0F).spreadAmmo(1.35F).delay(20).reload(22, 10, 13, 0).jam(24).sound(HBMSoundHandler.fireShotgun.get(), 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(1, 6).addConfigs(ALL))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_MARESLEG))
                    .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
            // default ammo (not yet wired): G12 x24
        );
    }

    public static ItemGunBaseNT gun_maresleg_broken() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
            new GunConfig()
                    .dura(0).draw(5).inspect(39).reloadSequential(true).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(32F).spreadAmmo(1.15F).delay(20).reload(22, 10, 13, 0).jam(24).sound(HBMSoundHandler.fireShotgun.get(), 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(0, 6).addConfigs(g12_equestrian_tkr, g12_bp, g12_bp_magnum, g12_bp_slug, g12, g12_slug, g12_flechette, g12_magnum, g12_explosive, g12_phosphorus))
                            .offset(0.75, -0.0625, -0.1875D)
                            .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_NOWEAR_FIRE).recoil(LAMBDA_RECOIL_MARESLEG))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G12_MAGNUM x24
        );
    }

    public static ItemGunBaseNT gun_liberator() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(200).draw(20).inspect(21).reloadSequential(true).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(16F).delay(20).rounds(4).reload(25, 15, 7, 0).jam(45).sound(HBMSoundHandler.fireShotgunAlt.get(), 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(0, 4).addConfigs(ALL))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_LIBERATOR))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G12 x12
        );
    }

    public static ItemGunBaseNT gun_spas12() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(600).draw(20).inspect(39).reloadSequential(true).reloadChangeType(true).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(32F).spreadHipfire(0F).delay(20).reload(5, 10, 10, 10, 0).jam(36).sound(HBMSoundHandler.shotgunShoot.get(), 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(0, 8).addConfigs(ALL))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_MARESLEG))
                    .setupStandardConfiguration().ps(LAMBDA_SPAS_SECONDARY).pt(null)
            // default ammo (not yet wired): G12 x16
        );
    }

    public static ItemGunBaseNT gun_autoshotgun() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(2_000).draw(10).inspect(33).reloadSequential(true).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(48F).delay(10).auto(true).autoAfterDry(true).dryfireAfterAuto(true).reload(44).jam(19).sound(HBMSoundHandler.fireShotgunAuto.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 20).addConfigs(ALL))
                            .offset(0.75, -0.125, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_AUTOSHOTGUN))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G12 x20
        );
    }

    public static ItemGunBaseNT gun_autoshotgun_shredder() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
            new GunConfig()
                    .dura(2_000).draw(10).inspect(33).reloadSequential(true).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(50F).delay(10).auto(true).autoAfterDry(true).dryfireAfterAuto(true).reload(44).jam(19).sound(HBMSoundHandler.fireShotgunAuto.get(), 1.0F, 1.0F)
                            .mag(new MagazineBelt().addConfigs(g12_shredder, g12_shredder_slug, g12_shredder_flechette, g12_shredder_magnum, g12_shredder_explosive, g12_shredder_phosphorus))
                            .offset(0.75, -0.125, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_AUTOSHOTGUN))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G12 x20
        );
    }

    public static ItemGunBaseNT gun_autoshotgun_sexy() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
            new GunConfig()
                    .dura(5_000).draw(20).inspect(65).reloadSequential(true).inspectCancel(false).crosshair(Crosshair.L_CIRCLE).hideCrosshair(false)
                    .rec(new Receiver(0)
                            .dmg(64F).delay(4).auto(true).dryfireAfterAuto(true).reload(110).jam(19).sound(HBMSoundHandler.fireShotgunAuto.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 100).addConfigs(g12_equestrian_bj, g12_bp, g12_bp_magnum, g12_bp_slug, g12, g12_slug, g12_flechette, g12_magnum, g12_explosive, g12_phosphorus))
                            .offset(0.75, -0.125, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_SEXY))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G12_MAGNUM x50
        );
    }
}
