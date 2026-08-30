package com.hbm.entity.projectile;

import com.hbm.api.entity.IThrowable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Port of CE's {@code com.hbm.entity.projectile.EntityThrowableNT} (370 lines, abstract) - "Near-
 * identical copy of EntityThrowable but deobfuscated & untangled" per CE's own class javadoc. This
 * is the real per-tick ballistics physics every bullet/beam entity in the gun framework builds on;
 * see {@code docs/phase3/gun_framework.md}'s Package A table for the full research this implements.
 * <p>
 * <b>Base class - a confirmed, deliberate departure from CE's own hand-rolled owner tracking.</b>
 * CE extends {@code Entity} directly and implements {@code IProjectile} (a 1.12 marker interface)
 * plus a hand-rolled {@code thrower}/{@code throwerName} pair - the latter existing only because
 * 1.12 entities couldn't reliably persist a UUID reference across a save round-trip, so CE stored
 * the shooter's player *name* and re-resolved it by name on load (CE's own comment on
 * {@code getThrower()} flags this as a limitation of that era). This extends vanilla
 * {@link Projectile} instead and uses its real {@code setOwner}/{@code getOwner} (which already
 * persists an owner UUID across saves natively) - confirmed real by Neo Edition's own parallel
 * {@code BulletBaseMK4}, and explicitly recommended by the gun-framework report's "Key design/API
 * decisions". The name-based re-resolution workaround is dropped entirely, not ported - it solves a
 * problem 1.21.1 does not have.
 * <p>
 * <b>Motion model - preserved exactly, not "fixed".</b> Position update is
 * {@code pos += motion*motionMult(); motion *= drag; motion.y -= gravity} every tick: a flat,
 * per-tick gravity *subtraction*, not real acceleration - this produces the same parabolic drop
 * vanilla arrows use, and is CE's actual intentional behavior (see {@link #getGravityVelocity()}'s
 * javadoc). It happens to line up with vanilla {@code Projectile#getDefaultGravity()}'s own
 * override-point shape (see {@link #getDefaultGravity()}), which is not a coincidence - vanilla's
 * own thrown-item gravity model is the same flat-subtraction scheme.
 * <p>
 * <b>Penetrating vs non-penetrating impact order - preserved exactly, a documented CE quirk, not a
 * bug.</b> Non-penetrating bullets track and dispatch only the *nearest* intersecting entity along
 * the swept segment. Penetrating bullets instead call {@link #hitTargetOrDeflectSelf(HitResult)}
 * once per intersecting entity in whatever order {@link Level#getEntities(Entity, AABB, Predicate)}
 * happens to return them - NOT distance-sorted. See the gun-framework report's "Open questions" for
 * why this matters for damage-falloff-by-penetration ammo and should be unit-tested against, not
 * "cleaned up" into a sorted loop.
 */
public abstract class EntityThrowableNT extends Projectile implements IThrowable {

    private static final EntityDataAccessor<Byte> DATA_STUCK_IN =
            SynchedEntityData.defineId(EntityThrowableNT.class, EntityDataSerializers.BYTE);

    public int throwableShake;
    protected boolean inGround;
    protected int ticksInGround;
    protected int ticksInAir;

    private BlockPos stuckBlockPos = BlockPos.ZERO;
    @Nullable
    private Block stuckBlock;

    protected EntityThrowableNT(EntityType<? extends EntityThrowableNT> type, Level level) {
        super(type, level);
    }

    protected EntityThrowableNT(EntityType<? extends EntityThrowableNT> type, Level level, double x, double y, double z) {
        this(type, level);
        this.ticksInGround = 0;
        this.setPos(x, y, z);
    }

    /** General "thrown by hand" constructor (arc toss, e.g. a grenade) - not used by the bullet/beam entities. */
    protected EntityThrowableNT(EntityType<? extends EntityThrowableNT> type, Level level, LivingEntity thrower) {
        this(type, level);
        this.setOwner(thrower);
        this.moveTo(thrower.getX(), thrower.getY() + thrower.getEyeHeight(), thrower.getZ(), thrower.getYRot(), thrower.getXRot());
        this.setPos(
                this.getX() - Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F,
                this.getY() - 0.1D,
                this.getZ() - Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F
        );
        float velocity = 0.4F;
        double mx = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI) * velocity;
        double mz = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI) * velocity;
        double my = -Mth.sin((this.getXRot() + throwAngle()) / 180.0F * (float) Math.PI) * velocity;
        this.shoot(mx, my, mz, throwForce(), 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STUCK_IN, (byte) 0);
    }

    public int getStuckIn() {
        return this.getEntityData().get(DATA_STUCK_IN);
    }

    public void setStuckIn(int side) {
        this.getEntityData().set(DATA_STUCK_IN, (byte) side);
    }

    protected float throwForce() {
        return 1.5F;
    }

    /** CE's own inaccuracy-scatter magnitude, consumed by {@link #getMovementToShoot}. Not a real vanilla hook. */
    protected double headingForceMult() {
        return 0.0075D;
    }

    protected float throwAngle() {
        return 0.0F;
    }

    /** CE's own per-tick "distance traveled" scale factor, applied to raw delta-movement. Not a real vanilla hook. */
    protected double motionMult() {
        return 1.0D;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = getBoundingBox().getSize() * 4.0D;
        if (Double.isNaN(d0)) d0 = 4.0D;
        d0 *= 64.0D;
        return distance < d0 * d0;
    }

    /**
     * Real vanilla {@link Projectile} override point - {@code shoot(x,y,z,velocity,inaccuracy)}
     * itself is inherited unchanged and calls this to compute the scattered launch vector, then sets
     * delta-movement and yaw/pitch from it. This is CE's own {@code shoot()} body (normalize, add
     * per-axis Gaussian noise scaled by {@link #headingForceMult()}, then scale by velocity) -
     * deliberately Gaussian, not vanilla's own default triangular-distribution scatter, to preserve
     * CE's exact spread behavior.
     */
    @Override
    public Vec3 getMovementToShoot(double x, double y, double z, float velocity, float inaccuracy) {
        double len = Math.sqrt(x * x + y * y + z * z);
        x /= len;
        y /= len;
        z /= len;

        x += this.random.nextGaussian() * headingForceMult() * inaccuracy;
        y += this.random.nextGaussian() * headingForceMult() * inaccuracy;
        z += this.random.nextGaussian() * headingForceMult() * inaccuracy;

        return new Vec3(x, y, z).scale(velocity);
    }

    @Override
    public void tick() {
        super.tick();

        if (throwableShake > 0) --throwableShake;

        if (inGround) {
            if (level().getBlockState(stuckBlockPos).getBlock() == stuckBlock) {
                ++ticksInGround;

                if (groundDespawn() > 0 && ticksInGround == groundDespawn()) {
                    discard();
                }

                return;
            }

            inGround = false;
            ticksInGround = 0;
            ticksInAir = 0;
        }

        ++ticksInAir;

        double mm = motionMult();
        Vec3 pos = position();
        Vec3 motion = getDeltaMovement();
        Vec3 nextPos = pos.add(motion.scale(mm));

        HitResult mop = null;
        if (!isSpectral()) {
            HitResult blockHit = level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) mop = blockHit;
        }

        if (mop != null) {
            nextPos = mop.getLocation();
        }

        if (!level().isClientSide && doesImpactEntities()) {
            LivingEntity shooter = getThrower();

            Predicate<Entity> filter = null;
            if (shooter != null && ticksInAir < selfDamageDelay()) {
                filter = e -> e != shooter;
            }
            Predicate<Entity> finalFilter = filter;

            if (!doesPenetrate()) {
                EntityHitResult entHit = findNearestEntityHit(pos, nextPos, finalFilter);
                if (entHit != null) mop = entHit;
            } else {
                AABB swept = getBoundingBox().expandTowards(motion.scale(mm)).inflate(1.0D);
                List<Entity> candidates = level().getEntities(this, swept, e -> finalFilter == null || finalFilter.test(e));
                for (Entity entity : candidates) {
                    if (!entity.isPickable()) continue;
                    double hitbox = 0.3D;
                    AABB aabb = entity.getBoundingBox().inflate(hitbox, hitbox, hitbox);
                    Optional<Vec3> clip = aabb.clip(pos, nextPos);
                    if (clip.isPresent()) {
                        hitTargetOrDeflectSelf(new EntityHitResult(entity, clip.get()));
                    }
                }
            }
        }

        if (mop != null) {
            // CE skips onImpact entirely for a portal-block hit (calling the 1.12-only
            // Entity#setPortal(pos) instead, letting the bullet fly into the portal rather than
            // ricocheting/exploding on it) - preserved here, with checkInsideBlocks() below standing
            // in for the actual portal-entry detection setPortal(pos) used to do.
            boolean isPortalHit = mop instanceof BlockHitResult bhr
                    && level().getBlockState(bhr.getBlockPos()).is(Blocks.NETHER_PORTAL);

            if (!isPortalHit && !EventHooks.onProjectileImpact(this, mop)) {
                hitTargetOrDeflectSelf(mop);
            }
        }

        checkInsideBlocks();

        if (!onGround()) {
            double horizontalDist = motion.horizontalDistance();
            this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
            this.setXRot((float) (Math.atan2(motion.y, horizontalDist) * 180.0D / Math.PI));

            while (getXRot() - xRotO < -180.0F) xRotO -= 360.0F;
            while (getXRot() - xRotO >= 180.0F) xRotO += 360.0F;
            while (getYRot() - yRotO < -180.0F) yRotO -= 360.0F;
            while (getYRot() - yRotO >= 180.0F) yRotO += 360.0F;

            setXRot(xRotO + (getXRot() - xRotO) * 0.2F);
            setYRot(yRotO + (getYRot() - yRotO) * 0.2F);
        }

        float drag = getAirDrag();
        double gravity = getGravityVelocity();

        double newX = pos.x + motion.x * mm;
        double newY = pos.y + motion.y * mm;
        double newZ = pos.z + motion.z * mm;

        if (isInWater()) {
            for (int i = 0; i < 4; ++i) {
                float f = 0.25F;
                level().addParticle(ParticleTypes.BUBBLE, newX - motion.x * f, newY - motion.y * f, newZ - motion.z * f, motion.x, motion.y, motion.z);
            }
            drag = getWaterDrag();
        }

        Vec3 dragged = motion.scale(drag);
        setDeltaMovement(dragged.x, dragged.y - gravity, dragged.z);

        setPos(newX, newY, newZ);
    }

    @Nullable
    private EntityHitResult findNearestEntityHit(Vec3 pos, Vec3 nextPos, @Nullable Predicate<Entity> filter) {
        double nearest = 0.0D;
        EntityHitResult nearestHit = null;

        AABB region = getBoundingBox().expandTowards(nextPos.subtract(pos)).inflate(1.0D);
        for (Entity entity : level().getEntities(this, region, e -> (filter == null || filter.test(e)) && e.isPickable())) {
            double hitbox = 0.3D;
            AABB aabb = entity.getBoundingBox().inflate(hitbox, hitbox, hitbox);
            Optional<Vec3> clip = aabb.clip(pos, nextPos);
            if (clip.isPresent()) {
                double dist = pos.distanceTo(clip.get());
                if (dist < nearest || nearest == 0.0D) {
                    nearest = dist;
                    nearestHit = new EntityHitResult(entity, clip.get());
                }
            }
        }

        return nearestHit;
    }

    public boolean doesImpactEntities() {
        return true;
    }

    public boolean doesPenetrate() {
        return false;
    }

    public boolean isSpectral() {
        return false;
    }

    public int selfDamageDelay() {
        return 5;
    }

    public void getStuck(BlockPos pos, int side) {
        this.stuckBlockPos = pos;
        this.stuckBlock = level().getBlockState(pos).getBlock();
        this.inGround = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setStuckIn(side);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(this, new ClientboundTeleportEntityPacket(this));
        }
    }

    /**
     * CE's own comment: "Why 0.03? this is overridden in every child class no?" - kept as CE has it,
     * the base-class default that every concrete bullet type overrides with its own
     * {@code BulletConfig.gravity}.
     */
    public double getGravityVelocity() {
        return 0.03D;
    }

    /** Delegates to {@link #getGravityVelocity()} so any vanilla-internal caller of the real hook stays consistent with this class's own manual gravity math in {@link #tick()}. */
    @Override
    protected double getDefaultGravity() {
        return getGravityVelocity();
    }

    protected abstract void onImpact(HitResult result);

    /**
     * Real vanilla {@link Projectile} override point, consulted by {@link #hitTargetOrDeflectSelf}
     * for shield-deflection eligibility - confirmed real by Neo Edition's own {@code BulletBaseMK4}
     * override using this exact method name. CE's own equivalent gate ({@code entity.
     * canBeCollidedWith()} combined with {@link #doesImpactEntities()}) is already applied inline by
     * this class's own {@link #tick()} sweep before a {@link net.minecraft.world.phys.EntityHitResult}
     * is ever constructed; this override exists so vanilla's own internal call path agrees with the
     * same {@link #doesImpactEntities()} gate rather than silently falling back to a default that
     * doesn't know about it.
     */
    @Override
    protected boolean canHitEntity(Entity target) {
        return doesImpactEntities() && target.canBeHitByProjectile();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        onImpact(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        onImpact(result);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("xTile", stuckBlockPos.getX());
        compound.putInt("yTile", stuckBlockPos.getY());
        compound.putInt("zTile", stuckBlockPos.getZ());
        compound.putString("inTile", stuckBlock == null ? "" : BuiltInRegistries.BLOCK.getKey(stuckBlock).toString());
        compound.putByte("shake", (byte) throwableShake);
        compound.putBoolean("inGround", inGround);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        stuckBlockPos = new BlockPos(compound.getInt("xTile"), compound.getInt("yTile"), compound.getInt("zTile"));
        String blockId = compound.getString("inTile");
        stuckBlock = blockId.isEmpty() ? null : BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
        throwableShake = compound.getByte("shake") & 255;
        inGround = compound.getBoolean("inGround");
    }

    @Nullable
    public LivingEntity getThrower() {
        Entity owner = this.getOwner();
        return owner instanceof LivingEntity living ? living : null;
    }

    @Override
    public void setThrower(LivingEntity thrower) {
        this.setOwner(thrower);
    }

    protected float getAirDrag() {
        return 0.99F;
    }

    protected float getWaterDrag() {
        return 0.8F;
    }

    protected int groundDespawn() {
        return 1200;
    }
}
