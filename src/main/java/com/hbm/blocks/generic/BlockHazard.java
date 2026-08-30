package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.hazard.HazardSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code BlockHazard}: the contact-radiation storage-block behavior (e.g.
 * {@code block_thorium}, {@code block_uranium}, {@code block_schrabidium}). CE additionally drove a
 * chunk-wide radiation field ({@code ChunkRadiationManager}), a beacon-base flag, and per-block
 * particle effects ({@code ExtDisplayEffect}) from this class.
 * <p>
 * The chunk-radiation field has no equivalent system in this port yet ({@code ChunkRadiationManager}
 * is not ported) and beacon-base membership is a data-driven {@code minecraft:beacon_base_blocks}
 * tag in modern Minecraft rather than a per-block Java override (confirmed: no
 * {@code isBeaconBase}-shaped method exists on {@code Block}/{@code BlockBehaviour} in this
 * toolchain) - both are left as datagen/future-system follow-ups. What survives is the one
 * mechanism the port's {@link HazardSystem} already implements end to end: applying the block's
 * registered {@link com.hbm.hazard.HazardData} to any entity standing in/on it, exactly like
 * {@link BlockNTMOre#entityInside}.
 */
public class BlockHazard extends BlockBase {

    public BlockHazard(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (entity instanceof LivingEntity living) {
            HazardSystem.applyHazards(this, living);
        }
    }
}
