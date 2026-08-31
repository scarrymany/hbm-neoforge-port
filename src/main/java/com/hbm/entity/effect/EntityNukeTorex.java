package com.hbm.entity.effect;

import com.hbm.interfaces.IConstantRenderer;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityNukeTorex} (616 lines, read in full) - the
 * mushroom-cloud "Toroidial Convection Simulation" VFX entity spawned alongside most nuke
 * detonations.
 * <p>
 * This class previously carried only entity registration/despawn plumbing, with the ~500-line
 * client-only {@code onUpdate()} cloudlet simulation explicitly deferred as a Phase 5 TODO (see
 * git history / {@code docs/phase5/reactor_and_explosion_visual_effects.md} Headline finding 7 for
 * that prior scope cut). This pass ports that simulation in full: the {@link Cloudlet} inner class
 * (motion/color/alpha math for all 4 {@link TorexType} values, verbatim from CE except for the
 * 1.21.1 API-shape deltas documented per-method below) and {@link #clientTick()} (CE's {@code
 * onUpdate()}'s {@code if (world.isRemote)} branch). The actual quad/flare/flash rendering and the
 * HUD-flash/shake trigger live in the companion class {@code com.hbm.client.render.entity.effect.
 * TorexRenderer} (client-only package - see that class's javadoc), matching CE's own
 * entity/renderer split ({@code EntityNukeTorex} simulates, {@code RenderTorex} draws).
 *
 * <h2>1.12 to 1.21.1 API deltas applied throughout this port</h2>
 * <ul>
 *   <li>{@code Entity.posX/posY/posZ} (public mutable fields in 1.12) - has no field equivalent on
 *       modern {@link Entity}; every read of the outer entity's own position is {@link #getX()}/
 *       {@link #getY()}/{@link #getZ()} instead. {@link Cloudlet}'s <em>own</em> {@code posX/posY/posZ}
 *       fields are unaffected (they were always plain doubles owned by the cloudlet itself, not
 *       inherited from {@code Entity}).</li>
 *   <li>{@code ticksExisted} - this class's own already-committed manual {@link #age} counter
 *       (see that field's javadoc for why {@code tick()} cannot rely on vanilla's {@code tickCount}).
 *       Incremented once at the very top of {@link #tick()}, before either branch runs, so {@code
 *       age == 1} on the first real tick - matching CE's {@code ticksExisted == 1} check.</li>
 *   <li>{@code MathHelper}/{@code Vec3NT} (CE's own vector helper, rotate-self methods) -
 *       {@link Mth} and vanilla {@link Vec3} respectively. CE's {@code Vec3NT.rotateYawSelf(a)} is
 *       algebraically identical to vanilla {@link Vec3#yRot(float)} and {@code rotateRollSelf(a)}
 *       to {@link Vec3#zRot(float)} - confirmed by direct formula comparison against CE's own
 *       {@code com.hbm.util.Vec3NT} source ({@code rotateYaw}: {@code x*cos+z*sin, y, z*cos-x*sin};
 *       {@code rotateRoll}: {@code x*cos+y*sin, y*cos-x*sin, z}), both matching vanilla {@link Vec3}'s
 *       own {@code yRot}/{@code zRot} bit-for-bit. {@link Vec3} is immutable, so every
 *       {@code vec.rotateXSelf(a)} call becomes a reassignment {@code vec = vec.xRot(a)}.</li>
 *   <li>{@code World.getHeight(x, z)} - {@link Level#getHeight(Heightmap.Types, int, int)} with
 *       {@link Heightmap.Types#MOTION_BLOCKING}, the same substitution this port already uses
 *       consistently elsewhere (e.g. {@code EntityHunterChopper}, {@code EntityUFO}).</li>
 *   <li>{@code Biome.getRainfall()} (a continuous 0..~1 float in 1.12) - no public rainfall/downfall
 *       accessor on {@link Biome} was confirmed reachable in this sandbox (no compiled NeoForge jar
 *       available to verify against - same finding {@code com.hbm.world.feature.OilBubbleFeature}'s
 *       own javadoc already documented for the identical gap). This port substitutes {@link
 *       Biome#hasPrecipitation()} (a confirmed-real boolean, already used elsewhere in this port -
 *       see {@code HazardTypeHydroactive}), mapped to a fixed representative {@link #humidity} value
 *       for precipitating biomes. This is a deliberate, honestly-flagged fidelity reduction (CE's
 *       continuous rainfall gradient - e.g. jungle vs. plains - collapses to two humidity buckets
 *       here) affecting only the condensation-cloud density/spread of this purely cosmetic effect,
 *       not any gameplay value.</li>
 *   <li>{@code World.setLastLightningBolt(2)} (a pure client-side "flash the sky" call, no real bolt
 *       entity, CE's own vanilla-1.12 field for the ambient sky-flash render effect) - ported to the
 *       confirmed-real 1.21.1 equivalent {@link ClientLevel#setSkyFlashTime(int)} (same literal
 *       argument, {@code 2}, preserved per this port's ground rule that CE's own numbers are the
 *       source of truth) rather than this package's sibling {@code EntityCloudFleija}/{@code
 *       EntityCloudSolinium}/{@code EntityCloudTom}'s earlier real-{@code LightningBolt}-entity
 *       substitution (written before {@code setSkyFlashTime} had been confirmed reachable) - flagged
 *       in this task's own notes as a nicer substitution the coordinator may want those 3 sibling
 *       classes to adopt too, out of this task's own file-scope to apply itself.</li>
 *   <li>{@code MainRegistry.proxy.me()}/{@code .playSoundClient(...)} (CE's {@code @SidedProxy}
 *       indirection, used specifically so a common-loaded class never directly references
 *       client-only types) - this port has no equivalent proxy class to route through (and this
 *       task's ground rules forbid editing the one shared {@code ClientProxy.java} that exists).
 *       Instead, {@link #clientTick()} itself is annotated {@link OnlyIn}{@code (Dist.CLIENT)} -
 *       the same safe, already-established pattern this port's own {@code
 *       com.hbm.items.tool.ItemToolAbility}/{@code IKeybindReceiver} etc. already use for a
 *       common-loaded class that needs one clearly-bounded client-only method (NeoForge's
 *       {@code RuntimeDistCleaner} strips an {@code @OnlyIn}-annotated member's body on the wrong
 *       physical side, so referencing {@link Minecraft}/{@link ClientLevel}/{@link LocalPlayer}
 *       inside just this one method - never elsewhere in the class - is safe on a dedicated server).
 *       {@link #tick()} itself stays unannotated/dist-agnostic and only ever calls
 *       {@link #clientTick()} from inside an {@code isClientSide()} branch, so the call is also
 *       logically unreachable server-side even before the annotation is considered.</li>
 *   <li>{@code Cloudlet.getInterpPos(float)}/{@code getInterpColor(float)} and this class's own
 *       {@code getInterpColor(double, byte)} - confirmed (grep of {@code RenderTorex.java}) to have
 *       <b>zero callers anywhere in CE</b>; {@code RenderTorex} reimplements the identical
 *       interpolation inline via its own private static helpers instead of ever calling these public
 *       methods. Genuinely dead code, not a deferred feature - not ported, matching this class's own
 *       established convention of documenting deliberate exclusions rather than silently dropping
 *       them. {@code TorexRenderer} performs the same interpolation CE's renderer actually executes,
 *       inline, against {@link Cloudlet}'s raw {@code prevPosX/posX} etc. fields.</li>
 *   <li>{@code Cloudlet.renderSortDistanceSq}/{@code EntityNukeTorex.lastRenderSortTick} and {@code
 *       RenderTorex.sortCloudlets} (CE's manual per-frame back-to-front cloudlet sort, needed because
 *       CE's raw-GL immediate/instanced draw path has no automatic translucency ordering) - not
 *       ported. The 1.21.1 {@link net.minecraft.client.renderer.RenderType} this port's cloudlet
 *       quads draw through is built with {@code sortOnUpload = true} (see {@code TorexRenderer}),
 *       which gives the exact same "back-to-front for correct alpha blending" result automatically,
 *       making CE's hand-rolled sort machinery redundant rather than a missing feature.</li>
 * </ul>
 */
public class EntityNukeTorex extends Entity implements IConstantRenderer {

    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.BYTE);

    public static final int firstCondenseHeight = 130;
    public static final int secondCondenseHeight = 170;
    public static final int blastWaveHeadstart = 5;
    public static final int maxCloudlets = 20_000;

    // Nuke colors
    public static final double nr1 = 2.5;
    public static final double ng1 = 1.3;
    public static final double nb1 = 0.4;
    public static final double nr2 = 0.1;
    public static final double ng2 = 0.075;
    public static final double nb2 = 0.05;

    // Balefire colors
    public static final double br1 = 1;
    public static final double bg1 = 2;
    public static final double bb1 = 0.5;
    public static final double br2 = 0.1;
    public static final double bg2 = 0.1;
    public static final double bb2 = 0.1;

    public double coreHeight = 3;
    public double convectionHeight = 3;
    public double torusWidth = 3;
    public double rollerSize = 1;
    public double heat = 1;
    public double lastSpawnY = -1;
    /** CE: {@code ArrayList<Cloudlet> cloudlets} - public, read directly by {@code TorexRenderer}. */
    public final List<Cloudlet> cloudlets = new ArrayList<>();
    public float humidity = -1;

    public boolean didPlaySound = false;
    /**
     * CE writes this from {@code RenderTorex.doRender}, not from the entity's own update logic -
     * "a renderer mutating its target entity's field," a legal-but-unusual pattern preserved
     * deliberately here (see {@code TorexRenderer}'s own javadoc) rather than reworked into a
     * separate client-side map keyed by entity id.
     */
    public boolean didShake = false;

    public int maxAge = 1000;
    /**
     * CE's {@code ticksExisted}, tracked manually rather than relying on vanilla's
     * {@link Entity#tickCount} - see this fix's note on {@link #tick()}: this class's own
     * {@code tick()} never calls {@code super.tick()}/{@code baseTick()} (matching CE's own
     * onEntityUpdate() fully overriding the base class), and {@code tickCount} is only ever
     * incremented from inside {@code baseTick()}, so relying on it here left the despawn check below
     * permanently false - the same manual-counter pattern this file's sibling
     * {@code EntityCloudFleija}/{@code EntityCloudSolinium}/{@code EntityEMPBlast} already use, for
     * the same reason. Public (matching CE's own public {@code Entity.ticksExisted}) - read directly
     * by {@code TorexRenderer} for the flare/flash duration checks.
     */
    public int age;

    public EntityNukeTorex(EntityType<? extends EntityNukeTorex> entityType, Level level) {
        super(entityType, level);
        // CE: `this.ignoreFrustumCheck = true;` (EntityNukeTorex.java's own constructor) - paired
        // with the IConstantRenderer marker on this class; 1.21.1's equivalent field is
        // Entity#noCulling, matching this port's own already-established EntityMIRV/EntityMissile*
        // precedent for the identical CE flag on other IConstantRenderer entities. `isImmuneToFire`
        // (also set by CE's constructor) has no per-instance field to port here - it's already
        // covered by EffectEntityTypes' own `.fireImmune()` builder call on TOREX's EntityType.
        this.noCulling = true;
    }

    /** CE: {@code isInRangeToRenderDist(double) -> true}, unconditionally - this entity's visual footprint (a mushroom cloud) can legitimately exceed vanilla's normal render-distance cutoff. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0F);
        builder.define(TYPE, (byte) 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.contains("scale")) setScale(nbt.getFloat("scale"));
        if (nbt.contains("type")) this.entityData.set(TYPE, nbt.getByte("type"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putFloat("scale", this.entityData.get(SCALE));
        nbt.putByte("type", this.entityData.get(TYPE));
    }

    /** CE returns {@code false} from both {@code writeToNBTOptional}/{@code writeToNBTAtomically} - this ephemeral VFX entity is never meant to survive a save/load cycle. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        this.age++;

        if (level().isClientSide()) {
            clientTick();
        } else if (this.age > maxAge) {
            this.discard();
        }
    }

    /**
     * CE: {@code onUpdate()}'s entire {@code if (world.isRemote)} branch (the ~500-line cloudlet
     * simulation). See class javadoc for the full list of 1.12 to 1.21.1 API deltas applied
     * throughout, and for why this method is safe to annotate {@link OnlyIn}{@code (Dist.CLIENT)}
     * despite living on a class that also loads server-side.
     */
    @OnlyIn(Dist.CLIENT)
    private void clientTick() {
        double s = this.getScale();
        double cs = 1.5;
        if (this.age == 1) this.setScale((float) s);

        if (this.humidity == -1) {
            Biome biome = level().getBiome(this.blockPosition()).value();
            // CE: world.getBiome(pos).getRainfall() - see class javadoc's API-delta list for why
            // this substitutes a boolean-derived representative value instead.
            this.humidity = biome.hasPrecipitation() ? 0.7F : 0F;
        }

        if (this.lastSpawnY == -1) {
            this.lastSpawnY = getY() - 3;
        }

        int spawnTarget = Math.max(level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(getX()), (int) Math.floor(getZ())) - 3, 1);
        double moveSpeed = 0.5D;

        if (Math.abs(spawnTarget - lastSpawnY) < moveSpeed) {
            lastSpawnY = spawnTarget;
        } else {
            lastSpawnY += moveSpeed * Math.signum(spawnTarget - lastSpawnY);
        }

        // spawn mush clouds
        double range = (torusWidth - rollerSize) * 0.5;
        double simSpeed = getSimulationSpeed();
        int lifetime = Math.min((this.age * this.age) + 200, maxAge - this.age + 200);
        int toSpawn = (int) (0.6 * Math.min(Math.max(0, maxCloudlets - cloudlets.size()), Math.ceil(10 * simSpeed * simSpeed * Math.min(1, 1200 / (double) lifetime))));

        for (int i = 0; i < toSpawn; i++) {
            double x = getX() + this.random.nextGaussian() * range;
            double z = getZ() + this.random.nextGaussian() * range;
            Cloudlet cloud = new Cloudlet(x, lastSpawnY, z, (float) (this.random.nextDouble() * 2D * Math.PI), 0, lifetime);
            cloud.setScale((float) (Math.sqrt(s) * 3 + this.age * 0.0025 * s), (float) (Math.sqrt(s) * 3 + this.age * 0.0025 * 6 * cs * s));
            cloudlets.add(cloud);
        }

        if (this.age < 120 * s) {
            if (level() instanceof ClientLevel clientLevel) clientLevel.setSkyFlashTime(2);
        }

        // spawn shock clouds
        if (this.age < 150) {
            int cloudCount = Math.min(this.age * 2, 100);
            int shockLife = Math.max(400 - this.age * 20, 50);

            for (int i = 0; i < cloudCount; i++) {
                Vec3 vec = new Vec3((this.age + this.random.nextDouble() * 2) * 1.5, 0, 0);
                float rot = (float) (Math.PI * 2 * this.random.nextDouble());
                vec = vec.yRot(rot);
                this.cloudlets.add(new Cloudlet(vec.x + getX(), level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) (vec.x + getX()) + 1, (int) (vec.z + getZ())), vec.z + getZ(), rot, 0, shockLife, TorexType.SHOCK)
                        .setScale((float) s * 5F, (float) s * 2F).setMotion(Mth.clamp(0.25 * this.age - 5, 0, 1)));
            }

            if (!didPlaySound) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && player.distanceTo(this) < (this.age * 1.5 + 1) * 1.5) {
                    if (level() instanceof ClientLevel clientLevel) {
                        clientLevel.playLocalSound(getX(), getY(), getZ(), HBMSoundHandler.nuclearExplosion.get(), SoundSource.HOSTILE, 10_000F, 1F, false);
                    }
                    didPlaySound = true;
                }
            }
        }

        // spawn ring clouds
        if (this.age < 200) {
            lifetime = (int) (lifetime * s);
            for (int i = 0; i < 2; i++) {
                Cloudlet cloud = new Cloudlet(getX(), getY() + coreHeight, getZ(), (float) (this.random.nextDouble() * 2D * Math.PI), 0, lifetime, TorexType.RING);
                cloud.setScale((float) (Math.sqrt(s) * cs + this.age * 0.0015 * s), (float) (Math.sqrt(s) * cs + this.age * 0.0015 * 6 * cs * s));
                cloudlets.add(cloud);
            }
        }

        if (this.humidity > 0 && this.age < 220) {
            // spawn lower condensation clouds
            spawnCondensationClouds(this.age, this.humidity, firstCondenseHeight, 80, 4, s, cs);
            // spawn upper condensation clouds
            spawnCondensationClouds(this.age, this.humidity, secondCondenseHeight, 80, 2, s, cs);
        }

        for (int i = cloudlets.size() - 1; i >= 0; i--) {
            Cloudlet cloud = cloudlets.get(i);
            if (cloud.isDead) {
                cloudlets.remove(i);
                continue;
            }
            cloud.update();
        }

        coreHeight += 0.15;
        torusWidth += 0.05;
        rollerSize = torusWidth * 0.35;
        convectionHeight = coreHeight + rollerSize;

        int maxHeat = (int) (50 * s * s);
        heat = maxHeat - Math.pow((maxHeat * this.age) / maxAge, 0.6);
    }

    /** CE: {@code spawnCondensationClouds(int, float, int, int, int, double, double)}, verbatim aside from the API deltas documented on the class javadoc. */
    private void spawnCondensationClouds(int age, float humidity, int height, int count, int spreadAngle, double s, double cs) {
        if ((getY() + age) > height) {
            for (int i = 0; i < (int) (5 * humidity * count / (double) spreadAngle); i++) {
                for (int j = 1; j < spreadAngle; j++) {
                    float angle = (float) (Math.PI * 2 * this.random.nextDouble());
                    Vec3 vec = new Vec3(0, age, 0);
                    vec = vec.zRot((float) Math.acos((height - getY()) / (double) age) + (float) Math.toRadians(humidity * humidity * 90 * j * (0.1 * this.random.nextDouble() - 0.05)));
                    vec = vec.yRot(angle);
                    Cloudlet cloud = new Cloudlet(getX() + vec.x, getY() + vec.y, getZ() + vec.z, angle, 0, (int) ((20 + age / 10) * (1 + this.random.nextDouble() * 0.1)), TorexType.CONDENSATION);
                    cloud.setScale(3F * (float) (cs * s), 4F * (float) (cs * s));
                    cloudlets.add(cloud);
                }
            }
        }
    }

    public EntityNukeTorex setScale(float scale) {
        if (!level().isClientSide()) {
            this.entityData.set(SCALE, scale);
        }
        this.coreHeight = this.coreHeight * scale;
        this.convectionHeight = this.convectionHeight * scale;
        this.torusWidth = this.torusWidth * scale;
        this.rollerSize = this.rollerSize * scale;
        this.maxAge = (int) (45 * 20 * scale);
        return this;
    }

    public EntityNukeTorex setType(int type) {
        this.entityData.set(TYPE, (byte) type);
        return this;
    }

    public double getScale() {
        return this.entityData.get(SCALE);
    }

    // CE: EntityNukeTorex.getType() -> byte (a 0/1 color-scheme status code stored in the TYPE
    // synced data value, unrelated to vanilla's own Entity#getType() -> EntityType<?>). Renamed to
    // getTorexType() so this class's own byte accessor no longer collides with - and fails to
    // override - vanilla's real getType() (confirmed real/1.21.1 by javac's own "getType() in
    // EntityNukeTorex cannot override getType() in Entity... return type byte is not compatible
    // with EntityType<?>" error against the old name).
    public byte getTorexType() {
        return this.entityData.get(TYPE);
    }

    public double getSimulationSpeed() {
        int simSlow = maxAge / 4;
        int life = this.age;

        if (life > maxAge) {
            return 0D;
        }
        if (life > simSlow) {
            return 1D - ((double) (life - simSlow) / (double) (maxAge - simSlow));
        }
        return 1.0D;
    }

    /** CE: {@code EntityNukeTorex.getAlpha()} - the whole-cloud fade-out factor. Also used directly by {@code TorexRenderer} instead of that class's own now-redundant duplicate (CE's {@code RenderTorex.getCloudAlphaBase} computed the identical formula a second time - consolidated here, not a behavior change). */
    public float getAlpha() {
        int fadeOut = maxAge * 3 / 4;
        int life = this.age;

        if (life > fadeOut) {
            float fac = (float) (life - fadeOut) / (float) (maxAge - fadeOut);
            return 1F - fac;
        }
        return 1.0F;
    }

    /**
     * A single cloudlet ("mush cloud" quad) belonging to this Torex. Ported verbatim from CE's
     * inner class of the same name - its own motion/color math operates entirely on raw doubles
     * (no {@code Vec3NT} usage at all inside {@link #update()} and its helpers), so only the outer
     * class's position reads needed the {@code getX()}/{@code getY()}/{@code getZ()} API-shape
     * delta documented on the enclosing class's javadoc.
     */
    public class Cloudlet {

        public double posX;
        public double posY;
        public double posZ;
        public double prevPosX;
        public double prevPosY;
        public double prevPosZ;
        public double motionX;
        public double motionY;
        public double motionZ;
        public int age;
        public int cloudletLife;
        public float angle;
        public boolean isDead = false;
        float rangeMod = 1.0F;
        public float colorMod = 1.0F;
        public double colorR;
        public double colorG;
        public double colorB;
        public double prevColorR;
        public double prevColorG;
        public double prevColorB;
        public TorexType type;
        public float startingScale = 3F;
        public float growingScale = 5F;
        private double computedMotionX;
        private double computedMotionY;
        private double computedMotionZ;

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge) {
            this(posX, posY, posZ, angle, age, maxAge, TorexType.STANDARD);
        }

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge, TorexType type) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.age = age;
            this.cloudletLife = maxAge;
            this.angle = angle;
            RandomSource rand = EntityNukeTorex.this.random;
            this.rangeMod = 0.3F + rand.nextFloat() * 0.7F;
            this.colorMod = 0.8F + rand.nextFloat() * 0.2F;
            this.type = type;

            this.updateColor();
        }

        private final double motionConvectionMult = 0.5F;
        private final double motionLiftMult = 0.625F;
        private final double motionRingMult = 0.5F;
        private final double motionCondensationMult = 1F;
        private final double motionShockwaveMult = 1F;
        private double motionMult = 1F;

        private void update() {
            age++;

            if (age > cloudletLife) {
                this.isDead = true;
            }

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            double simDeltaX = EntityNukeTorex.this.getX() - this.posX;
            double simDeltaZ = EntityNukeTorex.this.getZ() - this.posZ;
            double simPosX = EntityNukeTorex.this.getX() + Math.sqrt(simDeltaX * simDeltaX + simDeltaZ * simDeltaZ);

            if (this.type == TorexType.STANDARD) {
                getConvectionMotion(simPosX);
                double convectionX = this.computedMotionX;
                double convectionY = this.computedMotionY;
                double convectionZ = this.computedMotionZ;
                getLiftMotion(simPosX);

                double factor = Mth.clamp((this.posY - EntityNukeTorex.this.getY()) / EntityNukeTorex.this.coreHeight, 0, 1);
                double inverseFactor = 1D - factor;
                this.motionX = convectionX * factor + this.computedMotionX * inverseFactor;
                this.motionY = convectionY * factor + this.computedMotionY * inverseFactor;
                this.motionZ = convectionZ * factor + this.computedMotionZ * inverseFactor;
            } else if (this.type == TorexType.RING) {
                getRingMotion(simPosX);
                this.motionX = this.computedMotionX;
                this.motionY = this.computedMotionY;
                this.motionZ = this.computedMotionZ;
            } else if (this.type == TorexType.CONDENSATION) {
                getCondensationMotion();
                this.motionX = this.computedMotionX;
                this.motionY = this.computedMotionY;
                this.motionZ = this.computedMotionZ;
            } else if (this.type == TorexType.SHOCK) {
                getShockwaveMotion();
                this.motionX = this.computedMotionX;
                this.motionY = this.computedMotionY;
                this.motionZ = this.computedMotionZ;
            }

            double mult = this.motionMult * getSimulationSpeed();

            this.posX += this.motionX * mult;
            this.posY += this.motionY * mult;
            this.posZ += this.motionZ * mult;

            this.updateColor();
        }

        private void getCondensationMotion() {
            double speed = motionCondensationMult * EntityNukeTorex.this.getScale() * 0.125D;
            setNormalizedMotion(this.posX - EntityNukeTorex.this.getX(), 0D, this.posZ - EntityNukeTorex.this.getZ(), speed);
        }

        private void getShockwaveMotion() {
            double speed = motionShockwaveMult * EntityNukeTorex.this.getScale() * 0.25D;
            setNormalizedMotion(this.posX - EntityNukeTorex.this.getX(), 0D, this.posZ - EntityNukeTorex.this.getZ(), speed);
        }

        private void getRingMotion(double simPosX) {
            if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }

            double torusPosX = EntityNukeTorex.this.getX() + torusWidth;
            double torusPosY = EntityNukeTorex.this.getY() + coreHeight * 0.5D;

            double deltaX = torusPosX - simPosX;
            double deltaY = torusPosY - this.posY;

            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod * 0.25D;
            double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / roller - 1D;

            double func = 1D - Math.exp(-dist);
            float angle = (float) (func * Math.PI * 0.5D); // [0;90 deg]

            double rotX = -deltaX / dist;
            double rotY = -deltaY / dist;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            double rotatedX = rotX * cos + rotY * sin;
            double rotatedY = rotY * cos - rotX * sin;

            setNormalizedMotion(torusPosX + rotatedX - simPosX, torusPosY + rotatedY - this.posY, 0D, motionRingMult * 0.5D);
            rotateComputedMotionAroundY();
        }

        /* simulated on a 2D-plane along the X/Y axis */
        private void getConvectionMotion(double simPosX) {
            if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }

            double torusPosX = EntityNukeTorex.this.getX() + torusWidth;
            double torusPosY = EntityNukeTorex.this.getY() + coreHeight;

            double deltaX = torusPosX - simPosX;
            double deltaY = torusPosY - this.posY;

            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod;
            double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / roller - 1D;

            double func = 1D - Math.exp(-dist);
            float angle = (float) (func * Math.PI * 0.5D); // [0;90 deg]

            double rotX = -deltaX / dist;
            double rotY = -deltaY / dist;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            double rotatedX = rotX * cos + rotY * sin;
            double rotatedY = rotY * cos - rotX * sin;

            setNormalizedMotion(torusPosX + rotatedX - simPosX, torusPosY + rotatedY - this.posY, 0D, motionConvectionMult);
            rotateComputedMotionAroundY();
        }

        private void getLiftMotion(double simPosX) {
            double scale = Mth.clamp(1D - (simPosX - (EntityNukeTorex.this.getX() + torusWidth)), 0, 1) * motionLiftMult;

            setNormalizedMotion(
                    EntityNukeTorex.this.getX() - this.posX,
                    (EntityNukeTorex.this.getY() + convectionHeight) - this.posY,
                    EntityNukeTorex.this.getZ() - this.posZ,
                    scale);
        }

        private void setComputedMotion(double x, double y, double z) {
            this.computedMotionX = x;
            this.computedMotionY = y;
            this.computedMotionZ = z;
        }

        private void setNormalizedMotion(double x, double y, double z, double speed) {
            double lengthSq = x * x + y * y + z * z;
            if (lengthSq < 1.0E-8D) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }

            double scale = speed / Math.sqrt(lengthSq);
            setComputedMotion(x * scale, y * scale, z * scale);
        }

        private void rotateComputedMotionAroundY() {
            float cos = Mth.cos(this.angle);
            float sin = Mth.sin(this.angle);
            double motionX = this.computedMotionX;
            double motionZ = this.computedMotionZ;
            this.computedMotionX = motionX * cos + motionZ * sin;
            this.computedMotionZ = motionZ * cos - motionX * sin;
        }

        private void updateColor() {
            this.prevColorR = this.colorR;
            this.prevColorG = this.colorG;
            this.prevColorB = this.colorB;

            double exX = EntityNukeTorex.this.getX();
            double exY = EntityNukeTorex.this.getY() + EntityNukeTorex.this.coreHeight;
            double exZ = EntityNukeTorex.this.getZ();

            double distX = exX - posX;
            double distY = exY - posY;
            double distZ = exZ - posZ;

            double distSq = distX * distX + distY * distY + distZ * distZ;
            distSq /= this.type == TorexType.SHOCK ? EntityNukeTorex.this.heat * 3 : EntityNukeTorex.this.heat;

            double col = 2D / Math.max(distSq, 1); // col goes from 2-0

            byte type = EntityNukeTorex.this.getTorexType();
            if (type == 0) {
                this.colorR = nr2 + (nr1 - nr2) * col;
                this.colorG = ng2 + (ng1 - ng2) * col;
                this.colorB = nb2 + (nb1 - nb2) * col;
                return;
            }

            this.colorR = br2 + (br1 - br2) * col;
            this.colorG = bg2 + (bg1 - bg2) * col;
            this.colorB = bb2 + (bb1 - bb2) * col;
        }

        public float getAlpha() {
            float alpha = (1F - ((float) age / (float) cloudletLife)) * EntityNukeTorex.this.getAlpha();
            if (this.type == TorexType.CONDENSATION) alpha *= 0.25;
            return Mth.clamp(alpha, 0.0001F, 1F);
        }

        public float getScale() {
            return startingScale + ((float) age / (float) cloudletLife) * growingScale;
        }

        public Cloudlet setScale(float start, float grow) {
            this.startingScale = start;
            this.growingScale = grow;
            return this;
        }

        public Cloudlet setMotion(double mult) {
            this.motionMult = mult;
            return this;
        }
    }

    public enum TorexType {
        STANDARD,
        RING,
        CONDENSATION,
        SHOCK
    }

    /**
     * Spawns a standard Torex. Matches {@code HazardTypeUnstable}'s existing call sites exactly.
     */
    public static void statFac(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(EffectEntityTypes.TOREX.get(), level)
                .setScale(Mth.clamp(scale * 0.01F, 0.25F, 5F));
        torex.setPos(x, y, z);
        level.addFreshEntity(torex);
    }

    /**
     * Spawns a Torex, balefire variant.
     */
    public static void statFacBale(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(EffectEntityTypes.TOREX.get(), level)
                .setScale(Mth.clamp(scale * 0.01F, 0.25F, 5F))
                .setType(1);
        torex.setPos(x, y, z);
        level.addFreshEntity(torex);
    }
}
