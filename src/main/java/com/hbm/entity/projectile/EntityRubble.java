package com.hbm.entity.projectile;

import com.hbm.damage.ModDamageTypes;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.entity.projectile.EntityRubble} (88 lines, read in full) - the
 * small thrown-block-visual entity {@code multitool_joule}/{@code multitool_mega}/
 * {@code shimmer_sledge} fling when they convert a struck block into flying debris (see
 * {@code docs/phase3/melee_weapons.md}'s Deferred scope entry naming this exact class). CE extends
 * its own unported {@code EntityThrowableNT}; per this task's explicit instruction this is adapted
 * directly onto vanilla {@link ThrowableProjectile} instead (confirmed real - Neo Edition's own
 * {@code Shrapnel}/{@code Rocket}/{@code BombletZeta} all extend it directly), rather than depending
 * on this port's own parallel, concurrently-developed {@code com.hbm.entity.projectile.
 * EntityThrowableNT} (a different, ballistics-package-owned file this task deliberately avoids
 * coupling to). {@link ThrowableProjectile}'s stock {@link #tick()} already implements the exact
 * "gravity-arc, fly until it hits something" physics CE's base class hand-rolls, and its single
 * {@link #onHit(HitResult)} dispatch point (covering both block and entity hits) matches CE's own
 * {@code onImpact(RayTraceResult)} shape more closely than {@link net.minecraft.world.entity.
 * projectile.Projectile}'s split {@code onHitBlock}/{@code onHitEntity} pair would.
 * <p>
 * <b>Block identity</b>: CE syncs a registry-id int (blockID) plus a caller-supplied raw meta int
 * (BLOCKMETA, set via {@code setMetaBasedOnBlock(Block, int)}) via two {@code DataParameter}s. 1.21
 * has no universal "meta" concept left (this port's metadata-flattening ground rule), and real call
 * sites (melee tools reading {@code level.getBlockState(pos)} before breaking the block) naturally
 * have a full {@link BlockState} in hand anyway, so this stores that state directly as a single
 * synced {@code int} via {@link Block#getId(BlockState)}/{@link Block#stateById(int)} - the same
 * lightweight int-palette-id pattern {@code EntityTNTPrimedBase} (this same package) and Neo
 * Edition's confirmed-real {@code FallingBlockEntityNT}/{@code TNTPrimedBase} use for identical
 * "which block was this" identity-carrying.
 * <p>
 * <b>Not ported</b>: CE's {@code getAirDrag() = 1F} override (no velocity decay in flight). Vanilla
 * {@link ThrowableProjectile#tick()} hardcodes its own drag (0.99 air / 0.8 water) with no exposed
 * override point (confirmed by Neo Edition's own {@code Shrapnel}, which extends it directly and
 * overrides no drag hook) - reimplementing the entire tick loop by hand purely to restore a ~1%-
 * per-tick velocity difference was judged not worth the risk for an entity that, per
 * {@link #onHit}, discards itself within its first few ticks of flight regardless.
 */
public class EntityRubble extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> BLOCK_STATE_ID =
            SynchedEntityData.defineId(EntityRubble.class, EntityDataSerializers.INT);

    public EntityRubble(EntityType<? extends EntityRubble> type, Level level) {
        super(type, level);
    }

    /** CE: {@code EntityRubble(World, double, double, double)} - spawned at a fixed position, zero initial motion. */
    public EntityRubble(Level level, double x, double y, double z) {
        this(RubbleEntityTypes.RUBBLE.get(), level);
        this.setPos(x, y, z);
    }

    /**
     * CE: {@code EntityRubble(World, EntityLivingBase)}, delegating to {@code EntityThrowableNT}'s own
     * (World, EntityLivingBase) constructor - reproduced directly here (spawn at the thrower's eye,
     * nudge back along their look vector, then {@code shoot()} forward) since this class does not
     * depend on that base class. CE never overrides {@code throwForce()}/{@code headingForceMult()}/
     * {@code throwAngle()} for {@code EntityRubble}, so the base class's own defaults
     * (1.5F / 0.0075D / 0F) are inlined here.
     */
    public EntityRubble(Level level, LivingEntity thrower) {
        this(RubbleEntityTypes.RUBBLE.get(), level);
        this.setOwner(thrower);

        float yRot = thrower.getYRot();
        float xRot = thrower.getXRot();
        this.setPos(
                thrower.getX() - Mth.cos(yRot / 180.0F * (float) Math.PI) * 0.16F,
                thrower.getY() + thrower.getEyeHeight() - 0.1D,
                thrower.getZ() - Mth.sin(yRot / 180.0F * (float) Math.PI) * 0.16F
        );
        this.setYRot(yRot);
        this.setXRot(xRot);

        float velocity = 0.4F;
        double mx = -Mth.sin(yRot / 180.0F * (float) Math.PI) * Mth.cos(xRot / 180.0F * (float) Math.PI) * velocity;
        double mz = Mth.cos(yRot / 180.0F * (float) Math.PI) * Mth.cos(xRot / 180.0F * (float) Math.PI) * velocity;
        double my = -Mth.sin(xRot / 180.0F * (float) Math.PI) * velocity;
        this.shoot(mx, my, mz, 1.5F, 1.0F);
    }

    /** CE's own {@code EntityThrowableNT.shoot()} Gaussian-scatter formula, headingForceMult = 0.0075D (base-class default, never overridden by CE's {@code EntityRubble}). */
    @Override
    public Vec3 getMovementToShoot(double x, double y, double z, float velocity, float inaccuracy) {
        double len = Math.sqrt(x * x + y * y + z * z);
        x /= len;
        y /= len;
        z /= len;

        x += this.random.nextGaussian() * 0.0075D * inaccuracy;
        y += this.random.nextGaussian() * 0.0075D * inaccuracy;
        z += this.random.nextGaussian() * 0.0075D * inaccuracy;

        return new Vec3(x, y, z).scale(velocity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK_STATE_ID, 0);
    }

    public BlockState getBlockState() {
        return Block.stateById(this.entityData.get(BLOCK_STATE_ID));
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(BLOCK_STATE_ID, Block.getId(state));
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (result instanceof EntityHitResult ehr) {
            ehr.getEntity().hurt(this.level().damageSources().source(ModDamageTypes.RUBBLE), 15.0F);
        }

        if (this.tickCount > 2) {
            this.discard();

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    HBMSoundHandler.blockDebris.get(), SoundSource.BLOCKS, 1.5F, 1.0F);

            // CE additionally broadcasts a custom ParticleBurstPacket(posX-1, posY, posZ-1, blockId,
            // meta) here - that packet type is not ported anywhere in this port. Substituted with the
            // closest vanilla equivalent (a block-break dig-particle-and-sound burst keyed to the
            // exact carried BlockState), matching the same substitution style already established by
            // this package's sibling EntityMovingConveyorObject for its own unported vanillant-explosion
            // VFX call site.
            if (this.level() instanceof ServerLevel serverLevel) {
                BlockPos burstPos = BlockPos.containing(this.getX() - 1, this.getY(), this.getZ() - 1);
                serverLevel.levelEvent(2001, burstPos, Block.getId(getBlockState()));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BlockStateId", this.entityData.get(BLOCK_STATE_ID));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(BLOCK_STATE_ID, tag.getInt("BlockStateId"));
    }
}
