package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectable;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.explosion.ExplosionLarge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.entity.missile.EntityMIRV} (229 lines, read through the
 * MIRV-relevant half) - the cluster sub-munition {@code EntityMissileCustom.mirvSplit()} spawns.
 * Extends vanilla {@link Projectile} directly (CE extends its own {@code EntityThrowable}) since
 * this class needs none of {@code EntityThrowableNT}'s clip-and-dispatch machinery - CE's own
 * {@code onImpact} is empty, impact detection is instead a manual "is there a non-air block at my
 * feet" check run every tick in {@code onUpdate} (ported as-is below). Own 25 HP pool, own
 * {@code killMissile()} calling the same {@link ExplosionLarge#explode}/{@link
 * ExplosionLarge#spawnShrapnelShower} pair {@link EntityMissileBaseNT} uses. Ballistic free-fall
 * only - inherits the parent missile's velocity at split time (set by {@code mirvSplit}) and never
 * seeks a target of its own.
 */
public class EntityMIRV extends Projectile implements IChunkLoader, IRadarDetectable {

    public int health = 25;
    private ChunkPos loadedChunkPos = new ChunkPos(0, 0);

    public EntityMIRV(EntityType<? extends EntityMIRV> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.isRemoved() && !level().isClientSide()) {
            health -= amount;
            if (this.health <= 0) {
                this.discard();
                this.killMissile();
            }
        }
        return true;
    }

    private void killMissile() {
        Level level = level();
        Entity detonator = getOwner();
        ExplosionLarge.explode(level, detonator, getX(), getY(), getZ(), 5, true, false, true);
        Vec3 motion = getDeltaMovement();
        ExplosionLarge.spawnShrapnelShower(level, getX(), getY(), getZ(), motion.x, motion.y, motion.z, 15, 0.075);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        Vec3 motion = getDeltaMovement();
        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (!level().isClientSide()) {
            updateChunkTicket(this);
        }

        setDeltaMovement(motion.x, motion.y - 0.03, motion.z);

        rotation();

        if (!level().getBlockState(this.blockPosition()).is(Blocks.AIR)) {
            if (!level().isClientSide()) {
                level().addFreshEntity(EntityNukeExplosionMK5.statFac(level(), BombConfig.MIRV_RADIUS.get(), getX(), getY(), getZ()).setDetonator(getOwner()));
                if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                    EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), BombConfig.MIRV_RADIUS.get());
                }
            }
            this.discard();
        }
    }

    private void rotation() {
        Vec3 motion = getDeltaMovement();
        float f2 = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25_000;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // CE: empty onImpact - the actual "hit the ground" detection is the manual block check in tick().
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // CE: empty onImpact.
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MIRVLET;
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

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    public void setThrower(LivingEntity thrower) {
        this.setOwner(thrower);
    }
}
