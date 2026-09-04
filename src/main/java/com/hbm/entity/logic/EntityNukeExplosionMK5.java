package com.hbm.entity.logic;

import com.hbm.config.BombConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeRayBatched;
import com.hbm.interfaces.IExplosionRay;
import com.hbm.main.MainRegistry;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ported from CE's {@code com.hbm.entity.logic.EntityNukeExplosionMK5} (234 lines, read in full) -
 * the mk5 ray-based nuke explosion entity. This is the class {@code com.hbm.hazard.type.
 * HazardTypeUnstable} already imports and calls {@link #statFac}/{@link #setDetonator} on (a
 * pre-existing compile break independent of this pass - see that file and {@code docs/phase3/
 * melee_weapons.md}'s headline finding #5); the two {@code statFac}/{@code statFacNoRad} factory
 * signatures below match that file's 4 existing call sites exactly.
 * <p>
 * Only {@code BombConfig.explosionAlgorithm}'s "Legacy" ray-batched algorithm ({@link
 * ExplosionNukeRayBatched}) is wired up here - the fully-threaded, off-heap default algorithm
 * ({@code ExplosionNukeRayParallelized}) is explicitly deferred (see {@code
 * ExplosionNukeRayBatched}'s own javadoc and {@code docs/phase3/explosion_engine.md}'s "Deferred
 * scope"), so every mk5 detonation currently runs the single-threaded batched algorithm regardless
 * of the configured value.
 * <p>
 * <b>Not ported (documented forward references, each a real dependency this port doesn't have
 * yet)</b>: {@code AdvancementManager.grantAchievement} (achievements, Phase 5), {@code
 * EntityGlowingOne.convertInRadiusToGlow} (mob-conversion, not this phase's scope). {@code
 * SatelliteDetector.reportEvent} is Exact CE {@code EntityNukeExplosionMK5.java:121}. Each remaining stub has a comment
 * naming the real CE call it replaces; the core ray-based destruction, AoE damage, and
 * radiation-along-line-of-sight logic are all fully ported and functional. The {@code
 * EntityFalloutRain} spawn on completion ({@code docs/phase3/explosion_engine.md}'s "Fallout
 * trigger hook") is now wired - see {@code docs/phase4/fallout_rain_and_effects.md}.
 */
public class EntityNukeExplosionMK5 extends EntityExplosionChunkloading {

    /** Strength of the blast (ray resistance budget). */
    private int strength;
    /** How far rays are cast (max radius). */
    private int radius;

    private boolean fallout = true;
    private IExplosionRay explosion;
    private boolean initialized = false;
    private int falloutAdd = 0;
    private int algorithm;
    private long explosionStart = 0;

    public UUID detonator;

    public EntityNukeExplosionMK5(EntityType<? extends EntityNukeExplosionMK5> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Matches {@code HazardTypeUnstable}'s 4 existing call sites exactly: {@code
     * EntityNukeExplosionMK5.statFac(world, radius, x, y, z).setDetonator(entity)}.
     */
    public static EntityNukeExplosionMK5 statFac(Level level, int r, double x, double y, double z) {
        if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get() && !level.isClientSide()) {
            MainRegistry.logger.info("[NUKE] Initialized explosion at {} / {} / {} with radius {}!", x, y, z, r);
        }

        if (r == 0) r = 25;

        EntityNukeExplosionMK5 mk5 = new EntityNukeExplosionMK5(NukeEntityTypes.NUKE_MK5.get(), level);

        mk5.strength = 2 * r;
        mk5.radius = r;
        mk5.algorithm = BombConfig.EXPLOSION_ALGORITHM.get();

        mk5.setPos(x, y, z);
        if (BombConfig.DISABLE_NUCLEAR.get()) mk5.fallout = false;
        return mk5;
    }

    /** "Small, ambient" nuclear effects (e.g. {@code ExplosionLarge.explodeFire}/{@code buster}) opt out of the fallout-rain hook via this. */
    public static EntityNukeExplosionMK5 statFacNoRad(Level level, int r, double x, double y, double z) {
        EntityNukeExplosionMK5 mk5 = statFac(level, r, x, y, z);
        mk5.fallout = false;
        return mk5;
    }

    @Override
    public void tick() {
        // CE's onUpdate() does not call super.onUpdate() here (unlike EntityNukeExplosionMK3/
        // EntityBalefire) - it instead calls requestChunkLoaderTicketIfNeeded() itself below,
        // preserved faithfully rather than "fixed" into consistency.
        Level level = level();
        if (level.isClientSide()) return;
        requestChunkLoaderTicketIfNeeded();

        if (strength == 0) {
            this.discard();
            return;
        }
        loadChunk(chunkPosition().x, chunkPosition().z);

        // TODO(AdvancementManager, Phase 5): CE grants achManhattan to every player in the level here.

        double r = this.radius * 2.0D;
        List<Entity> list = level.getEntitiesOfClass(Entity.class,
                new AABB(getX() - r, getY() - r, getZ() - r, getX() + r, getY() + r, getZ() + r));

        if (fallout && explosion != null && this.tickCount < 10 && strength >= 75) {
            List<LivingEntity> livingList = new ArrayList<>(list.size());
            for (Entity e : list) {
                if (e instanceof LivingEntity living) livingList.add(living);
            }
            radiate(livingList, 2_500_000F / (this.tickCount * 5 + 1));
        }

        ExplosionNukeGeneric.dealDamage(level, list, getX(), getY(), getZ(), r);
        // TODO(EntityGlowingOne): CE converts mobs in radius to "Glowing One"s at tickExisted==42
        // while radiating, "until there is fallout rain" - EntityGlowingOne is not this phase's
        // scope.

        if (!initialized) {
            explosionStart = System.currentTimeMillis();
            explosion = new ExplosionNukeRayBatched(level, (int) getX(), (int) getY(), (int) getZ(), strength, radius);
            explosion.setDetonator(detonator);
            SatelliteDetector.reportEvent(level, SatelliteDetector.DURATION_HIGH,
                    SatelliteDetector.BurstIntensity.HIGH, getX(), getZ());
            initialized = true;
        }

        if (!explosion.isComplete()) {
            explosion.update(BombConfig.MK5_BLAST_TIME.get());
        } else {
            if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get() && explosionStart != 0) {
                MainRegistry.logger.info("[NUKE] Explosion complete. Time elapsed: {}ms", System.currentTimeMillis() - explosionStart);
            }
            if (fallout) {
                // CE: no-arg EntityFalloutRain, positioned at this entity's own position, scaled to
                // (int)(radius * 2.5 + falloutAdd) * BombConfig.falloutRange / 100 - no detonator
                // propagated (real CE asymmetry vs. the MK3 "waste" path below, faithfully preserved;
                // see docs/phase4/fallout_rain_and_effects.md's Key design/API decisions).
                EntityFalloutRain rain = new EntityFalloutRain(level);
                rain.setPos(getX(), getY(), getZ());
                rain.setScale((int) (this.radius * 2.5 + falloutAdd) * BombConfig.FALLOUT_RANGE.get() / 100);
                level.addFreshEntity(rain);
            }
            this.discard();
        }
    }

    private void radiate(List<LivingEntity> entities, float rads) {
        Level level = level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (LivingEntity e : entities) {
            Vec3 vec = new Vec3(e.getX() - getX(), (e.getY() + e.getEyeHeight()) - getY(), e.getZ() - getZ());
            double len = vec.length();
            if (len <= 0.0001D) continue;
            vec = vec.normalize();

            double res = 0F;
            int steps = Mth.floor(len);
            for (int i = 1; i < steps; i++) {
                int ix = Mth.floor(getX() + vec.x * i);
                int iy = Mth.floor(getY() + vec.y * i);
                int iz = Mth.floor(getZ() + vec.z * i);
                float blockRes = level.getBlockState(pos.set(ix, iy, iz)).getBlock().getExplosionResistance();
                res += blockRes;
            }

            if (res < 1.0) res = 1.0;
            double eRads = rads;
            eRads /= res;
            eRads /= len * len;
            ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, eRads);
        }
    }

    public EntityNukeExplosionMK5 setDetonator(Entity detonator) {
        if (detonator instanceof ServerPlayer) {
            this.detonator = detonator.getUUID();
        }
        return this;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        markChunkLoaderRestoredFromNBT();
        radius = nbt.getInt("radius");
        strength = nbt.getInt("strength");
        falloutAdd = nbt.getInt("falloutAdd");
        fallout = nbt.getBoolean("fallout");
        algorithm = nbt.getInt("algorithm");
        if (nbt.hasUUID("detonator")) detonator = nbt.getUUID("detonator");
        if (!initialized) {
            explosion = new ExplosionNukeRayBatched(level(), (int) getX(), (int) getY(), (int) getZ(), strength, radius);
            explosion.setDetonator(this.detonator);
        }
        explosion.readEntityFromNBT(nbt);
        initialized = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("radius", radius);
        nbt.putInt("strength", strength);
        nbt.putInt("falloutAdd", falloutAdd);
        nbt.putBoolean("fallout", fallout);
        nbt.putInt("algorithm", algorithm);
        if (detonator != null) nbt.putUUID("detonator", detonator);
        if (explosion != null) explosion.writeEntityToNBT(nbt);
    }

    /**
     * CE overrides both {@code setDead()} and {@code onRemovedFromWorld()} with the identical
     * {@code explosion.cancel(); clearChunkLoader();} body - the latter fired on mere chunk-unload
     * in 1.12/Forge, distinct from an intentional despawn. 1.21.1's removal model doesn't expose
     * that same "unloaded, not despawned" hook the way 1.12 did, and this port's chunk-loading
     * itself is already a documented no-op stub (see {@link EntityExplosionChunkloading}), so both
     * CE hooks fold into this single {@link #remove} override.
     */
    @Override
    public void remove(RemovalReason reason) {
        if (explosion != null) explosion.cancel();
        clearChunkLoader();
        super.remove(reason);
    }

    public EntityNukeExplosionMK5 moreFallout(int fallout) {
        falloutAdd = fallout;
        return this;
    }

    /**
     * TODO: CE sets the 1.12 {@code Entity#forceSpawn} field here to bypass spawn-cancellation
     * checks; no confirmed 1.21.1 {@code Entity} equivalent exists. Kept as a no-op fluent method
     * so existing call sites (e.g. {@code ExplosionNukeSmall}) don't need to change shape.
     */
    public EntityNukeExplosionMK5 forceSpawn() {
        return this;
    }
}
