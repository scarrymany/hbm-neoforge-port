package com.hbm.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityRagingVortex} (65 lines, read in full) - the
 * "occasionally detonates while alive" gravity well. Adds a {@link #timer} field and overrides
 * {@link #tick()} with its own separate, redundant {@link EntityBlackHole#isWarDim} re-check (already
 * checked once more by the {@code super.tick()} call this method makes at its end) - CE genuinely
 * checks the gate twice per tick for this one entity; preserved faithfully rather than "cleaned up"
 * into a single check, per the report's Open questions (harmless once the always-true stub applies,
 * but a real behavior change once real dimension gating exists, since the first check does not return
 * early on its <i>true</i> branch, only its <i>false</i> one).
 * <p>
 * Each tick: increments {@link #timer} (with a dead branch - {@code if(timer <= 20) timer -= 20;} can
 * never fire meaningfully once {@code timer} starts at 0 and only increments - preserved as CE has it,
 * not simplified away), computes a sinusoidal "pulse" shrink term, and on a flat 1-in-100 roll an
 * additional {@code 0.1F} shrink plus a real {@code level.explode(null, x, y, z, 10F,
 * Level.ExplosionInteraction.NONE)} - CE's real flag here is <b>no block damage</b> ({@code false}),
 * <b>not</b> Neo Edition's {@code .BLOCK} substitution for this specific call. {@code SIZE} shrinks by
 * {@code pulse + dec} every tick (so it can occasionally grow slightly on the sine wave's downswing)
 * until it hits 0.
 */
public class EntityRagingVortex extends EntityBlackHole {

    private int timer = 0;

    public EntityRagingVortex(EntityType<? extends EntityRagingVortex> type, Level level) {
        super(type, level);
    }

    public EntityRagingVortex(Level level, float size) {
        this(GravityWellEntityTypes.RAGING_VORTEX.get(), level);
        this.entityData.set(SIZE, size);
    }

    @Override
    public void tick() {
        Level level = this.level();

        // CE's own redundant double isWarDim check - see class javadoc, preserved as-is.
        if (!isWarDim(level)) {
            this.discard();
            return;
        }

        timer++;

        // Dead branch after the very first tick - see class javadoc.
        if (timer <= 20) {
            timer -= 20;
        }

        float pulse = (float) (Math.sin(timer) * Math.PI / 20D) * 0.35F;

        float dec = 0.0F;

        if (this.random.nextInt(100) == 0) {
            dec = 0.1F;
            // CE: world.createExplosion(null, posX, posY, posZ, 10F, false) - no isRemote gate in CE
            // either; Level#explode is safe to call on both sides (client just plays local FX).
            level.explode(null, this.getX(), this.getY(), this.getZ(), 10F, Level.ExplosionInteraction.NONE);
        }

        this.entityData.set(SIZE, this.entityData.get(SIZE) - pulse - dec);
        if (this.entityData.get(SIZE) <= 0) {
            this.discard();
            return;
        }

        super.tick();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.timer = tag.getInt("vortexTimer");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("vortexTimer", this.timer);
    }
}
