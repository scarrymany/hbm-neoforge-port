package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BlockChargeDynamite} (38 lines, read in full). CE's
 * original uses the older, non-{@code vanillant} {@code ExplosionNT} helper (not itself read in the
 * research survey, flagged there as a forward reference for whoever ports it) with default
 * attributes - i.e. normal partial block destruction plus normal entity damage/knockback, the same
 * shape {@link BlockChargeMiner} explicitly opts out of via its own {@code NOHURT}/{@code ALLDROP}
 * attributes. Per this task's instruction, this is approximated here with an equivalent
 * {@link ExplosionVNT} configuration (default resolution/drop-chance {@link BlockProcessorStandard},
 * default {@link EntityProcessorStandard} damage) rather than porting {@code ExplosionNT} itself -
 * the PORT_SPEC-mandated batched block removal is inherited for free from {@code ExplosionVNT}
 * either way. CE's own passed-{@code null}-detonator quirk (the block's real {@code detonator}
 * parameter is ignored for this specific variant, unlike every sibling charge block) is preserved
 * exactly rather than "fixed".
 */
public class BlockChargeDynamite extends BlockChargeBase {

    public BlockChargeDynamite(Properties properties) {
        super(properties);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;

        safe = true;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        safe = false;

        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        // CE: `new ExplosionNT(world, null, ...)` - detonator intentionally not passed through, preserved as-is.
        ExplosionVNT exp = new ExplosionVNT(level, x, y, z, 4F);
        exp.setBlockAllocator(new BlockAllocatorStandard());
        exp.setBlockProcessor(new BlockProcessorStandard());
        exp.setEntityProcessor(new EntityProcessorStandard());
        exp.setPlayerProcessor(new PlayerProcessorStandard());
        exp.explode();

        return BombReturnCode.DETONATED;
    }
}
