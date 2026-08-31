package com.hbm.entity.mob;

import com.hbm.lib.HBMSoundHandler;
import com.hbm.particle.HbmEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityQuackos} (186 lines, read in full,
 * {@code extends EntityDuck}, {@code /** BOW *&#47;} throughout) - see {@code docs/phase4/
 * entities_bosses.md}'s Quackos row. A joke pseudo-boss: 25x-scaled (0.3x0.7 -> 7.5x17.5, hardcoded at
 * the {@link EntityType.Builder} level rather than CE's runtime {@code setSize}), fully invulnerable,
 * never despawns, rideable, purple boss bar with zero attack code. Removed only via
 * {@link com.hbm.items.tool.ItemPeas}'s {@link #despawn()} call.
 * <p>
 * <b>Invulnerability</b>: CE's {@code getIsInvulnerable() -> true} maps onto 1.21.1's
 * {@link #isInvulnerableTo(DamageSource)} (confirmed real, already-used API in this port - see
 * {@code EntityWormBaseNT}/{@code EntityMinecartNTM}/{@code EntityPlaneBase}'s own identical-shaped
 * calls). CE's {@code setHealth} guard (refuses any decrease) is preserved separately since a
 * direct {@code /kill} or NBT edit bypasses damage entirely.
 * <p>
 * <b>Rideable</b> ({@link #positionRider}): the modern replacement for CE's {@code updatePassenger}
 * override point (confirmed real, same shape {@code EntityRailCarRidable}/{@code
 * EntityRailCarRidable.SeatDummyEntity} in this port already use per {@code docs/phase4/
 * entities_vehicles_aircraft.md}'s Key design decisions).
 * <p>
 * <b>Void-fallback threshold adjusted, not copied verbatim</b>: CE's {@code onLivingUpdate} teleports
 * Quackos back to Y=256 once {@code posY < -30} - a "fell through the 1.12 Y&gt;=0 world" catch. 1.21.1
 * worlds have valid negative Y down to {@link Level#getMinBuildHeight()} (-64 in an overworld-shaped
 * dimension), so a literal {@code posY < -30} threshold would wrongly teleport Quackos out of any
 * deep cave/underground build. Adjusted to trigger only below the level's own min build height minus a
 * safety margin, preserving CE's actual intent ("don't let this indestructible mount get stuck in the
 * void") without the 1.12-only assumption.
 */
public class EntityQuackos extends EntityDuck {

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public EntityQuackos(EntityType<? extends EntityQuackos> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return HBMSoundHandler.megaquacc.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HBMSoundHandler.megaquacc.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return HBMSoundHandler.megaquacc.get();
    }

    /** CE: {@code getIsInvulnerable() { return true; }} */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    /** CE: {@code setHealth} silently refuses any decrease - a direct NBT/command health edit can't
     *  reduce it either, on top of {@link #isInvulnerableTo} blocking ordinary damage. */
    @Override
    public void setHealth(float health) {
        if (health < this.getHealth()) return;
        super.setHealth(health);
    }

    /** CE: {@code canDespawn() { return false; }} (already {@link EntityDuck}'s default, restated here
     *  for clarity since Quackos additionally overrides {@link #despawn()} as its only removal path). */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = super.mobInteract(player, hand);
        if (result.consumesAction()) return result;

        if (!this.level().isClientSide && this.getPassengers().isEmpty()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        float yawRad = this.yBodyRot * (float) Math.PI / 180.0F;
        float sin = (float) Math.sin(yawRad);
        float cos = (float) Math.cos(yawRad);
        float sideOffset = 0.1F;

        // CE's own f3 (a second Y-offset term) is hardcoded 0.0F and never changed - not reproduced.
        callback.accept(passenger,
                this.getX() + sideOffset * sin,
                this.getY() + this.getBbHeight() - 0.125D,
                this.getZ() - sideOffset * cos);

        if (passenger instanceof LivingEntity living) {
            living.yBodyRot = this.yBodyRot;
        }
    }

    /**
     * CE: {@code despawn()} - a 150-particle {@link HbmEffect#BF} burst (now wired, see this method's
     * own inline comment for the network-efficiency deviation) followed by {@code this.isDead = true},
     * bypassing the normal death event/loot path entirely. {@link com.hbm.items.tool.ItemPeas}'s sole
     * call site.
     */
    public void despawn() {
        if (!this.level().isClientSide) {
            // CE broadcasts 150 SEPARATE AuxParticlePacketNT(BF) packets here, each spawning one
            // particle at its own randomized offset (upstream/hbm-ce/.../EntityQuackos.java:141-150) -
            // collapsed into one packet carrying count=150, with the client spawning the whole burst
            // locally from the same offset formula (see HbmEffect.BF's own handler javadoc for the
            // matching network-side note). Radius 150, matching CE's own TargetPoint.
            CompoundTag data = new CompoundTag();
            data.putInt("count", 150);
            HbmEffect.sendPacket(this.level(), HbmEffect.BF, this.getX(), this.getY(), this.getZ(), 150, data);
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.getY() < this.level().getMinBuildHeight() - 16) {
            this.teleportTo(
                    this.getX() + this.random.nextGaussian() * 30,
                    256,
                    this.getZ() + this.random.nextGaussian() * 30
            );
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
