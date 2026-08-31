package com.hbm.explosion;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.particle.HbmEffect;
import com.hbm.particle.HbmParticleOptions;
import com.hbm.particle.ModParticleTypes;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Ported from CE's {@code com.hbm.explosion.ExplosionLarge} (279 lines, read in full) - per
 * {@code docs/phase3/missile_framework.md}'s "Explosion-engine call surface" section, {@link
 * #explode}/{@link #explodeFire}/{@link #buster} are the confirmed spawn sites for {@code
 * EntityMissileCustom}'s HE/INC/BUSTER warheads and {@code ItemUnstable}/{@code bomb_waffle}/
 * {@code memespoon}'s small ambient nuclear effects (the latter two via {@link
 * EntityNukeExplosionMK5#statFacNoRad}, which explicitly opts out of the fallout-rain hook - see
 * that method's own javadoc).
 * <p>
 * <b>Particle-broadcast helpers now wired</b> ({@link #spawnParticlesRadial}, {@link #spawnFoam},
 * {@link #spawnParticles}, {@link #spawnShock} - each a thin wrapper around CE's networked
 * {@code AuxParticlePacketNT}) - now dispatch through {@link com.hbm.particle.HbmEffect}, matching CE's
 * own count/strength/radius=250 values 1:1; see {@code docs/phase5/particle_engine_and_generic_vfx.md}.
 * {@link #spawnBurst} is wired too (Phase 5 {@code c14-custom-particle-content} task), via
 * {@link net.minecraft.server.level.ServerLevel#sendParticles} against the real registered
 * {@code com.hbm.particle.ModParticleTypes.GAS_FLAME} type instead of the generic
 * {@code HbmEffect}/{@code AuxParticlePacketNT} path - see that method's own javadoc.
 * <p>
 * <b>Not ported (documented forward references)</b>: every debris-entity spawner ({@link #spawnRubble},
 * {@link #spawnShrapnels}, {@link #spawnTracers}, {@link #spawnShrapnelShower}) depends on
 * {@code EntityRubble}/{@code EntityShrapnel}, neither of which exists in this port yet (named as
 * Phase-3 prerequisites in {@code docs/phase1/items_special.md}/{@code items_food_gear.md}'s Deferred
 * sections, not this pass's to add). Kept as real methods with a documented no-op body (not silently
 * deleted) so every call site elsewhere in the mod - including {@link #explode}/{@link #explodeFire}/
 * {@link #buster} themselves, which call several of them internally - keeps compiling and running
 * (minus the debris) rather than needing to be rewritten once those dependencies land.
 * {@link #spawnMissileDebris} keeps its real item-drop logic (needs only
 * {@link net.minecraft.world.entity.item.ItemEntity}, which does exist) since it is not one of those
 * two blocked families. {@link #jolt} keeps its real block-removal loop (the actual "digs a tunnel"
 * gameplay effect) but stubs only the cosmetic {@code EntityRubble} spawn inside it, for the same
 * reason.
 * <p>
 * CE's {@code isWarDim} gates on {@link #jolt}/{@link #explodeFire}/{@link #buster} are dropped per
 * this port's documented always-true default.
 */
public final class ExplosionLarge {

    private ExplosionLarge() {
    }

    private static final Random RAND = new Random();

    // --- VFX (Phase 5 - documented no-ops, see class javadoc) -----------------------------------

    public static void spawnParticlesRadial(Level level, double x, double y, double z, int count) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        HbmEffect.sendPacket(level, HbmEffect.SMOKE_RADIAL, x, y, z, 250, data);
    }

    public static void spawnFoam(Level level, double x, double y, double z, int count) {
        // CE's own real handler for Smoke_FoamSplash is itself dead code (see HbmEffect.SMOKE_FOAM_SPLASH's
        // own javadoc) - the packet is still sent, matching CE's real call site 1:1, even though it
        // visibly does nothing on either a real CE client or this port's client.
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        HbmEffect.sendPacket(level, HbmEffect.SMOKE_FOAM_SPLASH, x, y, z, 250, data);
    }

    public static void spawnParticles(Level level, double x, double y, double z, int count) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        HbmEffect.sendPacket(level, HbmEffect.SMOKE_CLOUD, x, y, z, 250, data);
    }

    /**
     * Wired by the {@code c14-custom-particle-content} Phase 5 task. CE's real call site here
     * ({@code upstream/hbm-ce/.../explosion/ExplosionLarge.java:53-63}) is
     * {@code ParticleUtil.spawnGasFlame(world, x, y, z, vec.x, 0.0, vec.z)} in a loop, rotating an
     * evenly-spaced {@code strength}-length vector around Y - {@code count} times. A closer read of
     * {@code ParticleUtil.spawnGasFlame} (not fully traced by this method's earlier javadoc pass) shows
     * it DOES route through {@code MainRegistry.proxy.effectNT(HbmEffectNT.GasFlame, ...)}/
     * {@code AuxParticlePacketNT} when not already client-side - it is a named {@code HbmEffectNT}
     * broadcast after all, not a bypass of that system as this class's own javadoc previously assumed
     * (flagged here as a correction, not silently fixed elsewhere). Rather than adding a redundant
     * {@code GasFlame} constant to {@code com.hbm.particle.HbmEffect} (that enum's own dispatch table is
     * a sibling task's scope, not this task's to extend), this method uses the equivalent, simpler
     * "free transport" bridge {@code docs/phase5/particle_engine_and_generic_vfx.md}'s own
     * "Recommended architecture" point 3 names: {@link ModParticleTypes#GAS_FLAME} is already a real
     * registered vanilla {@link net.minecraft.core.particles.ParticleType}, so
     * {@link ServerLevel#sendParticles} gets it to nearby clients for free via the vanilla
     * {@link net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket} path, with no new
     * packet/dispatch-table entry needed.
     */
    public static void spawnBurst(Level level, double x, double y, double z, int count, double strength) {
        if (!(level instanceof ServerLevel serverLevel) || count <= 0) return;

        Vec3 vec = new Vec3(strength, 0, 0).yRot((float) Math.toRadians(RAND.nextInt(360)));
        float stepRadians = (float) Math.toRadians(360.0 / count);

        for (int i = 0; i < count; i++) {
            CompoundTag data = new CompoundTag();
            data.putDouble("mX", vec.x);
            data.putDouble("mY", 0.0D);
            data.putDouble("mZ", vec.z);
            // CE: ParticleGasFlame's own scale > 0 ? scale : 6.5F fallback (HbmEffectNT.java:471) - no
            // explicit scale is set at this call site, so 0 here resolves to CE's real 6.5F default.
            data.putFloat("scale", 0F);

            HbmParticleOptions options = new HbmParticleOptions(ModParticleTypes.GAS_FLAME.get(), data);
            serverLevel.sendParticles(options, x, y, z, 1, 0, 0, 0, 0);

            vec = vec.yRot(stepRadians);
        }
    }

    public static void spawnShock(Level level, double x, double y, double z, int count, double strength) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        data.putDouble("strength", strength);
        HbmEffect.sendPacket(level, HbmEffect.SMOKE_SHOCK, x, y + 0.5, z, 250, data);
    }

    // --- debris entities (blocked on EntityRubble/EntityShrapnel, see class javadoc) ------------

    public static void spawnRubble(Level level, double x, double y, double z, int count) {
        // TODO(EntityRubble): see class javadoc.
    }

    public static void spawnShrapnels(Level level, double x, double y, double z, int count) {
        // TODO(EntityShrapnel): see class javadoc.
    }

    public static void spawnTracers(Level level, double x, double y, double z, int count) {
        // TODO(EntityShrapnel): see class javadoc.
    }

    public static void spawnShrapnelShower(Level level, double x, double y, double z, double motionX, double motionY, double motionZ, int count, double deviation) {
        // TODO(EntityShrapnel): see class javadoc.
    }

    public static void spawnMissileDebris(Level level, double x, double y, double z, double motionX, double motionY, double motionZ, double deviation, List<ItemStack> debris, ItemStack rareDrop) {
        if (debris != null) {
            for (ItemStack itemStack : debris) {
                if (itemStack != null && !itemStack.isEmpty()) {
                    int k = RAND.nextInt(itemStack.getCount() + 1);
                    for (int j = 0; j < k; j++) {
                        ItemStack copy = itemStack.copy();
                        copy.setCount(1);
                        spawnDebrisItem(level, x, y, z, motionX, motionY, motionZ, deviation, copy);
                    }
                }
            }
        }

        if (rareDrop != null && RAND.nextInt(10) == 0) {
            spawnDebrisItem(level, x, y, z, motionX, motionY, motionZ, deviation * 0.1, rareDrop.copy());
        }
    }

    private static void spawnDebrisItem(Level level, double x, double y, double z, double motionX, double motionY, double motionZ, double deviation, ItemStack stack) {
        double mx = (motionX + RAND.nextGaussian() * deviation) * 0.85;
        double my = (motionY + RAND.nextGaussian() * deviation) * 0.85;
        double mz = (motionZ + RAND.nextGaussian() * deviation) * 0.85;

        net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(
                level, x + mx * 2, y + my * 2, z + mz * 2, stack);
        item.setDeltaMovement(mx, my, mz);
        level.addFreshEntity(item);
    }

    // --- real block/entity mutation ----------------------------------------------------------------

    public static void jolt(Level level, Entity detonator, double posX, double posY, double posZ, double strength, int count, double vel) {
        for (int j = 0; j < count; j++) {
            double phi = RAND.nextDouble() * (Math.PI * 2);
            double costheta = RAND.nextDouble() * 2 - 1;
            double theta = Math.acos(costheta);
            Vec3 vec = new Vec3(Math.sin(theta) * Math.cos(phi), Math.sin(theta) * Math.sin(phi), Math.cos(theta));
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int i = 0; i < strength; i++) {
                double x0 = posX + (vec.x * i);
                double y0 = posY + (vec.y * i);
                double z0 = posZ + (vec.z * i);
                pos.set((int) x0, (int) y0, (int) z0);

                if (!level.isClientSide()) {
                    var blockState = level.getBlockState(pos);
                    var block = blockState.getBlock();
                    if (!blockState.getFluidState().isEmpty()) {
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    }

                    if (!blockState.is(net.minecraft.world.level.block.Blocks.AIR)) {
                        if (block.getExplosionResistance() > 70) continue;

                        // TODO(CompatDynamicTrees): tree-destruction compat hook not ported (no consumer mod present).
                        // TODO(EntityRubble): see class javadoc - the debris-chunk visual is skipped, but the block
                        // is still removed below, preserving jolt's real "digs a tunnel" gameplay effect.

                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        break;
                    }
                }
            }
        }
    }

    public static void explode(Level level, Entity detonator, double x, double y, double z, float strength, boolean cloud, boolean rubble, boolean shrapnel) {
        // CE: world.createExplosion(detonator, x, y, z, strength, true) - the boolean is "causes fire".
        level.explode(detonator, x, y, z, strength, true, Level.ExplosionInteraction.TNT);
        if (cloud) spawnParticles(level, x, y + 2, z, cloudFunction((int) strength));
        if (rubble) spawnRubble(level, x, y + 2, z, rubbleFunction((int) strength));
        if (shrapnel) spawnShrapnels(level, x, y + 2, z, shrapnelFunction((int) strength));
    }

    public static int cloudFunction(int i) {
        return (int) (545 * (1 - Math.pow(Math.E, -i / 15.0)) + 15);
    }

    public static int rubbleFunction(int i) {
        return i / 10;
    }

    public static int shrapnelFunction(int i) {
        return i / 3;
    }

    public static void explodeFire(Level level, Entity detonator, double x, double y, double z, float strength, boolean cloud, boolean rubble, boolean shrapnel) {
        level.addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(level, (int) strength, x, y, z).setDetonator(detonator));
        ContaminationUtil.radiate(level, x, y, z, strength, 0, 0, strength * 20F, strength * 5F);

        if (cloud) spawnParticles(level, x, y + 2, z, cloudFunction((int) strength));
        if (rubble) spawnRubble(level, x, y + 2, z, rubbleFunction((int) strength));
        if (shrapnel) spawnShrapnels(level, x, y + 2, z, shrapnelFunction((int) strength));
    }

    public static void buster(Level level, Entity detonator, double x, double y, double z, Vec3 vector, float strength, float depth) {
        vector = vector.normalize();
        for (int i = 0; i <= depth; i += 3) {
            double ix = x + vector.x * i;
            double iy = y + vector.y * i;
            double iz = z + vector.z * i;
            ContaminationUtil.radiate(level, ix, iy, iz, strength, 0, 0, 0, strength * 10F);
            level.addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(level, (int) strength, ix, iy, iz).setDetonator(detonator));
        }
        spawnParticles(level, x, y + 2, z, cloudFunction((int) strength));
        spawnRubble(level, x, y + 2, z, rubbleFunction((int) strength));
        spawnShrapnels(level, x, y + 2, z, shrapnelFunction((int) strength));
    }
}
