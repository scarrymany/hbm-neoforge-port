package com.hbm.entity.logic;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IConstantRenderer;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.logic.EntityOrbitalLaser} (69 lines, read in full) -
 * {@code SatellitePrecisionLaser}'s payload, per
 * {@code docs/phase4/satellites_followup_and_loot_pools.md}'s Headline finding #2: the most
 * ready-to-port of the 5 classes that report covers, since every class {@link #explode()} calls was
 * already real and compiling in this port before this pass, needing only one small additive setter
 * ({@link EntityProcessorCrossSmooth#setDamageClass}, added in the foundation wave this package
 * builds on).
 * <p>
 * A pure, non-colliding {@link Entity} that self-destructs after {@link #MAX_AGE} (5) ticks. Unlike
 * its sibling {@link EntityDeathBlast}, CE has <b>no {@code isWarDim} gate at all</b> for this class
 * - {@link #explode()} always fires unconditionally, matching CE exactly.
 */
public class EntityOrbitalLaser extends Entity implements IConstantRenderer {

    public static final int MAX_AGE = 5;

    public EntityOrbitalLaser(EntityType<? extends EntityOrbitalLaser> type, Level level) {
        super(type, level);
    }

    public EntityOrbitalLaser(Level level) {
        this(SatellitePayloadEntityTypes.ORBITAL_LASER.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // CE's entityInit() is empty - no synced fields.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // CE's readEntityFromNBT is empty.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // CE's writeEntityToNBT is empty.
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && this.tickCount >= MAX_AGE) this.discard();
    }

    /**
     * CE's real, unconditional payload - a single {@link ExplosionVNT} laser burst. Every class used
     * here was already real and compiling in this port before this package landed (see class
     * javadoc); this method is a direct, verbatim transcription of CE's own {@code explode()} body.
     */
    public void explode() {
        ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY(), getZ(), 5F);
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 1_000F).setupPiercing(50F, 0.5F).setDamageClass(DamageClass.LASER));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
        vnt.explode();
    }
}
