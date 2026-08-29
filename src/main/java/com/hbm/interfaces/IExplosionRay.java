package com.hbm.interfaces;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Interface for procedural explosions.
 *
 * @author mlbv
 */
public interface IExplosionRay {
    /**
     * Called every tick.
     * All heavy calculations are recommended to be done off the main thread.
     *
     * @param processTimeMs maximum time to process in this tick
     */
    void update(int processTimeMs);

    /**
     * Immediately cancels the explosion.
     */
    void cancel();

    /**
     * @return true if the explosion is finished or canceled.
     */
    boolean isComplete();

    /**
     * @return true if all rays stop within the maximum radius.
     */
    boolean isContained();

    void setDetonator(UUID detonator);

    void readEntityFromNBT(CompoundTag nbt);

    void writeEntityToNBT(CompoundTag nbt);
}
