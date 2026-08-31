package com.hbm.entity.mob;

import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.projectile.ChopperMineEntityTypes;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityChopperMine;
import com.hbm.entity.projectile.LegacyMobBulletConfigs;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.special.SpecialItems;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityHunterChopper} (442 lines, {@code extends
 * EntityFlying implements IMob, IRadiationImmune}, read in full - CE's own top-of-file comment
 * preserved by both research reports: <i>"Drillgon200: This whole thing is messed up and janky and I
 * don't know what to about it."</i>) - see {@code docs/phase4/entities_bosses.md}'s Hunter Chopper row
 * and {@code docs/phase4/entities_vehicles_aircraft.md}'s movement supplement. 750 HP, purple boss bar
 * + {@code setDarkenScreen(true)} (1.21.1's renamed {@code BossEvent} setter for CE's
 * {@code setDarkenSky(true)}).
 * <p>
 * <b>Movement - accumulating impulse, not reset-then-set</b> ({@link #updateMovement()}): unlike
 * {@link EntityUFO}, CE's {@code onUpdate} <em>adds</em> a small (0.1D-scaled) unit-vector impulse
 * toward the waypoint via {@code +=} once every {@code courseChangeCooldown} (2-6 ticks), so the
 * chopper carries real momentum/drag between impulses. A separate {@link #updateRotation()} banking
 * calculation drives visible yaw/pitch independent of the actual motion math.
 * <p>
 * <b>Two confirmed, self-contained CE bugs fixed here</b> (both from this exact class, matching this
 * port's established "document and fix, don't preserve dead/broken logic" precedent already used by
 * {@code EntityMaskMan#prevHealth}):
 * <ol>
 *     <li>{@code onUpdate}'s rotation-smoothing has 3 branches: {@code >= 10} steps down,
 *     {@code <= -10} steps up, and a third {@code "< 10 && > 10"} branch meant to snap directly once
 *     close enough - a condition that can never be true for any float (nothing is both {@code <10} and
 *     {@code >10}), so CE's yaw genuinely never converges, only oscillates in +-10-degree steps forever.
 *     Fixed here to clamp the per-tick turn to +-10 degrees toward the true target angle instead
 *     (the evident intent), via {@link Mth#wrapDegrees}/{@link Mth#clamp}.</li>
 *     <li>{@code attackEntityFrom}'s invulnerability gate ({@code source instanceof
 *     EntityDamageSource -> return false}) is unconditional - and CE's own {@code ModDamageSource.
 *     causeBulletDamage}/{@code causeTauDamage} (used by every gun in the mod, including this port's
 *     own Sedna framework's standard hit lambda, which always attributes a shooter entity) construct
 *     an {@code EntityDamageSourceIndirect}, which itself extends {@code EntityDamageSource}. Read
 *     literally, this blocks <em>all</em> entity-attributed damage - melee <em>and</em> every ranged
 *     weapon in the mod - leaving only bare environmental damage (fire tick, drowning, out-of-world)
 *     able to hurt this "boss-tier hostile" at all, which cannot be the tested, shipped behavior for a
 *     mob meant to be shot down. Not reproduced: this port's {@link #hurt} keeps the 90%-damage-
 *     reduction-except-big-sources logic and the already-dying/health-clamp guards, but drops the
 *     blanket entity-source immunity line.</li>
 * </ol>
 * <p>
 * <b>Crash/dying state machine</b> ({@link #updateDying()}): {@link #hurt} intercepts a would-be-lethal
 * hit, clamps health to 0.1, and flips {@link #isDying} - from then on, manual gravity
 * ({@code motionY -= 0.08}/tick, {@code EntityFlying}/{@link FlyingMob} applies none on its own),
 * horizontal speed floored at 1.8 blocks/tick, periodic small explosions, until ground impact triggers
 * a final 15-block explosion + wreckage loot ({@link SpecialItems}'s
 * {@code CHOPPER_HEAD/TORSO/WING/TAIL/GUN/BLADES}, this task's own registrations). No achievement is
 * granted on death (confirmed absent from CE's own file).
 * <p>
 * <b>{@link EntityChopperMine} proximity mines</b>: read in full from CE (145 lines, small and
 * self-contained per this task's own instruction) and ported alongside this class - see that class's
 * own javadoc.
 * <p>
 * <b>Review-pass fix - no chopper-on-chopper friendly fire</b>: CE's own {@code Library.
 * getClosestEntityForChopper} explicitly excludes {@code instanceof EntityHunterChopper} (never target
 * another chopper) and any player with {@code capabilities.disableDamage} (creative <em>and</em>
 * spectator in vanilla 1.12) - {@link #retarget()} previously only excluded itself and creative players,
 * matching {@link EntityUFO}'s identical, separately-fixed gap. Fixed to exclude other choppers and
 * spectators too.
 */
public class EntityHunterChopper extends FlyingMob implements Enemy, IRadiationImmune {

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private double waypointX;
    private double waypointY;
    private double waypointZ;
    private int courseChangeCooldown;
    @Nullable
    private LivingEntity targetedEntity;
    private int attackCounter;
    private int mineDropCounter;
    private boolean isDying = false;

    public EntityHunterChopper(EntityType<? extends EntityHunterChopper> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 500;
        this.bossEvent.setDarkenScreen(true);
    }

    /** CE: {@code applyEntityAttributes} - {@code MAX_HEALTH = 750}. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 750.0D)
                .add(Attributes.FOLLOW_RANGE, 250.0D);
    }

    // NOTE: CE's isNonBoss() -> false override has no confirmed 1.21.1 Mob/LivingEntity equivalent
    // method (not found on any vanilla class this port has cross-referenced) - not reproduced, see
    // this task's knownGaps. The boss bar itself (the part of "counts as a boss" that actually matters
    // for players) is fully wired below regardless.

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** See class javadoc "Two confirmed, self-contained CE bugs fixed here" #2. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || this.getHealth() <= 0.1F) return false;

        boolean bigDamage = source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(ModDamageTypes.TAU)
                || source.is(ModDamageTypes.SHRAPNEL)
                || source.is(ModDamageTypes.NUCLEAR_BLAST)
                || source.is(ModDamageTypes.BLACK_HOLE)
                || source.is(ModDamageTypes.SUBATOMIC_1) || source.is(ModDamageTypes.SUBATOMIC_2)
                || source.is(ModDamageTypes.SUBATOMIC_3) || source.is(ModDamageTypes.SUBATOMIC_4)
                || source.is(ModDamageTypes.SUBATOMIC_5);
        if (!bigDamage) amount *= 0.1F;

        if (amount >= this.getHealth()) {
            this.initDeath();
            this.setHealth(0.1F);
            return false;
        }

        if (!this.isDying && this.random.nextInt(15) == 0) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 5F, Level.ExplosionInteraction.MOB);
            this.dropDamageItem();
        }

        return super.hurt(source, amount);
    }

    /** See {@link EntityUFO#travel(Vec3)}'s identical javadoc note on why this is fully overridden. */
    @Override
    public void travel(Vec3 relative) {
        if (this.isControlledByLocalInstance()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
    }

    @Override
    protected void customServerAiStep() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }

        if (!this.isDying) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), HBMSoundHandler.nullChopper.get(), SoundSource.HOSTILE, 10.0F, 0.5F);
            updateMovement();
            updateAttack();
            updateRotation();
        } else {
            updateDying();
        }

        this.bossEvent.setProgress(this.getMaxHealth() > 0F ? this.getHealth() / this.getMaxHealth() : 0F);
    }

    private void updateMovement() {
        double dx = this.waypointX - this.getX();
        double dy = this.waypointY - this.getY();
        double dz = this.waypointZ - this.getZ();
        double d3 = dx * dx + dy * dy + dz * dz;

        if (d3 < 1.0D || d3 > 3600.0D) {
            repickWaypoint(this.targetedEntity != null ? this.targetedEntity.getX() : this.getX(),
                    this.targetedEntity != null ? this.targetedEntity.getZ() : this.getZ());
            dx = this.waypointX - this.getX();
            dy = this.waypointY - this.getY();
            dz = this.waypointZ - this.getZ();
            d3 = dx * dx + dy * dy + dz * dz;
        }

        if (this.courseChangeCooldown-- <= 0) {
            this.courseChangeCooldown += this.random.nextInt(5) + 2;
            double dist = Math.sqrt(d3);

            if (isCourseTraversable(dist)) {
                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(motion.add(dx / dist * 0.1D, dy / dist * 0.1D, dz / dist * 0.1D));
            } else {
                repickWaypoint(this.getX(), this.getZ());
            }
        }
    }

    private void repickWaypoint(double baseX, double baseZ) {
        this.waypointX = baseX + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
        this.waypointZ = baseZ + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
        this.waypointY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) this.waypointX, (int) this.waypointZ)
                + 10 + this.random.nextInt(15);
    }

    private boolean isCourseTraversable(double len) {
        if (len < 1.0E-4) return true;
        double dx = (this.waypointX - this.getX()) / len;
        double dy = (this.waypointY - this.getY()) / len;
        double dz = (this.waypointZ - this.getZ()) / len;
        AABB box = this.getBoundingBox();

        for (int i = 1; i < len; i++) {
            box = box.move(dx, dy, dz);
            if (!this.level().noCollision(this, box)) return false;
        }
        return true;
    }

    private void updateAttack() {
        if (this.targetedEntity != null && (!this.targetedEntity.isAlive())) {
            this.targetedEntity = null;
        }

        if (this.targetedEntity == null || this.attackCounter <= 0) {
            retarget();
        }

        double range = 64.0D;
        if (this.targetedEntity != null && this.targetedEntity.distanceToSqr(this) < range * range) {
            this.attackCounter++;
            if (this.attackCounter >= 200) this.attackCounter -= 200;

            if (this.attackCounter % 2 == 0 && this.attackCounter >= 120) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), HBMSoundHandler.osiprShoot.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
                EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this.level(), LegacyMobBulletConfigs.CHOPPER_BULLET, this, this.targetedEntity, 3.0F, 0.05F);
                this.level().addFreshEntity(bullet);
            }
            if (this.attackCounter == 80) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), HBMSoundHandler.chopperCharge.get(), SoundSource.HOSTILE, 5.0F, 1.0F);
            }

            this.mineDropCounter++;
            if (this.mineDropCounter > 100 && this.random.nextInt(15) == 0) {
                dropMines();
                this.mineDropCounter = 0;
            }
        } else {
            this.attackCounter = 0;
        }
    }

    /** CE: {@code Library.getClosestEntityForChopper} (250-block range, sneaking halves-ish the
     *  effective range to 80%, excludes creative-mode players). */
    private void retarget() {
        double radius = 250D;
        LivingEntity best = null;
        double bestDistSq = -1D;

        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius))) {
            // CE: getClosestEntityForChopper explicitly excludes `instanceof EntityHunterChopper` (never
            // target another chopper) and any player with capabilities.disableDamage set (creative AND
            // spectator in vanilla 1.12) - both preserved here; the port previously only excluded itself
            // and creative players, letting multiple choppers snipe each other and letting spectators be
            // "targeted" (harmlessly, but not CE-accurate).
            if (e == this || e instanceof EntityHunterChopper || !e.isAlive()) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;

            double effectiveRadius = e.isShiftKeyDown() ? radius * 0.8D : radius;
            double distSq = e.distanceToSqr(this);
            if (distSq > effectiveRadius * effectiveRadius) continue;

            if (bestDistSq < 0 || distSq < bestDistSq) {
                bestDistSq = distSq;
                best = e;
            }
        }

        this.targetedEntity = best;
    }

    private void dropMines() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), HBMSoundHandler.chopperDrop.get(), SoundSource.HOSTILE, 15.0F, 1.0F);
        spawnMine(0, 0);
        if (this.random.nextInt(3) == 0) {
            spawnMine(1, 0);
            spawnMine(0, 1);
            spawnMine(-1, 0);
            spawnMine(0, -1);
        }
    }

    private void spawnMine(double mx, double mz) {
        EntityChopperMine mine = new EntityChopperMine(ChopperMineEntityTypes.CHOPPER_MINE.get(), this.level(),
                this.getX(), this.getY() - 0.5D, this.getZ(), mx, -0.3D, mz, this);
        this.level().addFreshEntity(mine);
    }

    /** See class javadoc "Two confirmed, self-contained CE bugs fixed here" #1. */
    private void updateRotation() {
        Vec3 motion = this.getDeltaMovement();
        float targetYaw;
        if (this.targetedEntity == null) {
            targetYaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        } else {
            targetYaw = (float) (Math.atan2(this.getX() - this.targetedEntity.getX(), this.getZ() - this.targetedEntity.getZ()) * 180.0D / Math.PI);
        }
        float turn = Mth.clamp(Mth.wrapDegrees(targetYaw - this.getYRot()), -10F, 10F);
        this.setYRot(this.getYRot() + turn);
        this.yRotO = this.getYRot();

        float horiz = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float pitch = (float) (Math.atan2(motion.y, horiz) * 180.0D / Math.PI);
        this.setXRot(pitch);
        this.xRotO = pitch;

        // CE: a render-model workaround dead-zone - never let rotationPitch sit inside 30-330 degrees.
        if (this.getXRot() <= 330F && this.getXRot() >= 30F) {
            this.setXRot(this.getXRot() < 180F ? 30F : 330F);
        }
    }

    private void updateDying() {
        Vec3 motion = this.getDeltaMovement().add(0, -0.08D, 0);
        double horizSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizSpeed * 1.2D < 1.8D) {
            motion = new Vec3(motion.x * 1.2D, motion.y, motion.z * 1.2D);
        }
        this.setDeltaMovement(motion);

        if (this.random.nextInt(20) == 0) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 5F, Level.ExplosionInteraction.MOB);
        }

        this.setYRot(this.getYRot() + 20F);

        if (this.onGround()) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 15F, Level.ExplosionInteraction.MOB);
            dropItems();
            this.discard();
        }

        if (this.tickCount % 2 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), HBMSoundHandler.nullCrashing.get(), SoundSource.HOSTILE, 10.0F, 0.5F);
        }
    }

    /** CE: {@code initDeath()}. Public - {@link #hurt} calls this on a would-be-lethal hit. */
    public void initDeath() {
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 10F, Level.ExplosionInteraction.MOB);
        if (!this.isDying) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), HBMSoundHandler.chopperDamage.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
        }
        this.isDying = true;
    }

    /**
     * CE: {@code dropDamageItem()} - a 1-in-10-weighted roll among {@code combine_scrap} (60%),
     * {@code plate_combine_steel} (20%), {@code wire_fine} magtung (20%). Only the middle case is
     * reproduced - {@code combine_scrap}/{@code wire_fine} are not registered anywhere in this port yet
     * (same documented items-scope gap as {@link EntityTaintCrab}'s coil drops).
     */
    public void dropDamageItem() {
        int i = this.random.nextInt(10);
        if (i >= 6 && i <= 7) {
            this.spawnAtLocation(new ItemStack(com.hbm.items.PlateCrystalWasteItems.PLATE_COMBINE_STEEL.get()));
        }
    }

    /** CE: {@code dropItems()} - the wreckage loot table. */
    protected void dropItems() {
        if (this.random.nextInt(2) == 0) this.spawnAtLocation(new ItemStack(SpecialItems.CHOPPER_HEAD.get()));
        if (this.random.nextInt(2) == 0) this.spawnAtLocation(new ItemStack(SpecialItems.CHOPPER_TORSO.get()));
        if (this.random.nextInt(2) == 0) this.spawnAtLocation(new ItemStack(SpecialItems.CHOPPER_WING.get()));
        if (this.random.nextInt(3) == 0) this.spawnAtLocation(new ItemStack(SpecialItems.CHOPPER_TAIL.get()));
        if (this.random.nextInt(3) == 0) this.spawnAtLocation(new ItemStack(SpecialItems.CHOPPER_GUN.get()));
        if (this.random.nextInt(3) == 0) this.spawnAtLocation(new ItemStack(SpecialItems.CHOPPER_BLADES.get()));

        this.spawnAtLocation(new ItemStack(com.hbm.items.PlateCrystalWasteItems.PLATE_COMBINE_STEEL.get(), this.random.nextInt(5) + 1));
        // combine_scrap (0-8) / wire_fine magtung (1-3) not registered in this port yet - see class javadoc.
    }

    @Override
    public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }
}
