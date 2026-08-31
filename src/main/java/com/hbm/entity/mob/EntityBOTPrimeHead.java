package com.hbm.entity.mob;

import com.hbm.items.special.SpecialItems;
import com.hbm.main.AdvancementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.botprime.EntityBOTPrimeHead} (189 lines, read in full)
 * - see {@code docs/phase4/entities_bosses.md}'s worm-boss table. The boss-bar owner (green,
 * {@link ServerBossEvent}) among the worm's 75 independently-full-health segments.
 * <p>
 * <b>Boss-bar API shape</b> - the research report flags this as well-established Mojang-mapping
 * knowledge (vanilla {@code EnderDragon}/{@code WitherBoss}'s own wiring) but explicitly
 * <em>not</em> independently verified against a compiled jar or Neo Edition (which has ported zero
 * boss content) in this sandbox. See this package's own knownGaps.
 * <p>
 * <b>{@link #spawnBody()}</b> - CE's {@code onInitialSpawn}: spawns exactly 74
 * {@link EntityBOTPrimeBody} segments at the head's own position with sequential {@code partNumber}
 * 0-73, sharing the head's own entity id as every segment's {@code headID}. Called explicitly by every
 * spawn path this package wires up ({@code ItemChopper}'s {@code spawn_worm}, {@code BlockBallsSpawner})
 * - CE itself calls {@code onInitialSpawn} explicitly from its own summon call sites (matching
 * {@code BlockBallsSpawner.onBlockActivated}'s own explicit call), since manually-constructed-and-
 * {@code addFreshEntity}'d mobs are not routed through vanilla's natural-spawn {@code finalizeSpawn}
 * dispatch. No {@code finalizeSpawn} override exists here - see the inline note above that method's
 * (deliberately absent) location for why, and this package's own knownGaps.
 * <p>
 * <b>Self-heal</b> (in {@link #customServerAiStep()}): +1 HP/6 ticks while it has a
 * {@link #targetedEntity} (engaged), +4 HP/6 ticks while idle and not recently hit
 * ({@link #recentlyHit} == 0) - a real, intentional regen-while-disengaged mechanic per the research
 * report, preserved exactly.
 * <p>
 * <b>On death</b> ({@link #die}): grants {@link AdvancementManager#bossWorm} (now real, foundation
 * wave) + one {@code coin_worm} to every player within 200 blocks - CE's {@code onDeathUpdate} does
 * this at {@code deathTime == 19} (a ~1-second delay purely for death-animation timing); this port
 * fires it immediately from {@code die()} instead, matching this port's own established convention
 * ({@code EntityCreeperNuclear#die}) rather than reimplementing a death-tick timer for a cosmetic delay
 * with no gameplay effect.
 */
public class EntityBOTPrimeHead extends EntityBOTPrimeBase {

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    private final WormMovementHeadNT movement = new WormMovementHeadNT(this);

    public EntityBOTPrimeHead(EntityType<? extends EntityBOTPrimeHead> type, Level level) {
        super(type, level);
        this.wasNearGround = false;
        this.attackRange = 150.0D;
        this.maxSpeed = 1.0D;
        this.fallSpeed = 0.006D;
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new EntityAINearestAttackableTargetNT(this, Player.class, 0, false, false, null, 128.0D));
    }

    /** CE: {@code applyEntityAttributes}, override on top of {@link EntityBOTPrimeBase#createAttributes()} - {@code MOVEMENT_SPEED = 0.15}. */
    public static AttributeSupplier.Builder createAttributes() {
        return EntityBOTPrimeBase.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.15D);
    }

    @Override
    public boolean getIsHead() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (super.hurt(source, amount)) {
            this.dmgCooldown = 4;
            return true;
        }
        return false;
    }

    /** See class javadoc - CE's {@code onInitialSpawn}. Public: called directly by every spawn site. */
    public void spawnBody() {
        this.setHeadID(this.getId());

        BlockPos pos = this.blockPosition();
        Level level = this.level();

        for (int i = 0; i < 74; i++) {
            EntityBOTPrimeBody body = new EntityBOTPrimeBody(WormEntityTypes.BOTPRIME_BODY.get(), level);
            body.setPartNumber(i);
            body.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            body.setHeadID(this.getId());
            level.addFreshEntity(body);
        }

        this.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        this.spawnPoint = pos;
    }

    // NOTE: no finalizeSpawn(...) override here - the exact 1.21.1 parameter type for the "spawn
    // reason" enum on that method (MobSpawnType vs. a possible EntitySpawnReason rename in
    // this-or-nearby versions) is not independently confirmed against a compiled jar in this sandbox,
    // and this method is not load-bearing for either real spawn path this package wires (ItemChopper's
    // spawn_worm and BlockBallsSpawner both call spawnBody() explicitly, matching CE's own explicit
    // onInitialSpawn call sites). A vanilla /summon of this entity type would therefore spawn a
    // headless head with no body chain - see this package's own knownGaps.

    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
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

    @Override
    protected void customServerAiStep() {
        this.movement.updateMovement();

        if (this.getHealth() < this.getMaxHealth() && this.tickCount % 6 == 0) {
            if (this.targetedEntity != null) {
                this.heal(1.0F);
            } else if (this.recentlyHit == 0) {
                this.heal(4.0F);
            }
        }

        if (this.targetedEntity != null && this.targetedEntity.distanceToSqr(this) < this.attackRange * this.attackRange) {
            if (hasLineOfSight(this.targetedEntity)) {
                this.attackCounter++;
                if (this.attackCounter == 30) {
                    laserAttack(this.targetedEntity, true);
                    this.attackCounter = 0;
                }
            } else {
                this.attackCounter = 0;
            }
        } else {
            this.attackCounter = 0;
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!this.level().isClientSide) {
            List<ServerPlayer> players = this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(200));
            for (ServerPlayer player : players) {
                AdvancementManager.grantAchievement(player, AdvancementManager.bossWorm);
                ItemStack reward = new ItemStack(SpecialItems.COIN_WORM.get());
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
            }
        }
    }

    /** CE: {@code onUpdate} - instantly snaps (both current and previous) rotation to face the current
     *  motion vector; unlike {@link WormMovementHeadNT}'s own rotation set, CE updates both fields here
     *  (no interpolation lag). */
    @Override
    public void tick() {
        super.tick();

        Vec3 motion = this.getDeltaMovement();
        float horiz = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, horiz) * 180.0D / Math.PI);

        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(pitch);
        this.xRotO = pitch;
    }

    @Override
    public float getAttackStrength(Entity target) {
        return 1000F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("spawnX", this.spawnPoint.getX());
        tag.putInt("spawnY", this.spawnPoint.getY());
        tag.putInt("spawnZ", this.spawnPoint.getZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.spawnPoint = new BlockPos(tag.getInt("spawnX"), tag.getInt("spawnY"), tag.getInt("spawnZ"));
    }
}
