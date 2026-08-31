package com.hbm.entity.projectile;

import com.hbm.entity.GunEntityTypes;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.BobMathUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import javax.annotation.Nullable;

/**
 * Port of CE's {@code com.hbm.entity.projectile.EntityBulletBaseMK4} (288 lines) - the concrete
 * "physical bullet" entity: originally 4 constructors (submunition, standard gun-fired, turret-fired
 * by raw yaw/pitch, and the bare no-arg used for deserialization/{@code EntityType} construction), plus
 * a 5th purely-additive "aim at a target" constructor added by
 * {@code docs/phase4/entities_legacy_bullet_system.md}'s pre-Sedna mob ballistics retarget (see that
 * constructor's own javadoc); a lockon-homing branch; and the fixed {@code onImpact} dispatch order the
 * gun-framework report calls out explicitly: {@code config.onImpact}, then - only if the bullet is
 * still alive - the type-appropriate {@code config.onRicochet}/{@code config.onEntityHit}.
 */
public class EntityBulletBaseMK4 extends EntityThrowableInterp implements IEntityWithComplexSpawn {

    private static final EntityDataAccessor<String> DATA_BULLET_CONFIG =
            SynchedEntityData.defineId(EntityBulletBaseMK4.class, EntityDataSerializers.STRING);

    public BulletConfig config;
    /** Used for rendering tracers - not consumed by this package, kept for Phase 5's renderer. */
    public double velocity;
    public double prevVelocity;
    public double accel;
    public float damage;
    public int ricochets = 0;
    @Nullable
    public Entity lockonTarget = null;

    public EntityBulletBaseMK4(EntityType<? extends EntityBulletBaseMK4> type, Level level) {
        super(type, level);
    }

    public EntityBulletBaseMK4(Level level) {
        this(GunEntityTypes.BULLET_MK4.get(), level);
    }

    // NOTE on the 3 "content" constructors below: each has a `protected`, EntityType-parameterized
    // primary overload plus a `public` CE-shaped convenience overload that hardcodes
    // GunEntityTypes.BULLET_MK4. This is NOT CE's own shape (CE's constructors never needed an
    // EntityType parameter at all - 1.12/Forge determined an entity's network identity from its Java
    // class dynamically at spawn time, not from a type object baked in at construction). Modern
    // Entity(EntityType, Level) requires every constructor in the hierarchy to thread an explicit type
    // through, and simply delegating (as CE's own EntityBulletBaseMK4CL does via `super(entity, config,
    // ...)`) would silently bake EntityBulletBaseMK4's own BULLET_MK4 type into every
    // EntityBulletBaseMK4CL instance instead of BULLET_MK4CL - since `this(...)`/`super(...)` calls
    // always resolve to the literal class they're written in, not the constructed object's real class.
    // Threading the type through explicitly is the correct, necessary fix; EntityBulletBaseMK4CL uses
    // the protected overloads directly with its own registered type.

    /** For submunitions! */
    protected EntityBulletBaseMK4(EntityType<? extends EntityBulletBaseMK4> type, Level level, LivingEntity entity, BulletConfig config, float damage, float gunSpread, Vec3 pos, Vec3 motion) {
        this(type, level);

        this.setOwner(entity);
        this.setBulletConfig(config);

        this.damage = damage;

        this.moveTo(pos.x, pos.y, pos.z, 0, 0);
        this.setPos(pos);

        this.setDeltaMovement(motion);

        this.shoot(motion.x, motion.y, motion.z, 1.0F, this.config.spread + gunSpread);
        alignRotationWithMotion();
    }

    public EntityBulletBaseMK4(Level level, LivingEntity entity, BulletConfig config, float damage, float gunSpread, Vec3 pos, Vec3 motion) {
        this(GunEntityTypes.BULLET_MK4.get(), level, entity, config, damage, gunSpread, pos, motion);
    }

    /** For standard guns. */
    protected EntityBulletBaseMK4(EntityType<? extends EntityBulletBaseMK4> type, LivingEntity entity, BulletConfig config, float baseDamage, float gunSpread, double sideOffset, double heightOffset, double frontOffset) {
        this(type, entity.level());
        this.setOwner(entity);
        this.setBulletConfig(config);

        this.damage = baseDamage * this.config.damageMult;

        this.moveTo(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), entity.getYRot(), entity.getXRot());

