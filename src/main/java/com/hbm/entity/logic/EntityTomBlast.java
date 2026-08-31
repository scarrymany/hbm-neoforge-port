package com.hbm.entity.logic;

import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionTom;
import com.hbm.saveddata.TomSaveData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.logic.EntityTomBlast} (105 lines, read in full) -
 * {@code EntityTom}'s real "impact" payload, per
 * {@code docs/phase4/satellites_followup_and_loot_pools.md}'s Deferred scope (flagged there as
 * needing {@link ExplosionTom} read and ported in full before this class could be, which this
 * package's own task brief explicitly required). Near-identical in shape to this port's own
 * already-committed {@link EntityBalefire} (same shell/leg/element expanding-spiral driver, same
 * {@code speed}-ramping per-tick iteration count, same chunk-forcing base class) -
 * {@link ExplosionTom} is this class's own real payload algorithm, {@code ExplosionBalefire}'s sibling.
 * <p>
 * <b>{@code isWarDim} - resolved as "always true, guarded content is real"</b>, matching
 * {@code ExplosionLarge}'s established policy rather than leaving the guarded content itself a
 * permanent no-op (per this package's task brief's explicit instruction to build {@link
 * ExplosionTom} in full, resolving {@code docs/phase4/satellites_followup_and_loot_pools.md}'s own
 * flagged open question in that direction for this specific class).
 */
public class EntityTomBlast extends EntityExplosionChunkloading {

    public int age = 0;
    public int destructionRange = 0;
    public ExplosionTom exp;
    public int speed = 1;
    public boolean did = false;

    public EntityTomBlast(EntityType<? extends EntityTomBlast> entityType, Level level) {
        super(entityType, level);
    }

    public EntityTomBlast(Level level) {
        this(SatellitePayloadEntityTypes.TOM_BLAST.get(), level);
    }

    /**
     * Package-local stub matching {@code com.hbm.potion.HbmPotionEffects#isWarDim}'s established
     * convention - see {@code EntityDeathBlast}'s own javadoc for the full reasoning; duplicated
     * here since {@link ExplosionTom} (a different, explosion-package class) needs its own copy too.
     */
    static boolean isWarDim(Level level) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();

        if (!isWarDim(level)) {
            this.discard();
            return;
        }

        if (!level.isClientSide()) loadChunk(chunkPosition().x, chunkPosition().z);

        if (!this.did) {
            exp = new ExplosionTom((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange);
            this.did = true;
        }

        speed += 1; // increase speed to keep up with expansion

        boolean flag = false;
        for (int i = 0; i < this.speed; i++) {
            flag = exp.update();

            if (flag) {
                this.discard();
                if (level instanceof ServerLevel serverLevel) {
                    TomSaveData data = TomSaveData.forWorld(serverLevel);
                    data.impact = true;
                    data.fire = 1F;
                    data.setDirty();
                }
            }
        }

        if (this.random.nextInt(5) == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 10000.0F, 0.8F + this.random.nextFloat() * 0.2F);
        }

        if (!flag) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 10000.0F, 0.8F + this.random.nextFloat() * 0.2F);
            ExplosionNukeGeneric.dealDamage(level, getX(), getY(), getZ(), this.destructionRange * 2);
        }

        age++;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        markChunkLoaderRestoredFromNBT();
        age = nbt.getInt("age");
        destructionRange = nbt.getInt("destructionRange");
        speed = nbt.getInt("speed");
        did = nbt.getBoolean("did");

        exp = new ExplosionTom((int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
        exp.readFromNbt(nbt, "exp_");

        this.did = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("age", age);
        nbt.putInt("destructionRange", destructionRange);
        nbt.putInt("speed", speed);
        nbt.putBoolean("did", did);

        if (exp != null) exp.saveToNbt(nbt, "exp_");
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkLoader();
        super.remove(reason);
    }
}
