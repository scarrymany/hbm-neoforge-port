package com.hbm.entity.logic;

import com.hbm.damage.ModDamageTypes;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.entity.logic.EntityPlaneBase} (241 lines, read in full) - the abstract
 * base every scripted (non-rideable) NPC aircraft ({@link EntityC130}, {@link EntityBomber}) extends,
 * per {@code docs/phase4/entities_vehicles_aircraft.md}'s aircraft section and
 * {@code docs/phase4/entities_orbital_and_beam_payloads.md}'s independently-confirmed cross-check of
 * this exact class (both reports agree, no discrepancy).
 * <p>
 * <b>No gravity, no drag, no {@code travel()} override while healthy</b> - CE's own {@code onUpdate()}
 * is literally {@code setPosition(posX + motionX, posY + motionY, posZ + motionZ)} once a tick, with
 * {@code motionY} forced to exactly {@code 0} every tick while {@code health > 0}: a plane flies dead
 * level forever on whatever course a subclass's spawn-factory method set, with no altitude AI, no
 * waypoints, no steering. Only once {@code health <= 0} does {@code motionY -= 0.025} kick in (a slow,
 * constant-acceleration nose-down), crashing into an {@link ExplosionVNT}-based blast the instant the
 * plane either hits a non-air block or its Y drops below 0.
 * <p>
 * <b>Chunk loading</b>: CE force-loads its current chunk unconditionally every tick via
 * {@code loadNeighboringChunks} (never gated on having actually crossed a chunk boundary). This port
 * uses the already-real {@link IChunkLoader#updateChunkTicket} diff-and-swap logic instead (force the
 * new chunk, unforce the old one, only when the entity's chunk has actually changed) - a real,
 * deliberate tightening the task brief calls for, not a behavior change to anything a player can
 * observe (the plane still always has its current chunk force-loaded).
 * <p>
 * <b>Client interpolation</b>: CE hand-rolls a {@code turnProgress}/{@code syncPosX/Y/Z}/
 * {@code syncYaw/Pitch} countdown lerp. This port uses vanilla {@link Entity}'s own real
 * {@code lerpTo}/{@code lerpTargetX/Y/Z}/{@code lerpTargetXRot/YRot}/
 * {@code lerpPositionAndRotationStep} hooks instead (API shape confirmed via Neo Edition's own
 * {@code PlaneBase.java}, which independently converged on the identical vanilla mechanism; behavior
 * itself is CE's, not Neo Edition's - see the report's Key design decisions) - the same substitution
 * this port's own {@link com.hbm.entity.projectile.EntityThrowableInterp}/
 * {@link com.hbm.entity.item.EntityMovingConveyorObject} already make for the identical CE pattern.
 * <p>
 * <b>Not ported</b>: CE's {@code ExplosionSmallCreator.composeEffect} particle burst in
 * {@link #killPlane()} - purely decorative client VFX with no gameplay effect (Phase 5 scope, matching
 * this port's established deferred-particle precedent); the crash sound it accompanies is kept.
 */
public abstract class EntityPlaneBase extends Entity implements IChunkLoader {

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(EntityPlaneBase.class, EntityDataSerializers.FLOAT);

    public float health = getMaxHealth();
    public int timer = getLifetime();

    // Non-null default (never (chunk 0,0) in practice by the time it matters) rather than a
    // @Nullable field - matches EntityMissileBaseNT's own identical defensive precedent, avoiding an
    // NPE in IChunkLoader#updateChunkTicket's `oldPos.x` read should tick() ever run a single frame
    // before onAddedToLevel() has set the real value.
    private ChunkPos loadedChunkPos = new ChunkPos(0, 0);

    /** Vanilla lerp-target fields - see class javadoc's client-interpolation note. */
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;
    private float lerpXRot;

    protected EntityPlaneBase(EntityType<? extends EntityPlaneBase> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public float getMaxHealth() {
        return 50F;
    }

    public int getLifetime() {
        return 200;
    }

    /** CE: {@code canBeCollidedWith() { return this.health > 0; }} */
    @Override
    public boolean isPickable() {
        return this.health > 0;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(ModDamageTypes.NUCLEAR_BLAST)) return false;
        if (this.isInvulnerableTo(source)) return false;

        if (!this.isRemoved() && !level().isClientSide() && this.health > 0) {
            health -= amount;
            this.entityData.set(DATA_HEALTH, health);
            if (this.health <= 0) this.killPlane();
        }
        return true;
    }

    /** CE: {@code killPlane()} - the particle burst ({@code ExplosionSmallCreator}) is Phase 5 VFX, not ported (see class javadoc); the sound is kept. */
    protected void killPlane() {
        level().playSound(null, getX(), getY(), getZ(), HBMSoundHandler.planeShotDown.get(), SoundSource.NEUTRAL, 25.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HEALTH, getMaxHealth());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.tickCount = tag.getInt("ticksExisted");
        this.health = tag.getFloat("health");
        this.entityData.set(DATA_HEALTH, this.health);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ticksExisted", this.tickCount);
        tag.putFloat("health", this.health);
    }

    @Override
    public void tick() {
        Level level = this.level();

        if (level.isClientSide) {
            this.health = this.entityData.get(DATA_HEALTH);
            if (this.lerpSteps > 0) {
                this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
                --this.lerpSteps;
            } else {
                this.reapplyPosition();
            }
            return;
        }

        this.tickCount++;
        this.entityData.set(DATA_HEALTH, this.health);
        updateChunkTicket(this);

        Vec3 motion = this.getDeltaMovement();
        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        this.rotation();

        if (this.health <= 0) {
            this.setDeltaMovement(motion.x, motion.y - 0.025, motion.z);

            BlockPos pos = BlockPos.containing(getX(), getY(), getZ());
            if (!level.getBlockState(pos).isAir() || getY() < 0) {
                this.discard();
                new ExplosionVNT(level, getX(), getY(), getZ(), 15F).makeStandard().explode();
                level.playSound(null, getX(), getY(), getZ(), HBMSoundHandler.planeCrash.get(), SoundSource.NEUTRAL, 25.0F, 1.0F);
                return;
            }
        } else {
            // CE: motionY = 0F every tick while healthy - a plane flies dead level, no altitude AI.
            this.setDeltaMovement(motion.x, 0D, motion.z);
        }

        if (this.tickCount > this.timer) this.discard();
    }

    /** CE: {@code rotation()} - derives yaw/pitch from the current motion vector for rendering. */
    protected void rotation() {
        Vec3 motion = this.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(motion.y, horizontal) * 180.0D / Math.PI) - 90);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps;
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? this.lerpXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? this.lerpYRot : this.getYRot();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    // --- IChunkLoader ---------------------------------------------------------------------------

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
}
