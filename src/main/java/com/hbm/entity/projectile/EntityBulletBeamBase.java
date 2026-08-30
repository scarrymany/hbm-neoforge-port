package com.hbm.entity.projectile;

import com.hbm.entity.GunEntityTypes;
import com.hbm.items.weapon.sedna.BulletConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Port of CE's {@code com.hbm.entity.projectile.EntityBulletBeamBase} (373 lines) - the hitscan
 * counterpart to {@link EntityBulletBaseMK4}. Unlike the bullet entities, this is not a per-tick
 * flight simulation: {@link #performHitscan()} runs once (from the aimed-shot constructor, or
 * on-demand via {@link #performHitscanExternal(double)} for turret/battery/grenade-filling callers
 * that need to re-fire a beam from an already-positioned entity), computing a single heading vector
 * scaled to a fixed range, raytracing blocks once, then resolving entity hits with the same
 * nearest-hit-only (non-penetrating) vs. unsorted-iteration-order (penetrating) asymmetry
 * {@link EntityThrowableNT} uses for bullets. {@link #tick()} only exists to expire the entity after
 * {@code config.expires} ticks (its tracer-render lifetime) - no gravity, no flight loop.
 * <p>
 * <b>{@code EntityCoin} special case - stubbed, not ported.</b> CE's {@code performHitscan} has a
 * "coin flip" branch: any {@code EntityCoin} (a themed target/currency entity) along the beam's path
 * always blocks non-coin hits behind it and triggers its own nearest-entity fan-out (spawning a
 * second beam from the coin's position toward whatever's nearest). {@code com.hbm.entity.item.
 * EntityCoin} does not exist in this port yet - out of this ballistics-core package's scope per
 * {@code docs/phase3/gun_framework.md}'s Deferred scope. Skipped gracefully: with no
 * {@code EntityCoin} instances ever present, this branch would never match anything in CE either, so
 * omitting it changes nothing observable until that entity lands - see the TODO in
 * {@link #performHitscan()}.
 */
public class EntityBulletBeamBase extends Entity implements IEntityWithComplexSpawn {

    private static final EntityDataAccessor<String> DATA_BULLET_CONFIG =
            SynchedEntityData.defineId(EntityBulletBeamBase.class, EntityDataSerializers.STRING);

    @Nullable
    public LivingEntity thrower;
    public BulletConfig config;
    public float damage;
    public double headingX;
    public double headingY;
    public double headingZ;
    public double beamLength;

    public EntityBulletBeamBase(EntityType<? extends EntityBulletBeamBase> type, Level level) {
        super(type, level);
    }

    public EntityBulletBeamBase(Level level) {
        this(GunEntityTypes.BULLET_BEAM.get(), level);
    }

    public EntityBulletBeamBase(Level level, BulletConfig config, float baseDamage) {
        this(level);
        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;
    }

    public EntityBulletBeamBase(LivingEntity entity, BulletConfig config, float baseDamage) {
        this(entity.level(), config, baseDamage);
        this.thrower = entity;
    }

    public EntityBulletBeamBase(LivingEntity entity, BulletConfig config, float baseDamage, float angularInaccuracy, double sideOffset, double heightOffset, double frontOffset) {
        this(entity.level());

        this.thrower = entity;
        this.setBulletConfig(config);

        this.damage = baseDamage * this.config.damageMult;

        float yaw = entity.getYRot() + (float) this.random.nextGaussian() * angularInaccuracy;
        float pitch = entity.getXRot() + (float) this.random.nextGaussian() * angularInaccuracy;
        this.moveTo(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), yaw, pitch);

        Vec3 offset = new Vec3(sideOffset, heightOffset, frontOffset);
        offset = offset.xRot(-this.getXRot() / 180F * (float) Math.PI);
        offset = offset.yRot(-this.getYRot() / 180F * (float) Math.PI);

        this.setPos(this.position().add(offset));

        this.headingX = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingZ = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);

        double range = 250D;
        this.headingX *= range;
        this.headingY *= range;
        this.headingZ *= range;

        performHitscan();
    }

    public void setRotationsFromVector(Vec3 delta) {
        this.setXRot((float) (-Math.asin(delta.y / delta.length()) * 180D / Math.PI));
        this.setYRot((float) (-Math.atan2(delta.x, delta.z) * 180D / Math.PI));

        this.headingX = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingZ = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
    }

    /** Re-fires this beam's hitscan from its current position/rotation at a caller-supplied range - used by turret/battery-socket/grenade-filling callers, not just the (unported) coin fan-out. */
    public void performHitscanExternal(double range) {
        this.headingX *= range;
        this.headingY *= range;
        this.headingZ *= range;
        performHitscan();
    }

    @Nullable
    public LivingEntity getThrower() {
        return this.thrower;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BULLET_CONFIG, "");
    }

    @Nullable
    public BulletConfig getBulletConfig() {
        String raw = this.getEntityData().get(DATA_BULLET_CONFIG);
        if (raw == null || raw.isEmpty()) return null;
        return BulletConfig.byId(raw);
    }

    public void setBulletConfig(BulletConfig config) {
        this.config = config;
        this.getEntityData().set(DATA_BULLET_CONFIG, config.id.toString());
    }

    @Override
    public void tick() {

        if (config == null) config = this.getBulletConfig();

        if (config == null) {
            this.discard();
            return;
        }

        if (config.onUpdate != null) config.onUpdate.accept(this);

        super.tick();

        if (!level().isClientSide && this.tickCount > config.expires) this.discard();
    }

    protected void performHitscan() {

        Level level = level();
        Vec3 pos = position();
        Vec3 nextPos = pos.add(headingX, headingY, headingZ);

        HitResult mop = null;
        if (!isSpectral()) {
            HitResult blockHit = level.clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) mop = blockHit;
        }
        Vec3 clippedNextPos = mop != null ? mop.getLocation() : nextPos;

        if (!level.isClientSide && doesImpactEntities()) {

            // TODO(entity-item-coin): CE's performHitscan also special-cases EntityCoin here - see
            // this class's javadoc for why it is skipped gracefully rather than ported.

            Entity hitEntity = null;
            double nearest = 0.0D;
            Vec3 nearestHitPos = null;

            AABB region = getBoundingBox().expandTowards(headingX, headingY, headingZ).inflate(1.0D, 1.0D, 1.0D);
            for (Entity value : level.getEntities(this, region, e -> true)) {
                if (value == thrower || !value.isPickable()) continue;

                double hitbox = 0.3D;
                AABB aabb = value.getBoundingBox().inflate(hitbox, hitbox, hitbox);
                Optional<Vec3> clip = aabb.clip(pos, clippedNextPos);

                if (clip.isPresent()) {
                    double dist = pos.distanceTo(clip.get());

                    // if penetration is enabled, run impact for all intersecting entities, in
                    // whatever order getEntities() returns them - NOT distance-sorted, matching
                    // EntityThrowableNT's own penetrating branch exactly.
                    if (doesPenetrate()) {
                        onImpact(new EntityHitResult(value, clip.get()));
                    } else if (dist < nearest || nearest == 0.0D) {
                        hitEntity = value;
                        nearest = dist;
                        nearestHitPos = clip.get();
                    }
                }
            }

            // if not penetrating, only run it for the closest hit
            if (!doesPenetrate() && hitEntity != null) {
                mop = new EntityHitResult(hitEntity, nearestHitPos);
            }
        }

        if (mop != null) {
            boolean isPortalHit = mop instanceof BlockHitResult bhr && level.getBlockState(bhr.getBlockPos()).is(Blocks.NETHER_PORTAL);
            if (!isPortalHit) {
                onImpact(mop);
            }
            this.beamLength = mop.getLocation().subtract(pos).length();
        } else {
            this.beamLength = clippedNextPos.subtract(pos).length();
        }
    }

    protected void onImpact(HitResult mop) {
        if (!level().isClientSide) {
            if (this.config.onImpactBeam != null) this.config.onImpactBeam.accept(this, mop);
        }
    }

    public boolean doesImpactEntities() {
        return this.config.impactsEntities;
    }

    public boolean doesPenetrate() {
        return this.config.doesPenetrate;
    }

    public boolean isSpectral() {
        return this.config.isSpectral;
    }

    /** Beams are pure ephemeral tracer-render entities - never persisted, matching CE's own writeToNBTOptional() == false / self-discard-on-load. */
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean save(CompoundTag tag) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeDouble(beamLength);
        buffer.writeFloat(this.getYRot());
        buffer.writeFloat(this.getXRot());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.beamLength = buffer.readDouble();
        this.setYRot(buffer.readFloat());
        this.setXRot(buffer.readFloat());
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
