package com.hbm.items.weapon.sedna.content;

import com.hbm.blocks.generic.BlockLayering;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
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
import com.hbm.main.MainRegistry;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryTool} - the fire extinguisher and
 * grapple/mortar charge-thrower. See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryTool}
 * table.
 * <p>
 * Extinguisher ricochet is Exact CE {@code XFactoryTool.java:59-211}: water clears 3×3×3 fire/foam
 * then {@code setDead}; foam/sand stack {@code foam_layer}/{@code sand_boron_layer} (layers &lt; 6
 * increment, else {@code block_foam}/{@code sand_boron}). Entity {@link LivingEntity#clearFire()}
 * stays. {@code IRepairable}/{@code CompatExternal} and volcanic-lava {@code onUpdate} stay skipped
 * (not registered). Client dust VFX skipped.
 * <p>
 * Mortar-charge slag litter is Exact CE {@code XFactoryTool.java:256-266}
 * {@code BlockMutatorDebris(block_slag)} — {@code block_slag} is registered (single-state; CE meta 1
 * is moot). {@code ExplosionCreator.composeEffectSmall} stays skipped (VFX).
 * <p>
 * <b>Forward references (documented, not silently dropped):</b>
 * <ul>
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

    public static Item ITEM_FEXT_WATER;
    public static Item ITEM_FEXT_FOAM;
    public static Item ITEM_FEXT_SAND;

    public static final BulletConfig fext_water = new BulletConfig("fext_water").setItem(() -> ITEM_FEXT_WATER)
            .setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04).setSpread(0.025F)
            .setOnEntityHit(XFactoryTool::extinguishHit).setOnRicochet(XFactoryTool::waterHit);
    public static final BulletConfig fext_foam = new BulletConfig("fext_foam").setItem(() -> ITEM_FEXT_FOAM)
            .setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04).setSpread(0.05F)
            .setOnEntityHit(XFactoryTool::extinguishHit).setOnRicochet(XFactoryTool::foamHit);
    public static final BulletConfig fext_sand = new BulletConfig("fext_sand").setItem(() -> ITEM_FEXT_SAND)
            .setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04).setSpread(0.05F)
            .setOnEntityHit(XFactoryTool::extinguishHit).setOnRicochet(XFactoryTool::sandHit);

    // ==================== charge-thrower ammo ====================

    public static Item ITEM_CT_HOOK;
    public static Item ITEM_CT_MORTAR;
    public static Item ITEM_CT_MORTAR_CHARGE;

    public static final BulletConfig ct_hook = new BulletConfig("ct_hook").setItem(() -> ITEM_CT_HOOK)
            .setRenderRotations(false).setLife(6_000).setVel(3F).setGrav(0.035).setDoesPenetrate(true).setDamageFalloffByPen(false)
            .setOnImpact(XFactoryTool::hookImpact);
    public static final BulletConfig ct_mortar = new BulletConfig("ct_mortar").setItem(() -> ITEM_CT_MORTAR)
            .setDamage(2.5F).setLife(200).setVel(3F).setGrav(0.035).setOnImpact(XFactoryTool::mortarImpact);
    public static final BulletConfig ct_mortar_charge = new BulletConfig("ct_mortar_charge").setItem(() -> ITEM_CT_MORTAR_CHARGE)
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

    /** Exact CE {@code XFactoryTool.java:59-89} {@code LAMBDA_WATER_HIT}. IRepairable skipped. */
    private static void waterHit(EntityBulletBaseMK4 bullet, BlockHitResult mop) {
        if (bullet.level().isClientSide) return;
        Level world = bullet.level();
        BlockPos base = mop.getBlockPos();
        Block foamLayer = hbm("foam_layer");
        Block blockFoam = hbm("block_foam");
        boolean fizz = false;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    BlockPos p = base.offset(i, j, k);
                    Block block = world.getBlockState(p).getBlock();
                    if (block == Blocks.FIRE || block == foamLayer || block == blockFoam) {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        fizz = true;
                    }
                }
            }
        }
        if (fizz) playFizz(world, bullet);
        bullet.discard();
    }

    /** Exact CE {@code XFactoryTool.java:113-162} {@code LAMBDA_FOAM_HIT}. IRepairable skipped. */
    private static void foamHit(EntityBulletBaseMK4 bullet, BlockHitResult mop) {
        if (bullet.level().isClientSide) return;
        Level world = bullet.level();
        BlockPos base = mop.getBlockPos();
        boolean fizz = clearFireCube(world, base);
        if (world.random.nextBoolean()) base = base.relative(mop.getDirection());
        stackLayer(world, base, hbm("foam_layer"), hbm("block_foam"), false);
        if (fizz) playFizz(world, bullet);
    }

    /** Exact CE {@code XFactoryTool.java:175-211} {@code LAMBDA_SAND_HIT}. IRepairable skipped. */
    private static void sandHit(EntityBulletBaseMK4 bullet, BlockHitResult mop) {
        if (bullet.level().isClientSide) return;
        Level world = bullet.level();
        BlockPos pos = mop.getBlockPos();
        if (world.random.nextBoolean()) pos = pos.relative(mop.getDirection());
        BlockState state = world.getBlockState(pos);
        Block layer = hbm("sand_boron_layer");
        if ((ceReplaceable(state) || state.getBlock() == layer)
                && layer.defaultBlockState().canSurvive(world, pos)) {
            stackLayer(world, pos, layer, hbm("sand_boron"), true);
            if (state.getBlock() instanceof FireBlock) playFizz(world, bullet);
        }
    }

    private static boolean clearFireCube(Level world, BlockPos base) {
        boolean fizz = false;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    BlockPos p = base.offset(i, j, k);
                    if (world.getBlockState(p).getBlock() instanceof FireBlock) {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        fizz = true;
                    }
                }
            }
        }
        return fizz;
    }

    /** CE foam/sand: replaceable + {@code canPlaceBlockAt} → new layer / increment meta&lt;6 / solid. */
    private static void stackLayer(Level world, BlockPos pos, Block layer, Block solid, boolean sandAlreadyChecked) {
        BlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        if (!sandAlreadyChecked) {
            if (!(ceReplaceable(state) && layer.defaultBlockState().canSurvive(world, pos))) return;
        }
        if (b != layer) {
            world.setBlock(pos, layer.defaultBlockState(), 3);
            return;
        }
        int meta = state.getValue(BlockLayering.LAYERS);
        if (meta < 6) {
            world.setBlock(pos, state.setValue(BlockLayering.LAYERS, meta + 1), 3);
        } else {
            world.setBlock(pos, solid.defaultBlockState(), 3);
        }
    }

    /** Exact CE {@code BlockLayering.isReplaceable}: layers &lt; 7, else vanilla {@code canBeReplaced}. */
    private static boolean ceReplaceable(BlockState state) {
        if (state.getBlock() instanceof BlockLayering && state.hasProperty(BlockLayering.LAYERS)) {
            return state.getValue(BlockLayering.LAYERS) < 7;
        }
        return state.canBeReplaced() || state.isAir();
    }

    private static void playFizz(Level world, EntityBulletBaseMK4 bullet) {
        world.playSound(null, bullet.getX(), bullet.getY(), bullet.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.5F + world.random.nextFloat() * 0.5F);
    }

    private static Block hbm(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
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
        vnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop()
                .withBlockEffect(new BlockMutatorDebris(hbm("block_slag"))));
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage).setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        bullet.discard();
    }
}
