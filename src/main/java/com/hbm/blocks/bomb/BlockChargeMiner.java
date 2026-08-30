package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BlockChargeMiner} (49 lines, read in full). CE's
 * {@code ExAttrib.NOHURT + ALLDROP} (mining charge: breaks/drops every block, does no entity
 * damage) maps onto {@link ExplosionVNT} by using {@link BlockProcessorStandard#setAllDrop()} and,
 * per {@link BlockChargeDynamite}'s javadoc, leaving the entity/player processor unset entirely -
 * {@link ExplosionVNT#explode()} skips the whole entity-processing branch when both are {@code null},
 * which is exactly "no hurt".
 */
public class BlockChargeMiner extends BlockChargeBase {

    public BlockChargeMiner(Properties properties) {
        super(properties);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;

        safe = true;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        safe = false;

        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        ExplosionVNT exp = new ExplosionVNT(level, x, y, z, 4F, detonator);
        exp.setBlockAllocator(new BlockAllocatorStandard());
        exp.setBlockProcessor(new BlockProcessorStandard().setAllDrop());
        exp.explode();

        return BombReturnCode.DETONATED;
    }
}
