package com.hbm.items.weapon.sedna.content;

import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineInfinite;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryAccelerator} - the 3 particle-
 * accelerator sidearms, each with its own bespoke ammo (no shared caliber family). See
 * {@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryAccelerator} table.
 * <p>
 * <b>{@code tau_uranium}/{@code coil_tungsten}/{@code coil_ferrouranium} have no discrete ammo
 * item</b> - CE's own {@code .setItem(GunFactory.EnumAmmo....)} calls exist purely to bind an
 * {@code ammo_standard} metadata variant for the Ammo Press/inventory-reload scan; since
 * {@code tau_uranium} is fed via {@link MagazineBelt} (draws straight from a held ammo item) and
 * {@code coil_*} via {@link MagazineSingleReload}, both still need a real backing {@link Item} for
 * the reload scan to match against - see {@code XFactory556mm}'s class javadoc for why every ammo
 * item below is a plain eager {@code static final} field (no sound/registry timing hazard).
 * <p>
 * <b>{@code gun_n_i_4_n_i}'s coin-throwing secondary fire is a documented forward reference, not
 * ported</b>: CE's {@code LAMBDA_NI4NI_SECONDARY_PRESS} spawns a {@code com.hbm.entity.item.EntityCoin}
 * from a per-stack coin counter ({@code ItemGunNI4NI}) - neither the entity nor the bespoke item
 * subclass exist anywhere in this port (confirmed: no {@code EntityCoin} under
 * {@code com.hbm.entity}). The gun's real weapon behavior (an infinite-ammo melee-range arc beam) is
 * fully ported below as a plain {@link ItemGunBaseNT}; the secondary press is a documented no-op
 * until {@code EntityCoin}/{@code ItemGunNI4NI} land (unrelated gambling/currency-item feature, not
 * ammo/ballistics content).
 * <p>
 * <b>{@code gun_tau}'s charged alt-fire is ported</b>: CE's secondary-press/-release pair spins up
 * over a hold, then on release spawns one bigger {@link EntityBulletBeamBase} scaled by hold
 * duration, consuming the belt-fed spectral {@code tau_uranium_charge} config directly (bypassing
 * the normal per-shot fire path) - reproduced faithfully using this port's real
 * {@link EntityBulletBeamBase} constructor and {@link Lego#calcSpread}/{@link Lego#getStandardWearDamage}
 * helpers, which match CE's own 1:1.
 */
public final class XFactoryAccelerator {

    private XFactoryAccelerator() {
    }

    // ==================== ammo ====================

    public static Item ITEM_TAU_URANIUM;
    public static Item ITEM_COIL_TUNGSTEN;
    public static Item ITEM_COIL_FERROURANIUM;

    public static final BulletConfig tau_uranium = new BulletConfig("tau_uranium").setItem(() -> ITEM_TAU_URANIUM)
            .setupDamageClass(DamageClass.SUBATOMIC).setBeam().setLife(5).setRenderRotations(false)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setOnBeamImpact(BulletConfig.LAMBDA_BEAM_HIT);
    /** Charged alt-fire round - spectral (ignores blocks), otherwise identical; never mag-loaded, only reached via {@link #LAMBDA_TAU_SECONDARY_RELEASE}. */
    public static final BulletConfig tau_uranium_charge = new BulletConfig("tau_uranium_charge").setItem(() -> ITEM_TAU_URANIUM)
            .setupDamageClass(DamageClass.SUBATOMIC).setBeam().setLife(5).setRenderRotations(false)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setSpectral(true).setOnBeamImpact(BulletConfig.LAMBDA_BEAM_HIT);

    public static final BulletConfig coil_tungsten = new BulletConfig("coil_tungsten").setItem(() -> ITEM_COIL_TUNGSTEN)
            .setVel(7.5F).setLife(50).setDoesPenetrate(true).setDamageFalloffByPen(false).setSpectral(true)
            .setOnUpdate(entity -> breakInPath(entity, 1.25F));
    public static final BulletConfig coil_ferrouranium = new BulletConfig("coil_ferrouranium").setItem(() -> ITEM_COIL_FERROURANIUM)
            .setVel(7.5F).setLife(50).setDoesPenetrate(true).setDamageFalloffByPen(false).setSpectral(true)
            .setOnUpdate(entity -> breakInPath(entity, 2.5F));

    /** No backing item - {@link MagazineInfinite} never scans an inventory. */
    public static final BulletConfig ni4ni_arc = new BulletConfig("ni4ni_arc")
            .setupDamageClass(DamageClass.PHYSICAL).setBeam().setLife(5).setThresholdNegation(10F).setArmorPiercing(0.2F)
            .setRenderRotations(false).setDoesPenetrate(false).setOnBeamImpact(BulletConfig.LAMBDA_BEAM_HIT);

    /**
     * Port of CE's {@code XFactoryAccelerator.breakInPath} - the coilgun round's in-flight "digs
     * through soft terrain" update tick: walks back along the bullet's last-tick displacement in
     * half-block steps, destroying any air-adjacent(-ish, CE's own check is just
     * {@code isAir && 0 <= hardness < threshold}) block softer than {@code threshold}.
     */
    private static void breakInPath(Entity entity, float threshold) {
        if (entity.level().isClientSide()) return;

        Vec3 delta = entity.position().subtract(entity.getX() - entity.getDeltaMovement().x, entity.getY() - entity.getDeltaMovement().y, entity.getZ() - entity.getDeltaMovement().z);
        double motion = Math.max(delta.length(), 0.1);
        Vec3 dir = delta.normalize();

        for (double d = 0; d < motion; d += 0.5) {
            BlockPos pos = BlockPos.containing(entity.getX() - dir.x * d, entity.getY() - dir.y * d, entity.getZ() - dir.z * d);
            BlockState state = entity.level().getBlockState(pos);
            if (state.isAir()) continue;
            float hardness = state.getDestroySpeed(entity.level(), pos);
            if (hardness >= 0 && hardness < threshold) {
                entity.level().destroyBlock(pos, false);
            }
        }
    }

    // ==================== guns ====================

    public static ItemGunBaseNT gun_tau() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(6_400).draw(10).inspect(10).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(25F).spreadHipfire(0F).spread(0F).delay(4).auto(true)
                                .mag(new MagazineBelt().addConfigs(tau_uranium))
                                .offset(1, -0.15625, -0.25D)
                                .setupStandardFire())
                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).rp(LAMBDA_TAU_PRIMARY_RELEASE)
                        .ps(LAMBDA_TAU_SECONDARY_PRESS).rs(LAMBDA_TAU_SECONDARY_RELEASE)
                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
        // default ammo (not yet wired): TAU_URANIUM x15
    }

    public static ItemGunBaseNT gun_coilgun() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.SPECIAL,
                new GunConfig()
                        .dura(400).draw(5).inspect(39).crosshair(Crosshair.L_CIRCUMFLEX)
                        .rec(new Receiver(0)
                                .dmg(35F).delay(5).reload(20).jam(33).sound(HBMSoundHandler.coilgunShoot.get(), 1.0F, 1.0F)
                                .mag(new MagazineSingleReload(0, 1).addConfigs(coil_tungsten, coil_ferrouranium))
                                .offset(0.75, -0.0625, -0.1875D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_COILGUN))
                        .setupStandardConfiguration());
        // default ammo (not yet wired): COIL_TUNGSTEN x5
    }

    public static ItemGunBaseNT gun_n_i_4_n_i() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.SPECIAL,
                new GunConfig()
                        .dura(0).draw(5).inspect(39).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(35F).delay(10).sound(HBMSoundHandler.coilgunShoot.get(), 1.0F, 1.0F)
                                .mag(new MagazineInfinite(ni4ni_arc))
                                .offset(0.75, -0.0625, -0.1875D)
                                .setupStandardFire().fire(Lego.LAMBDA_NOWEAR_FIRE))
                        .setupStandardConfiguration()
                        // TODO(entity-item-coin): CE's secondary press throws an EntityCoin from a
                        // per-stack counter (ItemGunNI4NI) - see this class's javadoc. No-op until
                        // com.hbm.entity.item.EntityCoin exists.
                        .ps((stack, ctx) -> { }));
    }

    // ==================== lambdas ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_TAU_PRIMARY_RELEASE = (stack, ctx) -> {
        if (ctx.getPlayer() == null || ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) != GunAnimationType.CYCLE.ordinal()) return;
        var player = ctx.getPlayer();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.fireTauRelease.get(), SoundSource.PLAYERS, 1F, 1F);
    };

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_TAU_SECONDARY_PRESS = (stack, ctx) -> {
        if (ctx.getPlayer() == null) return;
        if (ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) <= 0) return;
        ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimationType.SPINUP, ctx.configIndex);
    };

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_TAU_SECONDARY_RELEASE = (stack, ctx) -> {
        if (ctx.getPlayer() == null) return;
        int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

        if (timer >= 10 && ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) == GunAnimationType.SPINUP.ordinal()) {
            ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimationType.ALT_CYCLE, ctx.configIndex);
            int unitsUsed = 1 + Math.min(12, timer / 10);

            int index = ctx.configIndex;
            Receiver primary = ctx.config.getReceivers(stack)[0];

            Vec3 offset = primary.getProjectileOffset(stack);

            float damage = Lego.getStandardWearDamage(stack, ctx.config, index) * unitsUsed * 5;
            float spread = Lego.calcSpread(ctx, stack, primary, tau_uranium_charge, true, index, false);
            EntityBulletBeamBase beam = new EntityBulletBeamBase(ctx.entity, tau_uranium_charge, damage, spread, offset.z, offset.y, offset.x);
            ctx.entity.level().addFreshEntity(beam);

            ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + tau_uranium_charge.wear * unitsUsed, ctx.config.getDurability(stack)));
        } else {
            ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimationType.CYCLE_DRY, ctx.configIndex);
        }
    };

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_COILGUN =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
}
