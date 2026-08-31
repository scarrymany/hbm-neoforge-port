package com.hbm.items.weapon.sedna;

import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.util.BobMathUtil;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.BulletConfig} (470 lines) - the ammo/projectile
 * "stat card": a fluent-builder POJO (velocity, spread, gravity, damage, armor-piercing,
 * ricochet/penetration flags, ...) plus the default hit-resolution lambdas every gun/turret ammo
 * type uses unless it overrides them. See {@code docs/phase3/gun_framework.md}'s Package A table
 * and "Key design/API decisions" for the research this class implements.
 * <p>
 * <b>Id scheme - a deliberate deviation from CE, not an oversight.</b> CE's {@code BulletConfig.id}
 * is a raw {@code int} assigned by static append-only registration order
 * ({@code id = configs.size(); configs.add(this);}), synced over the network as a raw
 * {@code DataParameter<Integer>} on the projectile entity. That id's correctness depends entirely on
 * every {@code XFactory*.init()} call running in the exact same textual order CE's own static
 * initializers happen to produce - a single reordered content file silently desyncs every synced
 * bullet-config id (see the gun-framework report's "Open questions"). This port replaces it with an
 * explicit {@link ResourceLocation} key, per this port's general {@code DeferredRegister}-flavored
 * preference for explicit ids over implicit construction order (see {@link #REGISTRY}): every
 * {@code BulletConfig} is constructed with an explicit id string (namespaced under {@code hbm} by
 * the {@link #BulletConfig(String)} constructor) or a fully-qualified {@link ResourceLocation}, and
 * that id/{@code ResourceLocation} pair is what actually gets synced to the client (as a plain
 * string via {@code EntityDataSerializers.STRING} - a heavier per-packet payload than CE's raw int,
 * an explicitly accepted tradeoff per the report's own recommendation) and is what any future
 * NBT-persisted ammo/mag-type reference should key off, since - unlike CE's transient network id -
 * a {@link ResourceLocation} is stable across reorderings, reloads, and save round-trips.
 */
public class BulletConfig implements Cloneable {

    /** Registration order preserved (LinkedHashMap) purely for deterministic iteration - not id-load-bearing. */
    private static final Map<ResourceLocation, BulletConfig> REGISTRY = new LinkedHashMap<>();

    /**
     * Builds the {@link DamageSource} an ammo type's damage class maps onto. CE's
     * {@code DamageSourceSednaNoAttacker}/{@code WithAttacker} custom {@code DamageSource} subclasses
     * cannot be ported as classes (1.21's {@code DamageSource} cannot be subclassed) - this replaces
     * them with datapack {@link DamageType} keys from this port's already-committed
     * {@link ModDamageTypes} {@code SEDNA_*} entries, exactly as
     * {@code docs/phase3/gun_framework.md}'s "Key design/API decisions" specifies. With a shooter,
     * {@code directEntity = projectile, causingEntity = shooter} (matching
     * {@code DamageSourceSednaWithAttacker.getImmediateSource()}/{@code getTrueSource()} 1:1 - vanilla's
     * own {@code DamageSources.arrow(AbstractArrow, Entity)} convenience method confirms this
     * direct-then-causing argument order via {@code source(type, arrow, owner)}); with no shooter, no
     * entity is attached at all (matching {@code DamageSourceSednaNoAttacker}, which never references
     * the projectile either).
     */
    public static DamageSource getDamage(Entity projectile, @Nullable LivingEntity shooter, DamageClass dmgClass) {
        Level level = projectile.level();
        ResourceKey<DamageType> type = damageType(dmgClass);
        return shooter != null
                ? level.damageSources().source(type, projectile, shooter)
                : level.damageSources().source(type);
    }

    private static ResourceKey<DamageType> damageType(DamageClass dmgClass) {
        return switch (dmgClass) {
            case PHYSICAL -> ModDamageTypes.SEDNA_PHYSICAL;
            case FIRE -> ModDamageTypes.SEDNA_FIRE;
            case EXPLOSIVE -> ModDamageTypes.SEDNA_EXPLOSION;
            case ELECTRIC -> ModDamageTypes.SEDNA_ELECTRIC;
            // SEDNA_PLASMA is a new entry this package adds to ModDamageTypes - see this task's
            // wiring snippet for ModDamageTypes.java (a shared file, not edited directly here).
            case PLASMA -> ModDamageTypes.SEDNA_PLASMA;
            case LASER -> ModDamageTypes.SEDNA_LASER;
            case MICROWAVE -> ModDamageTypes.SEDNA_MICROWAVE;
            case SUBATOMIC -> ModDamageTypes.SEDNA_SUBATOMIC;
            case OTHER -> ModDamageTypes.SEDNA_OTHER;
            // Legacy pre-Sedna mob ballistics retarget (docs/phase4/entities_legacy_bullet_system.md):
            // these 3 map directly onto already-registered ModDamageTypes keys matching CE's own
            // ModDamageSource.causeBulletDamage/causeTauDamage/causeDisplacementDamage singletons, not
            // the generic SEDNA_* categories above - the legacy system's damage-type selection was never
            // generic to begin with. Used by LegacyMobBulletConfigs (GunNPCFactory retarget, chopper/tau).
            case REVOLVER_BULLET -> ModDamageTypes.REVOLVER_BULLET;
            case TAU -> ModDamageTypes.TAU;
            case CHOPPER_BULLET -> ModDamageTypes.CHOPPER_BULLET;
        };
    }

    // ============================================================================================
    // Standard hit-resolution lambdas - the "real" logic per the gun-framework report: these are not
    // pure (they touch World/Entity/DamageSource state) but are the fixed default behavior every ammo
    // type gets unless a content file (Package D) overrides onRicochet/onEntityHit/onImpactBeam.
    // Declared before the instance fields below that default to them, matching CE's own file layout.
    // ============================================================================================

    public static final BiConsumer<EntityBulletBaseMK4, BlockHitResult> LAMBDA_STANDARD_RICOCHET = (bullet, bhr) -> {

        Level level = bullet.level();
        BlockPos pos = bhr.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Vec3 hitLoc = bhr.getLocation();

        // TODO(phase3-blocks/explosions): CE's ricochet lambda also special-cases ModBlocks.red_barrel
        // (trigger its own explosion), BlockDetonatable (an interface hook for shootable-triggered
        // blocks), and deco_crt (cycle a decorative CRT block's meta state) - none of those
        // blocks/interfaces exist in this port yet (owned by docs/phase3/bomb_blocks_and_detonators.md
        // / explosion_engine.md, not this ballistics-core package). Once they land, re-add those
        // branches here following CE's exact behavior.

        // CE's check is `b.getMaterial() == Material.GLASS && ... getExplosionResistance(null) < 0.6f`
        // to avoid destroying reinforced/special glass while still letting bullets shatter weak vanilla-
        // like glass. 1.21's BlockState has no Material.GLASS concept any more; SoundType.GLASS is the
        // real, confirmed modern equivalent "is this a glass-like block" test (every vanilla glass/pane/
        // stained-glass variant uses it), combined with the same resistance threshold CE used.
        if (state.getSoundType() == SoundType.GLASS && state.getBlock().getExplosionResistance() < 0.6F) {
            level.destroyBlock(pos, false);
            bullet.setPos(hitLoc);
            return;
        }

        Direction dir = bhr.getDirection();
        Vec3 face = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        Vec3 vel = bullet.getDeltaMovement().normalize();

        double angle = Math.abs(BobMathUtil.getCrossAngle(vel, face) - 90);

        if (angle <= bullet.config.ricochetAngle) {

            bullet.ricochets++;
            if (bullet.ricochets > bullet.config.maxRicochetCount) {
                bullet.setPos(hitLoc);
                bullet.discard();
            }

            Vec3 motion = bullet.getDeltaMovement();
            bullet.setDeltaMovement(switch (dir) {
                case DOWN, UP -> new Vec3(motion.x, motion.y * -1, motion.z);
                case NORTH, SOUTH -> new Vec3(motion.x, motion.y, motion.z * -1);
                case WEST, EAST -> new Vec3(motion.x * -1, motion.y, motion.z);
            });

            level.playSound(null, bullet.getX(), bullet.getY(), bullet.getZ(), HBMSoundHandler.ricochet.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
            bullet.setPos(hitLoc);
            // send an explicit teleport so the ricochet reads accurately instead of the client's own
            // position-interpolation smoothing the direction reversal away - CE does the same via
            // TrackerUtil.sendTeleport/SPacketEntityTeleport.
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(bullet, new ClientboundTeleportEntityPacket(bullet));
            }
        } else {
            bullet.setPos(hitLoc);
            bullet.discard();
        }
    };

    public static final BiConsumer<EntityBulletBaseMK4, EntityHitResult> LAMBDA_STANDARD_ENTITY_HIT = (bullet, ehr) -> {

        Entity entity = ehr.getEntity();

        if (entity == bullet.getThrower() && bullet.tickCount < bullet.selfDamageDelay()) return;
        if (entity instanceof LivingEntity deadCheck && deadCheck.isDeadOrDying()) return;

        DamageSource source = getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);
        float intendedDamage = bullet.damage;

        if (!(entity instanceof LivingEntity living)) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, bullet.damage);
            return;
        }

        if (bullet.config.headshotMult > 1F) {
            double head = living.getBbHeight() - living.getEyeHeight();
            if (living.isAlive() && ehr.getLocation().y > (living.getY() + living.getBbHeight() - head * 2)) {
                intendedDamage *= bullet.config.headshotMult;
            }
        }

        float prevHealth = living.getHealth();

        EntityDamageUtil.attackEntityFromNT(living, source, intendedDamage, true, true, bullet.config.knockbackMult, bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent);

        float newHealth = living.getHealth();

        if (bullet.config.damageFalloffByPen) bullet.damage -= (float) (Math.max(prevHealth - newHealth, 0) * 0.5);
        if (!bullet.doesPenetrate() || bullet.damage < 0) {
            bullet.setPos(ehr.getLocation());
            bullet.discard();
        }

        // TODO(phase5-client): CE calls ConfettiUtil.decideConfetti(living, source) here on kill (a
        // themed death-effect particle spawn) - ConfettiUtil is Phase 5 client scope, not ported yet
        // (see docs/phase3/gun_framework.md's Deferred scope). Skipped gracefully.
    };

    public static final BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_STANDARD_BEAM_HIT = (beam, hr) -> {

        if (!(hr instanceof EntityHitResult ehr)) return;
        Entity entity = ehr.getEntity();

        if (entity instanceof LivingEntity deadCheck && deadCheck.isDeadOrDying()) return;

        DamageSource source = getDamage(beam, beam.getThrower(), beam.config.dmgClass);

        if (!(entity instanceof LivingEntity living)) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, beam.damage);
            return;
        }

        EntityDamageUtil.attackEntityFromNT(living, source, beam.damage, true, true, beam.config.knockbackMult, beam.config.armorThresholdNegation, beam.config.armorPiercingPercent);

        // TODO(phase5-client): CE calls ConfettiUtil.decideConfetti(living, source) here on kill -
        // see the TODO on LAMBDA_STANDARD_ENTITY_HIT above for why it isn't ported yet.
    };

    /** Same as {@link #LAMBDA_STANDARD_BEAM_HIT} except {@code allowSpecialCancel = false} and no confetti call - matches CE exactly. */
    public static final BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_BEAM_HIT = (beam, hr) -> {

        if (!(hr instanceof EntityHitResult ehr)) return;
        Entity entity = ehr.getEntity();

        if (entity instanceof LivingEntity deadCheck && deadCheck.isDeadOrDying()) return;

        DamageSource source = getDamage(beam, beam.getThrower(), beam.config.dmgClass);

        if (!(entity instanceof LivingEntity living)) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, beam.damage);
            return;
        }

        EntityDamageUtil.attackEntityFromNT(living, source, beam.damage, true, false, beam.config.knockbackMult, beam.config.armorThresholdNegation, beam.config.armorPiercingPercent);
    };

    // ============================================================================================
    // Instance state - field-for-field port of CE's own POJO.
    // ============================================================================================

    public ResourceLocation id;

    private @Nullable ComparableStack ammo;
    private @Nullable ItemStack casingItem;
    public int casingAmount;
    /** How much ammo is added to a standard mag when loading one item. */
    public int ammoReloadCount = 1;
    public float velocity = 10F;
    public float spread = 0F;
    public float wear = 1F;
    public int projectilesMin = 1;
    public int projectilesMax = 1;
    public ProjectileType pType = ProjectileType.BULLET;

    public float damageMult = 1.0F;
    public float armorThresholdNegation = 0.0F;
    public float armorPiercingPercent = 0.0F;
    public float knockbackMult = 0.1F;
    public float headshotMult = 1.25F;

    public DamageClass dmgClass = DamageClass.PHYSICAL;

    // ============================================================================================
    // Flat damage-range roll - additive support for docs/phase4/entities_legacy_bullet_system.md's
    // GunNPCFactory retarget. CE's legacy BulletConfiguration always rolled its own dmgMin/dmgMax at
    // hit time (`rand.nextFloat()*(dmgMax-dmgMin)+dmgMin`) rather than taking an externally-supplied
    // `baseDamage` the way every other Sedna-native constructor here does (gun/turret configs have no
    // dmgMin/dmgMax at all - see `damageMult` above). Unused (both stay 0) by any config that only ever
    // spawns via the existing baseDamage-taking constructors; only
    // EntityBulletBaseMK4's mob/boss "aim at a target" constructor reads these, via #rollDamage.
    // ============================================================================================
    public float dmgMin = 0F;
    public float dmgMax = 0F;

    public float ricochetAngle = 5F;
    public int maxRicochetCount = 2;
    /** Whether damage dealt to an entity is subtracted from the projectile's remaining damage on penetration. */
    public boolean damageFalloffByPen = true;

    public Consumer<Entity> onUpdate;
    public BiConsumer<EntityBulletBaseMK4, HitResult> onImpact;
    /** CE's own comment: "fuck fuck fuck fuck i should have used a better base class here god dammit". */
    public BiConsumer<EntityBulletBeamBase, HitResult> onImpactBeam;
    public BiConsumer<EntityBulletBaseMK4, BlockHitResult> onRicochet = LAMBDA_STANDARD_RICOCHET;
    public BiConsumer<EntityBulletBaseMK4, EntityHitResult> onEntityHit = LAMBDA_STANDARD_ENTITY_HIT;

    /** {@code double}, not CE's {@code float} - matches {@code Projectile#getDefaultGravity()}'s real signature. */
    public double gravity = 0;
    public int expires = 30;
    public boolean impactsEntities = true;
    public boolean doesPenetrate = false;
    /** Whether projectiles ignore blocks entirely. */
    public boolean isSpectral = false;
    public int selfDamageDelay = 2;
    public boolean blackPowder = false;
    public boolean renderRotations = true;

    public BiConsumer<EntityBulletBaseMK4, Float> renderer;
    public BiConsumer<EntityBulletBeamBase, Float> rendererBeam;

    // NOTE: CE's `casing` (SpentCasing spawn-effect data) field/setOnCasing(SpentCasing) setter is NOT
    // ported here - SpentCasing is a pure client-rendering type (particle spawn on fire) with no bearing
    // on ballistics/hit-resolution, and belongs with Phase 5's render.item.weapon.sedna.* package per
    // docs/phase3/gun_framework.md's Deferred scope. Add it back once SpentCasing lands.
    // CE's setItem(GunFactory.EnumAmmo)/setItem(GunFactory.EnumAmmoSecret) overloads and
    // setCasing(ItemEnums.EnumCasingType, int) are likewise not ported: GunFactory (Package D content,
    // not yet built) and the flattened ModItems.ammo_standard/ammo_secret/casing item families don't
    // exist yet. Package D should call setItem(Item)/setItem(ItemStack)/setCasing(ItemStack, int)
    // directly with whichever flattened item constant ModItems ends up defining for each ammo/casing
    // type - the metadata-flattening ground rule means each CE metadata variant becomes its own Item.

    public BulletConfig(String path) {
        this(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    public BulletConfig(ResourceLocation id) {
        register(id);
    }

    private void register(ResourceLocation id) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Duplicate BulletConfig id: " + id);
        }
        this.id = id;
        REGISTRY.put(id, this);
    }

    @Nullable
    public static BulletConfig byId(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    @Nullable
    public static BulletConfig byId(String stringId) {
        ResourceLocation parsed = ResourceLocation.tryParse(stringId);
        return parsed == null ? null : REGISTRY.get(parsed);
    }

    public static Collection<BulletConfig> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    @Nullable
    public ComparableStack getAmmo() {
        return this.ammo;
    }

    @Nullable
    public ItemStack getCasingItem() {
        return this.casingItem;
    }

    /**
     * Rolls a flat damage value from {@link #dmgMin}/{@link #dmgMax}, matching CE legacy
     * {@code BulletConfiguration}'s own hit-time roll formula
     * ({@code rand.nextFloat()*(dmgMax-dmgMin)+dmgMin}) exactly. Returns {@link #dmgMin} unrolled
     * (no randomness) when {@code dmgMax <= dmgMin}, which covers both misconfiguration and the
     * common flat-damage case (e.g. a config with {@code dmgMin == dmgMax}).
     */
    public float rollDamage(net.minecraft.util.RandomSource random) {
        return dmgMax > dmgMin ? dmgMin + random.nextFloat() * (dmgMax - dmgMin) : dmgMin;
    }

    // ============================================================================================
    // Fluent builder setters - all pure, all return `this`. Field-for-field port of CE's own builder.
    // ============================================================================================

    public BulletConfig setBeam() { this.pType = ProjectileType.BEAM; return this; }
    public BulletConfig setChunkloading() { this.pType = ProjectileType.BULLET_CHUNKLOADING; return this; }

    public BulletConfig setItem(Item ammo) { this.ammo = new ComparableStack(ammo); return this; }
    public BulletConfig setItem(ItemStack ammo) { this.ammo = new ComparableStack(ammo); return this; }
    public BulletConfig setItem(ComparableStack ammo) { this.ammo = ammo; return this; }

    public BulletConfig setCasing(ItemStack item, int amount) { this.casingItem = item; this.casingAmount = amount; return this; }

    public BulletConfig setReloadCount(int ammoReloadCount) { this.ammoReloadCount = ammoReloadCount; return this; }
    public BulletConfig setVel(float velocity) { this.velocity = velocity; return this; }
    public BulletConfig setSpread(float spread) { this.spread = spread; return this; }
    public BulletConfig setWear(float wear) { this.wear = wear; return this; }
    public BulletConfig setProjectiles(int amount) { this.projectilesMin = this.projectilesMax = amount; return this; }
    public BulletConfig setProjectiles(int min, int max) { this.projectilesMin = min; this.projectilesMax = max; return this; }
    public BulletConfig setDamage(float damageMult) { this.damageMult = damageMult; return this; }
    public BulletConfig setThresholdNegation(float armorThresholdNegation) { this.armorThresholdNegation = armorThresholdNegation; return this; }
    public BulletConfig setArmorPiercing(float armorPiercingPercent) { this.armorPiercingPercent = armorPiercingPercent; return this; }
    public BulletConfig setKnockback(float knockbackMult) { this.knockbackMult = knockbackMult; return this; }
    public BulletConfig setHeadshot(float headshotMult) { this.headshotMult = headshotMult; return this; }
    public BulletConfig setupDamageClass(DamageClass clazz) { this.dmgClass = clazz; return this; }
    public BulletConfig setDamageRange(float dmgMin, float dmgMax) { this.dmgMin = dmgMin; this.dmgMax = dmgMax; return this; }
    public BulletConfig setRicochetAngle(float angle) { this.ricochetAngle = angle; return this; }
    public BulletConfig setRicochetCount(int count) { this.maxRicochetCount = count; return this; }
    public BulletConfig setDamageFalloffByPen(boolean falloff) { this.damageFalloffByPen = falloff; return this; }
    public BulletConfig setGrav(double gravity) { this.gravity = gravity; return this; }
    public BulletConfig setLife(int expires) { this.expires = expires; return this; }
    public BulletConfig setImpactsEntities(boolean impact) { this.impactsEntities = impact; return this; }
    public BulletConfig setDoesPenetrate(boolean pen) { this.doesPenetrate = pen; return this; }
    public BulletConfig setSpectral(boolean spectral) { this.isSpectral = spectral; return this; }
    public BulletConfig setSelfDamageDelay(int delay) { this.selfDamageDelay = delay; return this; }
    public BulletConfig setBlackPowder(boolean bp) { this.blackPowder = bp; return this; }
    public BulletConfig setRenderRotations(boolean rot) { this.renderRotations = rot; return this; }

    public BulletConfig setRenderer(BiConsumer<EntityBulletBaseMK4, Float> renderer) { this.renderer = renderer; return this; }
    public BulletConfig setRendererBeam(BiConsumer<EntityBulletBeamBase, Float> renderer) { this.rendererBeam = renderer; return this; }

    public BulletConfig setOnUpdate(Consumer<Entity> lambda) { this.onUpdate = lambda; return this; }
    public BulletConfig setOnRicochet(BiConsumer<EntityBulletBaseMK4, BlockHitResult> lambda) { this.onRicochet = lambda; return this; }
    public BulletConfig setOnImpact(BiConsumer<EntityBulletBaseMK4, HitResult> lambda) { this.onImpact = lambda; return this; }
    public BulletConfig setOnBeamImpact(BiConsumer<EntityBulletBeamBase, HitResult> lambda) { this.onImpactBeam = lambda; return this; }
    public BulletConfig setOnEntityHit(BiConsumer<EntityBulletBaseMK4, EntityHitResult> lambda) { this.onEntityHit = lambda; return this; }

    /**
     * Replacement for CE's no-arg {@code clone()}/{@code forceReRegister()} pair - a new explicit id
     * is required at every clone site now that ids aren't assigned by construction order (see the
     * class javadoc). Package D content deriving a variant ammo config from a template must supply a
     * new id string here, e.g. {@code original.clone("bullet_9mm_ap")}.
     */
    public BulletConfig clone(String newPath) {
        return clone(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, newPath));
    }

    public BulletConfig clone(ResourceLocation newId) {
        try {
            BulletConfig cloned = (BulletConfig) super.clone();
            cloned.register(newId);
            return cloned;
        } catch (CloneNotSupportedException e) {
            // unreachable - this class implements Cloneable
            throw new AssertionError(e);
        }
    }

    public enum ProjectileType {
        BULLET,
        BULLET_CHUNKLOADING,
        BEAM
    }
}
