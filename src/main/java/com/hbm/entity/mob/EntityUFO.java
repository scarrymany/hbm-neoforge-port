package com.hbm.entity.mob;

import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.LegacyMobBulletConfigs;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.special.SpecialItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.AdvancementManager;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityUFO} (467 lines, {@code extends EntityFlying
 * implements IMob, IRadiationImmune}, read in full) - see {@code docs/phase4/entities_bosses.md}'s
 * UFO row (health/attack/death/loot/spawn) and {@code docs/phase4/entities_vehicles_aircraft.md}'s UFO
 * movement-supplement row (the exact motion math). <b>Not</b> built on {@code EntityUFOBase} - that
 * class backs {@code EntityFBIDrone} instead, a separate, unrelated, out-of-scope mob (confirmed by
 * the bosses report's Headline finding #3).
 * <p>
 * <b>{@link FlyingMob}</b> is this port's confirmed 1.21.1 analogue of CE's {@code EntityFlying} (per
 * both research reports' own explicit flag: well-established Mojang-mapping knowledge from vanilla's
 * {@code Ghast}/{@code Phantom}, <em>not</em> independently verified against a compiled jar in this
 * sandbox - see this task's knownGaps). {@link #travel(Vec3)} is fully overridden below regardless of
 * {@link FlyingMob}'s own default behavior, so this class does not actually depend on that class's
 * exact {@code travel()} contract being right - only on {@code FlyingMob}/{@code Mob} existing and
 * providing the AI/attribute/goal scaffolding.
 * <p>
 * <b>Movement - "dart then hang motionless," not smooth flight</b> ({@link #customServerAiStep()}):
 * CE's {@code updateAITasks} zeroes motion every tick, then - only while
 * {@link #courseChangeCooldown} &gt; 0 - recomputes a unit vector toward {@link #waypoint} at a flat
 * speed (5 blocks/tick chasing a {@link Player}, 2 otherwise), gated by {@link #isCourseTraversable}
 * (an AABB-sweep block-collision check, ported onto {@link Level#noCollision(Entity, AABB)} - the
 * modern equivalent of CE's raw {@code world.getCollisionBoxes(this, aabb).isEmpty()} check). Once the
 * cooldown expires or it arrives within 5 blocks, it hovers motionless until the next 50-tick target
 * rescan picks a fresh waypoint 35 blocks past the target (with a 2-in-3 chance of approaching from a
 * random angle instead of a straight line) - reproduced in {@link #pickWaypoint()} exactly.
 * <p>
 * <b>Attack cycle</b>: {@code tickCount % 300 < 200} -&gt; laser volleys twice every 4 ticks (a
 * primary-target shot every 2 ticks, alternating with a rotating-secondary-target shot - net effect:
 * "every 2 ticks" per this task's own framing); otherwise -&gt; rocket volleys twice every 20 ticks
 * (net "every 10 ticks"). Both fire through {@link LegacyMobBulletConfigs#WORM_LASER}/{@code
 * #UFO_ROCKET} via {@link EntityBulletBaseMK4}'s mob-aim-at-target constructor - the same pattern
 * {@code EntityBOTPrimeBase#laserAttack} already established for the worm boss (CE genuinely reuses
 * {@code WORM_LASER} for both bosses - a real content-reuse, not a bug, per the bosses report).
 * <p>
 * <b>Abduction beam</b> ({@link #updateBeam()}): within 25 blocks XZ (CE's own Manhattan-style
 * {@code abs(dx)+abs(dz)} check, preserved exactly, not corrected to Euclidean), raycasts straight down
 * to the first non-air block and deals 1000 damage + 5s fire + 5-point
 * {@link ContaminationUtil#contaminate} radiation to every entity in that column - all three effects
 * already-real, zero missing dependency.
 * <p>
 * <b>Death sequence</b> ({@link #die}): CE's {@code onDeathUpdate} fires this at {@code deathTime==19}
 * (a ~1-second delay purely for death-animation timing); this port fires it immediately from
 * {@code die()} instead, matching this port's own established convention
 * ({@code EntityBOTPrimeHead#die}/{@code EntityCreeperNuclear#die}) rather than reimplementing a
 * death-tick timer for a cosmetic delay with no gameplay effect.
 * <p>
 * <b>No spawn egg</b> - CE's own {@code @AutoRegister(name = "entity_ntm_ufo", trackingRange = 1000)}
 * carries no {@code eggColors}, unlike every other entity in this file's sibling classes; this port's
 * registry (see {@code Phase4BossEntityTypes2}) faithfully omits a spawn egg for this one entity to
 * match - the only spawn path is {@code ItemChopper}'s {@code spawn_ufo} variant, per design.
 * <p>
 * <b>Review-pass fix - no UFO-on-UFO friendly fire</b>: CE's own {@code canAttackClass(Class)} excludes
 * {@code entityClass == this.getClass()} from both target scanning and the abduction beam's damage pass
 * (so multiple UFOs never snipe or beam each other) - this was missing from {@link #scanTargets()}/
 * {@link #updateBeam()} (which only excluded {@code this} itself, not other {@code EntityUFO} instances),
 * unlike this same package's {@link EntityCyberCrab#TARGET_EXCLUSION}, which already excludes its own
 * family correctly. Fixed to match CE and that established sibling convention.
 */
public class EntityUFO extends FlyingMob implements Enemy, IRadiationImmune {

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    private int courseChangeCooldown;
    private int scanCooldown;
    private int hurtCooldown;
    private int beamTimer;
    private boolean beam;
    /** Set by {@code ItemChopper}'s {@code spawn_ufo} variant (CE: {@code scanCooldown = 100}). */
    public int initialScanCooldown = 0;
    private BlockPos waypoint = BlockPos.ZERO;
    @Nullable
    private Entity target;
    private final List<Entity> secondaries = new ArrayList<>();

    public EntityUFO(EntityType<? extends EntityUFO> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 500;
    }

    /** CE: {@code applyEntityAttributes} - {@code MAX_HEALTH = 20000}. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20000.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    /** CE: {@code canDespawn() { return false; }} */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** CE: a 5-tick post-hit i-frame, on top of (not instead of) vanilla's own invulnerable-time window. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.hurtCooldown > 0) return false;
        boolean hit = super.hurt(source, amount);
        if (hit) this.hurtCooldown = 5;
        return hit;
    }

    /**
     * CE never overrides a {@code travel()}-shaped method (Headline finding #4 of the vehicles
     * report) - it hand-sets {@code motionX/Y/Z} in {@code updateAITasks} and lets {@code EntityFlying}
     * apply them. Overridden fully here (rather than relying on {@link FlyingMob}'s own unverified
     * default) to guarantee that same "motion is applied as-is, no gravity, no re-derivation from
     * strafing inputs" contract regardless of the parent class's real 1.21.1 behavior.
     */
    @Override
    public void travel(Vec3 relative) {
        if (this.isControlledByLocalInstance()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
    }

    @Override
    protected void customServerAiStep() {
        Level level = this.level();

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }

        if (this.initialScanCooldown > 0) {
            this.scanCooldown = this.initialScanCooldown;
            this.initialScanCooldown = 0;
        }

        if (this.hurtCooldown > 0) this.hurtCooldown--;
        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;

        if (this.target != null && !this.target.isAlive()) this.target = null;

        if (this.scanCooldown <= 0) {
            scanTargets();
            this.scanCooldown = 50;
        }

        if (this.target != null && this.courseChangeCooldown <= 0) {
            pickWaypoint();
        }

        updateBeam();
        updateAttacks();

        Vec3 motion = Vec3.ZERO;
        if (this.courseChangeCooldown > 0) {
            double dx = this.waypoint.getX() - this.getX();
            double dy = this.waypoint.getY() - this.getY();
            double dz = this.waypoint.getZ() - this.getZ();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double speed = (this.target instanceof Player) ? 5D : 2D;

            if (len > 5D) {
                if (isCourseTraversable(len)) {
                    motion = new Vec3(dx / len * speed, dy / len * speed, dz / len * speed);
                } else {
                    this.courseChangeCooldown = 0;
                }
            }
        }
        this.setDeltaMovement(motion);

        this.bossEvent.setProgress(this.getMaxHealth() > 0F ? this.getHealth() / this.getMaxHealth() : 0F);
    }

    private void scanTargets() {
        AABB box = this.getBoundingBox().inflate(100, 50, 100);
        this.secondaries.clear();
        this.target = null;

        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
            // CE: canAttackClass(entityClass) excludes entityClass == this.getClass() (i.e. never target
            // another EntityUFO) alongside the bullet-class exclusion already implied by scanning only
            // LivingEntity here - preserved so multiple UFOs don't snipe/beam each other.
            if (e == this || e instanceof EntityUFO || !e.isAlive()) continue;

            if (e instanceof Player player) {
                if (player.isCreative() || player.isSpectator()) continue;
                if (player.hasEffect(MobEffects.INVISIBILITY)) continue;

                if (this.target == null || this.distanceToSqr(e) < this.distanceToSqr(this.target)) {
                    this.target = e;
                }
            }

            if (this.distanceToSqr(e) < 100D * 100D && this.hasLineOfSight(e) && e != this.target) {
                this.secondaries.add(e);
            }
        }

        if (this.target == null && !this.secondaries.isEmpty()) {
            this.target = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
        }
    }

    private void pickWaypoint() {
        Entity t = this.target;
        if (t == null) return;

        Vec3 vec = new Vec3(this.getX() - t.getX(), 0, this.getZ() - t.getZ());
        if (this.random.nextInt(3) > 0) {
            vec = vec.yRot((float) (Math.PI * 2 * this.random.nextFloat()));
        }

        double length = vec.length();
        if (length < 1.0E-4) length = 1.0E-4;
        double overshoot = 35D;

        int wx = (int) Math.floor(t.getX() - vec.x / length * overshoot);
        int wz = (int) Math.floor(t.getZ() - vec.z / length * overshoot);
        int groundY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
        int wy = Math.max(groundY + 20 + this.random.nextInt(15), (int) t.getY() + 15);

        this.waypoint = new BlockPos(wx, wy, wz);
        this.courseChangeCooldown = 40 + this.random.nextInt(20);
    }

    private boolean isCourseTraversable(double len) {
        double dx = (this.waypoint.getX() - this.getX()) / len;
        double dy = (this.waypoint.getY() - this.getY()) / len;
        double dz = (this.waypoint.getZ() - this.getZ()) / len;
        AABB box = this.getBoundingBox();

        for (int i = 1; i < len; i++) {
            box = box.move(dx, dy, dz);
            if (!this.level().noCollision(this, box)) return false;
        }
        return true;
    }

    private void updateBeam() {
        Level level = this.level();

        if (this.beamTimer <= 0 && this.beam) this.beam = false;

        if (this.target != null) {
            double dist = Math.abs(this.target.getX() - this.getX()) + Math.abs(this.target.getZ() - this.getZ());
            if (dist < 25D) this.beamTimer = 30;
        }

        if (this.beamTimer > 0) {
            this.beamTimer--;

            if (!this.beam) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), HBMSoundHandler.ufoBeam.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
                this.beam = true;
            }

            int ix = (int) Math.floor(this.getX());
            int iz = (int) Math.floor(this.getZ());
            int iy = level.getMinBuildHeight();
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(ix, 0, iz);

            for (int i = (int) Math.ceil(this.getY()); i >= level.getMinBuildHeight(); i--) {
                cursor.setY(i);
                if (!level.getBlockState(cursor).isAir()) {
                    iy = i;
                    break;
                }
            }

            if (iy < this.getY()) {
                AABB column = new AABB(this.getX(), iy, this.getZ(), this.getX(), this.getY(), this.getZ()).inflate(5, 0, 5);
                DamageSource source = level.damageSources().source(ModDamageTypes.COMBINE_BALL, this);

                for (Entity e : level.getEntitiesOfClass(Entity.class, column)) {
                    // CE: canAttackClass(e.getClass()) excludes this exact class (another EntityUFO)
                    // from the beam's damage/ignite/contaminate pass too - see scanTargets()'s identical note.
                    if (e == this || e instanceof EntityUFO) continue;
                    e.hurt(source, 1000F);
                    e.igniteForSeconds(5);
                    if (e instanceof LivingEntity living) {
                        ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, 5D);
                    }
                }
            }
        }
    }

    private void updateAttacks() {
        int t = this.tickCount;

        if (t % 300 < 200) {
            if (t % 4 == 0) {
                fireAtSecondaryOrPrimary(true);
            } else if (t % 4 == 2 && this.target != null) {
                laserAttack(this.target);
            }
        } else {
            if (t % 20 == 0) {
                fireAtSecondaryOrPrimary(false);
            } else if (t % 20 == 10 && this.target != null) {
                rocketAttack(this.target);
            }
        }
    }

    private void fireAtSecondaryOrPrimary(boolean laser) {
        if (!this.secondaries.isEmpty()) {
            Entity e = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
            if (!e.isAlive()) {
                this.secondaries.remove(e);
            } else if (laser) {
                laserAttack(e);
            } else {
                rocketAttack(e);
            }
        } else if (this.target != null) {
            if (laser) laserAttack(this.target); else rocketAttack(this.target);
        }
    }

    private void laserAttack(Entity e) {
        if (!(e instanceof LivingEntity living) || this.level().isClientSide) return;

        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this.level(), LegacyMobBulletConfigs.WORM_LASER, this, living, 2.0F, 0.02F);
        this.level().addFreshEntity(bullet);
        this.playSound(HBMSoundHandler.ballsLaser.get(), 5.0F, 1.0F);
    }

    private void rocketAttack(Entity e) {
        if (!(e instanceof LivingEntity living) || this.level().isClientSide) return;

        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this.level(), LegacyMobBulletConfigs.UFO_ROCKET, this, living, 2.0F, 0.02F);
        bullet.lockonTarget = living;
        this.level().addFreshEntity(bullet);
        this.playSound(HBMSoundHandler.richard_fire.get(), 5.0F, 1.0F);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!this.level().isClientSide) {
            EntityNukeTorex.statFac(this.level(), this.getX(), this.getY(), this.getZ(), 25);
            this.level().addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(this.level(), 25, this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5).setDetonator(this));

            for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(200))) {
                AdvancementManager.grantAchievement(player, AdvancementManager.bossUFO);
                ItemStack reward = new ItemStack(SpecialItems.COIN_UFO.get());
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    /**
     * Not a CE port - a correctness fix matching {@code EntityMaskMan}'s identical override: without
     * clearing {@link #bossEvent} on removal, the boss bar would stay stuck on every tracking player's
     * screen forever after this entity dies/unloads, since {@link ServerBossEvent} is not itself tied
     * to this entity's lifecycle. Matches vanilla {@code EnderDragon}/{@code WitherBoss}'s own
     * {@code remove(RemovalReason)} override for exactly this purpose.
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }
}
