package com.hbm.entity.logic;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.LegacyMobBulletConfigs;
import com.hbm.interfaces.IConstantRenderer;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.entity.logic.EntityDeathBlast} (93 lines, read in full) -
 * {@code SatelliteLaser}'s payload, per {@code docs/phase4/satellites_followup_and_loot_pools.md}.
 * A pure, non-colliding {@link Entity} that self-destructs after {@link #MAX_AGE} (60) ticks; on
 * death, server-side only:
 * <ol>
 *     <li>gated by {@link #isWarDim} (stubbed {@code true} - see that method's own javadoc): spawns
 *     a real {@link EntityNukeExplosionMK5#statFacNoRad} blast, plus a 100-bolt circular fan of
 *     {@link LegacyMobBulletConfigs#MASKMAN_BOLT} bullets fired via {@link EntityBulletBaseMK4}
 *     (the pre-Sedna mob-ballistics retarget this port's {@code docs/phase4/
 *     entities_legacy_bullet_system.md} already built) - CE's own
 *     {@code new EntityBulletBase(world, BulletConfigSyncingUtil.MASKMAN_BOLT)} loop, retargeted
 *     onto the real framework this port actually has rather than the still-fully-deferred legacy
 *     {@code EntityBulletBase} class itself;</li>
 *     <li>unconditionally: an explosion sound broadcast.</li>
 * </ol>
 * <p>
 * <b>Not ported</b>: CE's unconditional networked particle burst
 * ({@code AuxParticlePacketNT(HbmEffectNT.Muke, ...)} via {@code PacketThreading}) - that
 * packet/particle-helper infrastructure does not exist in this port yet, matching the identical
 * documented Phase 5 gap already established by {@code ExplosionLarge}/{@code ExplosionEffectWeapon}
 * for every other CE networked-particle call site. The sound half of that same CE line is real and
 * kept.
 * <p>
 * <b>Ring-fan velocity - a documented, deliberate adaptation, not a byte-for-byte match.</b> CE sets
 * each bolt's raw {@code motionX/Z} directly to a small (0.2 magnitude) vector with no re-scaling -
 * legacy {@code EntityBulletBase} has no per-tick "muzzle velocity" re-acceleration model. This
 * port's {@link EntityBulletBaseMK4} does (every tick multiplies raw delta-movement by
 * {@code config.velocity + accel}, {@code 5.0F} for {@link LegacyMobBulletConfigs#MASKMAN_BOLT}), so
 * passing CE's literal {@code 0.2F} magnitude to {@code shoot(...)} produces a net per-tick
 * expansion speed roughly 5x CE's own crawl rate - the same tradeoff this port's own
 * {@code LegacyMobBulletConfigs.maskmanOrbUpdate} already accepted for the exact same
 * {@code MASKMAN_BOLT} config fired in an analogous radiating pattern, rather than hand-deriving an
 * exact-magnitude-matching velocity that would fight the framework's own re-acceleration model.
 * Direction/zero-scatter fidelity (an exact ring, no randomization) is preserved exactly.
 */
public class EntityDeathBlast extends Entity implements IConstantRenderer {

    public static final int MAX_AGE = 60;

    /** CE: {@code EntityPlayerMP detonator} - never persisted (CE's own {@code readEntityFromNBT}/
     *  {@code writeEntityToNBT} are both empty), matching this entity's brief 60-tick lifetime. */
    @Nullable
    public ServerPlayer detonator;

    public EntityDeathBlast(EntityType<? extends EntityDeathBlast> type, Level level) {
        super(type, level);
    }

    public EntityDeathBlast(Level level) {
        this(SatellitePayloadEntityTypes.DEATH_BLAST.get(), level);
    }

    /**
     * Package-local stub matching {@code com.hbm.potion.HbmPotionEffects#isWarDim}'s established
     * convention (stubbed {@code true}, not {@code false} - CE's real default treats every
     * dimension as a "war dimension" until a server operator opts one out; see that method's own
     * javadoc for the full reasoning). Duplicated here rather than widening that method's package
     * visibility, matching {@code EntityFalloutRain}'s own identical precedent.
     */
    private static boolean isWarDim(Level level) {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // CE's entityInit() is empty - no synced fields.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // CE's readEntityFromNBT is empty - see class javadoc on detonator.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // CE's writeEntityToNBT is empty - see class javadoc on detonator.
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || this.tickCount < MAX_AGE) return;

        this.discard();

        if (isWarDim(level())) {
            level().addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(level(), 40, getX(), getY(), getZ()).setDetonator(detonator));

            int count = 100;
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                Vec3 dir = new Vec3(Math.cos(angle), -0.05D, Math.sin(angle));

                EntityBulletBaseMK4 bolt = new EntityBulletBaseMK4(level());
                bolt.setBulletConfig(LegacyMobBulletConfigs.MASKMAN_BOLT);
                bolt.damage = LegacyMobBulletConfigs.MASKMAN_BOLT.rollDamage(level().random);
                bolt.setOwner(detonator);
                bolt.setPos(getX(), getY() + 2, getZ());
                bolt.shoot(dir.x, dir.y, dir.z, 0.2F, 0F);
                level().addFreshEntity(bolt);
            }
        }

        level().playSound(null, getX(), getY(), getZ(), HBMSoundHandler.mukeExplosion.get(), SoundSource.HOSTILE, 25.0F, 0.9F);
    }
}
