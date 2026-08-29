package com.hbm.interfaces;

import com.hbm.handler.ClimbableRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implement this on any TE (or other object) that exposes a climbable AABB.
 * <p>
 * Lifecycle:
 * - Call {@link #registerClimbable()} in onLoad(). Do not call on validate()!
 * - Call {@link #unregisterClimbable()} in invalidate()/onChunkUnload().
 * - If the climb AABB or anchor changes at runtime, call {@link ClimbableRegistry#refresh(IClimbable)}.
 */
public interface IClimbable {

    // mlbv: For the two methods below, DO NOT attempt to name it getWorld()/getPos() and let TileEntity implement it.
    // This does work at dev, non-obf environment, but in obfuscated runtime it will throw an AbstractMethodError.
    @NotNull
    default Level world() {
        return ((BlockEntity) this).getLevel();
    }

    @NotNull
    default BlockPos pos() {
        return ((BlockEntity) this).getBlockPos();
    }

    boolean isEntityInClimbAABB(@NotNull LivingEntity entity);

    /**
     * AABB used for *indexing* across chunks. If null, the registry will index in the anchor chunk only.
     * Return your real climb box (world-space) for best coverage.
     */
    @Nullable AABB getClimbAABBForIndexing();

    default void registerClimbable() {
        ClimbableRegistry.register(this);
    }

    default void unregisterClimbable() {
        ClimbableRegistry.unregister(this);
    }
}
