package com.hbm.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityVortex} (50 lines, read in full) - the
 * "temporary, self-extinguishing" gravity well. Adds one field ({@link #shrinkRate}, default
 * {@code 0.0025F}, public {@link #setShrinkRate}, persisted to NBT) and overrides {@link #tick()} to
 * shrink {@code SIZE} by a <b>hardcoded literal {@code 0.0025F} every tick - not {@link #shrinkRate}
 * itself</b> - before delegating to {@code super.tick()}; once {@code SIZE <= 0} it discards itself.
 * <p>
 * <b>This is a confirmed, real CE bug, deliberately preserved rather than fixed</b> (per
 * docs/phase4/entities_vortex_gravity_wells.md's Open questions): every real CE call site
 * ({@code ItemDrop}'s 4 singularity branches, {@code ItemConserve}'s {@code BHOLE} food,
 * {@code ItemGlitch}'s case 27, {@code LegacyChargeWeapons}' {@code gun_b93} modes 4/5) constructs an
 * {@code EntityVortex} at some size, and exactly one of them ({@code ItemConserve}) also calls
 * {@link #setShrinkRate}(0.01F) - but {@link #tick()} never reads {@link #shrinkRate}, so every vortex
 * in real CE gameplay shrinks at the exact same fixed {@code 0.0025F}/tick rate regardless of what
 * {@link #setShrinkRate} was given. The field is still set, still persisted, and the setter is still
 * called (for parity) by every real caller - it simply has zero gameplay effect, matching CE exactly.
 */
public class EntityVortex extends EntityBlackHole {

    /** Set by real callers (e.g. {@code ItemConserve}) but never read by {@link #tick()} - see class javadoc. */
    private float shrinkRate = 0.0025F;

    public EntityVortex(EntityType<? extends EntityVortex> type, Level level) {
        super(type, level);
    }

    public EntityVortex(Level level, float size) {
        this(GravityWellEntityTypes.VORTEX.get(), level);
        this.entityData.set(SIZE, size);
    }

    public EntityVortex setShrinkRate(float shrinkRate) {
        this.shrinkRate = shrinkRate;
        return this;
    }

    @Override
    public void tick() {
        // CE: this.getDataManager().set(SIZE, this.getDataManager().get(SIZE) - 0.0025F) - hardcoded
        // literal, not this.shrinkRate. See class javadoc: preserved exactly, not "fixed".
        this.entityData.set(SIZE, this.entityData.get(SIZE) - 0.0025F);

        if (this.entityData.get(SIZE) <= 0) {
            this.discard();
            return;
        }

        super.tick();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.shrinkRate = tag.getFloat("shrinkRate");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("shrinkRate", this.shrinkRate);
    }
}
