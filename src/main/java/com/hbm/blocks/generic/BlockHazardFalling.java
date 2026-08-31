package com.hbm.blocks.generic;

import com.hbm.hazard.HazardSystem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code BlockHazardFalling} ({@code block_fallout}, {@code block_yellowcake}): a
 * falling, sand-like hazardous block. CE additionally ran a {@code ChunkRadiationManager} field tick
 * and a beacon-base flag from this class; both are deferred for the same reasons documented on
 * {@link BlockHazard} (no chunk-radiation system ported yet, beacon-base membership is a
 * {@code minecraft:beacon_base_blocks} tag in modern Minecraft, not a Java override). What survives
 * is CE's per-entity hazard application on contact, via {@link HazardSystem#applyHazards}.
 */
public class BlockHazardFalling extends FallingBlock {

    public static final MapCodec<BlockHazardFalling> CODEC = simpleCodec(BlockHazardFalling::new);

    public BlockHazardFalling(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BlockHazardFalling> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (entity instanceof LivingEntity living) {
            HazardSystem.applyHazards(this, living);
        }
    }
}
