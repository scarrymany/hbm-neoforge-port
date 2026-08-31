package com.hbm.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * No-clip fire-immune marker for leftover CE {@code @AutoRegister} FX / package / EMP / spear
 * entities. Discards after {@link #maxAge} ticks. Fallback renderer is enough.
 */
public class EntityLogicTail extends Entity {

    public int maxAge = 200;

    public EntityLogicTail(EntityType<? extends EntityLogicTail> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount >= maxAge) {
            this.discard();
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.maxAge = tag.getInt("maxAge");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("maxAge", maxAge);
    }
}
