package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.ItemMissileStandard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileBaseNT} (420 lines, read in full) -
 * the abstract target-seeking flight-physics base every standard/custom missile entity except
 * {@code EntityMissileAntiBallistic}/{@code EntityMIRV} extends.
 * <p>
 * <b>Base class - a deliberate, documented departure from the task brief's literal instruction.</b>
 * The task brief (written against {@code docs/phase3/missile_framework.md}, whose own research
 * pass found {@code com.hbm.entity.projectile} entirely absent from this port) says to extend
 * vanilla {@link net.minecraft.world.entity.projectile.Projectile} directly and hand-roll the
 * physics. Since that report was written, {@link EntityThrowableInterp}/{@code EntityThrowableNT}
 * (the gun-framework package's own ballistics base, read in full before this decision) landed in
 * this same wave and are now real, confirmed-compiling classes - restoring CE's actual inheritance
 * shape ({@code EntityMissileBaseNT extends EntityThrowableInterp}). Extending that base instead of
 * vanilla {@code Projectile} directly is strictly more faithful to CE (same chain CE itself uses)
 * and needs far less hand-rolled code: {@code EntityThrowableNT.tick()} already provides the
 * block-clip-and-dispatch loop CE's own {@code EntityThrowableNT} (the class CE's real base extends)
 * provides, and its {@link EntityThrowableInterp#tick()}/{@code motionMult()}/{@code
 * getGravityVelocity()}/{@code getAirDrag()}/{@code getWaterDrag()} override points line up exactly
 * with what CE's subclass already overrides (see below). This decision is called out explicitly per
 * this project's "no silent deviation" rule rather than followed as a rubber-stamp.
 * <p>
 * <b>Chunk loading</b>: CE's {@code ForgeChunkManager}-ticket scaffold is dropped entirely in favor
 * of implementing the already-ported {@link IChunkLoader} (single-chunk, refreshed via {@link
 * #tick()} calling {@link IChunkLoader#updateChunkTicket}), per the task brief.
 * <p>
 * <b>Guidance model - preserved exactly.</b> {@code accelXZ}/{@code decelY} are both
 * {@code 1/distance-to-target} (decelY doubled), and every tick nudges the current delta-movement
 * toward/away from the target XZ vector depending on the sign of the current vertical speed (rising
 * = accelerate toward target, falling = decelerate away) - a simple parabolic-arc approximation,
 * not real ballistics. This runs in {@link #tick()} *after* the physics-integration
 * {@code super.tick()} call, exactly matching CE's own ordering (CE calls
 * {@code super.onUpdate()} - the actual position/motion integration - before adjusting
 * {@code motionX/Y/Z} for the *next* tick).
 * <p>
 * <b>Not ported (documented, zero gameplay effect)</b>: {@code spawnContrail}/
 * {@code spawnControlWithOffset}/{@code getContrailScale} - CE's per-tick contrail particle spawn,
 * entirely client-side cosmetic VFX with no gameplay effect (confirmed by reading the class in
 * full), gone via the same networked-particle-packet infrastructure {@link ExplosionLarge}'s own
 * javadoc already documents as absent from this port (Phase 5 scope). The 1.12
 * {@code EntityTrackerEntry.encodedRotationYaw += 100} nudge in CE's {@code onUpdate} (a manual
 * workaround for 1.12's coarser rotation-sync packet resolution) is dropped outright, not stubbed -
 * 1.21's entity tracking already syncs rotation at full precision, the workaround solves a problem
 * this version of the game does not have (same reasoning {@link EntityThrowableInterp}'s own javadoc
 * gives for dropping CE's manual {@code turnProgress} interpolation).
 * <p>
 * <b>Simplification, documented</b>: CE's every concrete subclass repeats a second constructor
 * shape ({@code (World, float x, float y, float z, int targetX, int targetZ)}) purely so launch
 * infrastructure ({@code TileEntityLaunchTable}/{@code TileEntityCompactLauncher}, explicitly out of
 * this pass's scope) can spawn a targeted missile in one call. Since no in-scope caller needs that
 * exact two-constructor shape, this port collapses it to one constructor per concrete class (the
 * registry {@code (EntityType, Level)} shape) plus {@link #initTrajectory}, called by whichever code
 * spawns a targeted missile (see {@link EntityMissileCustom#spawn} for the pattern).
 */
public abstract class EntityMissileBaseNT extends EntityThrowableInterp implements IChunkLoader, IRadarDetectableNT {

    public int startX;
    public int startZ;
    public int targetX;
    public int targetZ;
    public double velocity;
    public double decelY;
    public double accelXZ;
    public boolean isCluster = false;
    public int health = 50;

    private ChunkPos loadedChunkPos = new ChunkPos(0, 0);

    protected EntityMissileBaseNT(EntityType<? extends EntityMissileBaseNT> type, Level level) {
        super(type, level);
        this.startX = (int) getX();
        this.startZ = (int) getZ();
        this.targetX = (int) getX();
        this.targetZ = (int) getZ();
    }

    /**
     * CE: {@code EntityMissileBaseNT(World, float x, float y, float z, int a, int b)}. Called once,
     * right after construction, by whichever code spawns a targeted missile (see class javadoc's
     * "Simplification" note).
     */
    public void initTrajectory(double x, double y, double z, int targetX, int targetZ) {
        this.setPos(x, y, z);
        this.startX = (int) x;
        this.startZ = (int) z;
        this.targetX = targetX;
        this.targetZ = targetZ;
        this.setDeltaMovement(0, 2, 0);

        Vec3 vector = new Vec3(targetX - startX, 0, targetZ - startZ);
        double len = vector.length();
        this.accelXZ = this.decelY = len == 0 ? 0 : 1 / len;
        this.decelY *= 2;
        this.velocity = 0;

        this.setYRot((float) (Math.atan2(targetX - getX(), targetZ - getZ()) * 180.0D / Math.PI));
    }

    /** Auto-generates radar blip level and all that from the item. */
    public abstract ItemStack getMissileItemForInfo();

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanMissiles;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return !params.smartMode || !(this.getDeltaMovement().y >= 0);
    }

    @Override
    protected double motionMult() {
        return velocity;
    }

    @Override
    public boolean doesImpactEntities() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.health <= 0) { // check it hasn't been blown up
            this.killMissile();
            return;
        }

        if (velocity < 4) {
            velocity += Mth.clamp(this.tickCount / 60D * 0.05D, 0, 0.05);
        }

        if (!level().isClientSide()) {
            Vec3 motion = getDeltaMovement();
            double mx = motion.x;
            double my = motion.y;
            double mz = motion.z;

            if (hasPropulsion()) {
                my -= decelY * velocity;

                Vec3 vector = new Vec3(targetX - startX, 0, targetZ - startZ).normalize();
                double vx = vector.x * accelXZ;
                double vz = vector.z * accelXZ;

                if (my > 0) {
                    mx += vx * velocity;
                    mz += vz * velocity;
                }
                if (my < 0) {
                    mx -= vx * velocity;
                    mz -= vz * velocity;
                }
            } else {
                mx *= 0.99;
                mz *= 0.99;
                if (my > -1.5) my -= 0.05;
            }

            setDeltaMovement(mx, my, mz);

            if (my < -1.5 && this.isCluster) {
                cluster();
                this.discard();
                return;
            }

            this.setYRot((float) (Math.atan2(targetX - getX(), targetZ - getZ()) * 180.0D / Math.PI));
            float f2 = Mth.sqrt((float) (mx * mx + mz * mz));
            this.setXRot((float) (Math.atan2(my, f2) * 180.0D / Math.PI) - 90);

            updateChunkTicket(this);
        } else {
            this.spawnContrail();
        }
    }

    public boolean hasPropulsion() {
        return true;
    }

    /** VFX-only, see class javadoc - no gameplay effect, not ported (Phase 5). */
    protected void spawnContrail() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        setDeltaMovement(nbt.getDouble("moX"), nbt.getDouble("moY"), nbt.getDouble("moZ"));
        setPos(nbt.getDouble("poX"), nbt.getDouble("poY"), nbt.getDouble("poZ"));
        decelY = nbt.getDouble("decel");
        accelXZ = nbt.getDouble("accel");
        targetX = nbt.getInt("tX");
        targetZ = nbt.getInt("tZ");
        startX = nbt.getInt("sX");
        startZ = nbt.getInt("sZ");
        velocity = nbt.getDouble("veloc");
        // CE does not persist `health`/`isCluster` here either - preserved exactly (a reloaded
        // missile resets to health=50/isCluster=false, a real CE gap, not one this port should fix).
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        Vec3 motion = getDeltaMovement();
        nbt.putDouble("moX", motion.x);
        nbt.putDouble("moY", motion.y);
        nbt.putDouble("moZ", motion.z);
        nbt.putDouble("poX", getX());
        nbt.putDouble("poY", getY());
        nbt.putDouble("poZ", getZ());
        nbt.putDouble("decel", decelY);
        nbt.putDouble("accel", accelXZ);
        nbt.putInt("tX", targetX);
        nbt.putInt("tZ", targetZ);
        nbt.putInt("sX", startX);
        nbt.putInt("sZ", startZ);
        nbt.putDouble("veloc", velocity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;

        if (this.health > 0 && !level().isClientSide()) {
            health -= amount;
            if (this.health <= 0) {
                this.killMissile();
            }
        }

        return true;
    }

    protected void killMissile() {
        if (!this.isRemoved()) {
            this.discard();
            Level level = level();
            Entity detonator = getOwner();
            ExplosionLarge.explode(level, detonator, getX(), getY(), getZ(), 5, true, false, true);
            Vec3 motion = getDeltaMovement();
            ExplosionLarge.spawnShrapnelShower(level, getX(), getY(), getZ(), motion.x, motion.y, motion.z, 15, 0.075);
            ExplosionLarge.spawnMissileDebris(level, getX(), getY(), getZ(), motion.x, motion.y, motion.z, 0.25, getDebris(), getDebrisRareDrop());
        }
    }

    @Override
    protected void onImpact(HitResult result) {
        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            this.onMissileImpact(result);
            this.discard();
        }
    }

    public abstract void onMissileImpact(HitResult mop);

    /** Nullable, matching CE exactly - {@code null} is the valid "no debris" sentinel {@link ExplosionLarge#spawnMissileDebris} checks for, not {@link ItemStack#EMPTY}. */
    @Nullable
    public abstract List<ItemStack> getDebris();

    /** Nullable, matching CE exactly - see {@link #getDebris()}'s javadoc for why {@code null}, not {@link ItemStack#EMPTY}, is the correct "no rare drop" sentinel here. */
    @Nullable
    public abstract ItemStack getDebrisRareDrop();

    public void cluster() {
    }

    @Override
    public double getGravityVelocity() {
        return 0.0D;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected float getWaterDrag() {
        return 1F;
    }

    /** CE: {@code isInRangeToRenderDist(distance) { return true; }} - always render regardless of distance. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    // --- IChunkLoader -----------------------------------------------------------------------------

    @Override
    public void setLoadedChunkPos(ChunkPos pos) {
        this.loadedChunkPos = pos;
    }

    @Override
    public ChunkPos getLoadedChunkPos() {
        return this.loadedChunkPos;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        this.onAddedToLevel((Entity) this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        this.onRemovedFromLevel((Entity) this);
    }

    /**
     * CE: {@code explodeStandard(strength, resolution, fire)}, a convenience wrapper around the
     * {@link ExplosionVNT} engine matching CE's own body exactly (block allocator/processor/entity
     * processor/player processor combination).
     */
    public void explodeStandard(float strength, int resolution, boolean fire) {
        ExplosionVNT xnt = new ExplosionVNT(level(), getX(), getY(), getZ(), strength);
        xnt.setBlockAllocator(new BlockAllocatorStandard(resolution));
        xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(fire ? new BlockMutatorFire() : null));
        xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(2));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
    }

    @Override
    public String getTranslationKey() {
        ItemStack item = this.getMissileItemForInfo();
        if (item != null && item.getItem() instanceof ItemMissileStandard missile) {
            return switch (missile.tier) {
                case TIER0 -> "radar.target.tier0";
                case TIER1 -> "radar.target.tier1";
                case TIER2 -> "radar.target.tier2";
                case TIER3 -> "radar.target.tier3";
                case TIER4 -> "radar.target.tier4";
            };
        }

        return "Unknown";
    }

    @Override
    public int getBlipLevel() {
        ItemStack item = this.getMissileItemForInfo();
        if (item != null && item.getItem() instanceof ItemMissileStandard missile) {
            return switch (missile.tier) {
                case TIER0 -> IRadarDetectableNT.TIER0;
                case TIER1 -> IRadarDetectableNT.TIER1;
                case TIER2 -> IRadarDetectableNT.TIER2;
                case TIER3 -> IRadarDetectableNT.TIER3;
                case TIER4 -> IRadarDetectableNT.TIER4;
            };
        }

        return IRadarDetectableNT.SPECIAL;
    }
}
