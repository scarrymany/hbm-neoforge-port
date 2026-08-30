package com.hbm.blocks.generic;

import com.hbm.hazard.HazardSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code WasteIce extends BlockIce}: contaminated ice that applies whatever
 * {@link com.hbm.hazard.HazardData} the port's {@link HazardSystem} has registered for it (CE's
 * own {@code HazardSystem.applyHazards} call, already ported end to end - see
 * {@link BlockHazard}/{@link BlockNTMOre} for the same pattern). No hazard data is registered for
 * this block by this pass (that is an items/hazard-registration-area concern, not a block-class
 * one), so {@link HazardSystem#applyHazards} is a safe no-op until it is.
 */
public class WasteIce extends IceBlock {

    public WasteIce(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof LivingEntity living) {
            HazardSystem.applyHazards(this, living);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (entity instanceof LivingEntity living) {
            HazardSystem.applyHazards(this, living);
        }
    }
}