        Vec3 offset = new Vec3(sideOffset, heightOffset, frontOffset);
        offset = offset.xRot(-this.getXRot() / 180F * (float) Math.PI);
        offset = offset.yRot(-this.getYRot() / 180F * (float) Math.PI);

        this.setPos(this.position().add(offset));

        float mx = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        float mz = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        float my = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);

        this.shoot(mx, my, mz, 1.0F, gunSpread);
        alignRotationWithMotion();
    }

    public EntityBulletBaseMK4(LivingEntity entity, BulletConfig config, float baseDamage, float gunSpread, double sideOffset, double heightOffset, double frontOffset) {
        this(GunEntityTypes.BULLET_MK4.get(), entity, config, baseDamage, gunSpread, sideOffset, heightOffset, frontOffset);
    }

    /** For turrets - angles are in radians, and pitch is negative! */
    protected EntityBulletBaseMK4(EntityType<? extends EntityBulletBaseMK4> type, Level level, BulletConfig config, float baseDamage, float gunSpread, float yaw, float pitch) {
        this(type, level);

        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;

        float yRotDeg = yaw * 180F / (float) Math.PI;
        float xRotDeg = -pitch * 180F / (float) Math.PI;
        this.setYRot(yRotDeg);
        this.setXRot(xRotDeg);
        this.yRotO = yRotDeg;
        this.xRotO = xRotDeg;

        float mx = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        float mz = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        float my = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
        this.shoot(mx, my, mz, 1.0F, gunSpread);
        alignRotationWithMotion();
    }

    public EntityBulletBaseMK4(Level level, BulletConfig config, float baseDamage, float gunSpread, float yaw, float pitch) {
        this(GunEntityTypes.BULLET_MK4.get(), level, config, baseDamage, gunSpread, yaw, pitch);
    }

    /**
     * For mob/boss AI firing at a specific target - the CE-faithful equivalent of legacy
     * {@code com.hbm.entity.projectile.EntityBulletBase}'s own
     * {@code (World, int, EntityLivingBase shooter, EntityLivingBase target, float motion, float
     * deviation)} mob-firing constructor, relocated here per
     * {@code docs/phase4/entities_legacy_bullet_system.md}'s Headline finding #5 / Key design
     * decisions (retarget the 8 live {@code GunNPCFactory} ammo definitions onto this class rather
     * than porting a second, parallel ballistics entity). Computes the same ~10-line aim-at-target
     * {@code atan2} trig CE's constructor does (this port's coordinate/angle conventions are
     * otherwise identical to CE's - see {@link #shoot}/{@link #alignRotationWithMotion}), then rolls
     * {@link #damage} from {@code config}'s {@link BulletConfig#dmgMin}/{@link BulletConfig#dmgMax}
     * (see that pair's own javadoc for why this constructor, uniquely among this class's 4
     * constructors, has no externally-supplied {@code baseDamage} parameter - CE's own equivalent
     * constructor doesn't take one either, since the legacy system always rolled its own flat damage
     * range instead of scaling a caller-supplied base).
     * <p>
     * <b>Purely additive</b> - does not alter any existing constructor's signature or behavior; the 3
     * gun/turret-firing constructors above and {@code TileEntityTurretBaseNT}'s own use of this class
     * are untouched.
     */
    protected EntityBulletBaseMK4(EntityType<? extends EntityBulletBaseMK4> type, Level level, BulletConfig config, LivingEntity shooter, LivingEntity target, float motion, float deviation) {
        this(type, level);

        this.setOwner(shooter);
        this.setBulletConfig(config);
        this.damage = config.rollDamage(this.random);

        double eyeY = shooter.getY() + shooter.getEyeHeight() - 0.10000000149011612D;
        double d0 = target.getX() - shooter.getX();
        double d1 = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - eyeY;
        double d2 = target.getZ() - shooter.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        if (d3 >= 1.0E-7D) {
            float yaw = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
            float pitch = (float) (-(Math.atan2(d1, d3) * 180.0D / Math.PI));
            double d4 = d0 / d3;
            double d5 = d2 / d3;
            this.moveTo(shooter.getX() + d4, eyeY, shooter.getZ() + d5, yaw, pitch);
            this.setPos(this.position());
            this.shoot(d0, d1, d2, motion, deviation);
            alignRotationWithMotion();
        } else {
            // CE's own constructor leaves position/motion untouched in this (practically unreachable -
            // shooter and target at the exact same X/Z) degenerate case; match that here rather than
            // inventing a fallback aim direction it never had.
            this.moveTo(shooter.getX(), eyeY, shooter.getZ(), shooter.getYRot(), shooter.getXRot());
            this.setPos(this.position());
        }
    }

    public EntityBulletBaseMK4(Level level, BulletConfig config, LivingEntity shooter, LivingEntity target, float motion, float deviation) {
        this(GunEntityTypes.BULLET_MK4.get(), level, config, shooter, target, motion, deviation);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
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

        Vec3 prevPos = this.position();

        super.tick();

        Vec3 newPos = this.position();
        double dX = newPos.x - prevPos.x;
        double dY = newPos.y - prevPos.y;
        double dZ = newPos.z - prevPos.z;

        if (this.lockonTarget != null && this.lockonTarget.isAlive()) {
            Vec3 motion = this.getDeltaMovement();
            double vel = motion.length();
            Vec3 delta = new Vec3(lockonTarget.getX() - this.getX(), lockonTarget.getY() + lockonTarget.getBbHeight() / 2D - this.getY(), lockonTarget.getZ() - this.getZ());
            float turn = Math.min(0.005F * this.tickCount, 1F);
            Vec3 newVec = new Vec3(
                    BobMathUtil.interp(motion.x, delta.x, turn),
                    BobMathUtil.interp(motion.y, delta.y, turn),
                    BobMathUtil.interp(motion.z, delta.z, turn)
            ).normalize().scale(vel);
            this.setDeltaMovement(newVec);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(this, new ClientboundTeleportEntityPacket(this));
            }
        }

        this.prevVelocity = this.velocity;
        this.velocity = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        // recompute orientation from the *actual* displacement this tick (not the raw motion vector,
        // which EntityThrowableNT.tick() already used for its own smoothed rotation update above) -
        // matches CE exactly, including the lack of smoothing here (a direct overwrite, not a lerp),
        // so the bullet's rendered orientation reflects where it truly ended up (important right after
        // a ricochet, where motion inverted mid-tick).
        if (!this.onGround() && velocity > 0) {
            float hyp = (float) Math.sqrt(dX * dX + dZ * dZ);
            this.setYRot((float) (Math.atan2(dX, dZ) * 180.0D / Math.PI));
            this.setXRot((float) (Math.atan2(dY, hyp) * 180.0D / Math.PI));

            while (this.getXRot() - this.xRotO < -180.0F) this.xRotO -= 360.0F;
            while (this.getXRot() - this.xRotO >= 180.0F) this.xRotO += 360.0F;
            while (this.getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
            while (this.getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;
        }

        if (!level().isClientSide && this.tickCount > config.expires) this.discard();

        if (this.config.onUpdate != null) this.config.onUpdate.accept(this);
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (!level().isClientSide) {
            if (this.config.onImpact != null) this.config.onImpact.accept(this, mop);
            if (this.isRemoved()) return;
            if (mop instanceof BlockHitResult bhr && this.config.onRicochet != null) this.config.onRicochet.accept(this, bhr);
            if (mop instanceof EntityHitResult ehr && this.config.onEntityHit != null) this.config.onEntityHit.accept(this, ehr);
        }
    }

    private void alignRotationWithMotion() {
        Vec3 motion = this.getDeltaMovement();
        float hyp = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, hyp) * 180.0D / Math.PI);

        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    protected double headingForceMult() {
        return 1D;
    }

    @Override
    public double getGravityVelocity() {
        return this.config.gravity;
    }

    @Override
    protected double motionMult() {
        return this.config.velocity + this.accel;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected float getWaterDrag() {
        return 1F;
    }

    @Override
    public boolean doesImpactEntities() {
        return this.config.impactsEntities;
    }

    @Override
    public boolean doesPenetrate() {
        return this.config.doesPenetrate;
    }

    @Override
    public boolean isSpectral() {
        return this.config.isSpectral;
    }

    @Override
    public int selfDamageDelay() {
        return this.config.selfDamageDelay;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        Entity thrower = this.getThrower();
        buffer.writeInt(thrower != null ? thrower.getId() : -1);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readInt();
        if (id >= 0 && level().getEntity(id) instanceof LivingEntity living) {
            this.setOwner(living);
        }
    }
}
