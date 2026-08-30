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
 * Ported from CE's {@code com.hbm.blocks.bomb.BlockChargeC4} (61 lines, read in full) - concrete
 * demonstration of {@code ExplosionVNT}'s real intended usage shape per
 * {@code docs/phase3/bomb_blocks_and_detonators.md}. No block drops when detonated.
 * <p>
 * CE's own additional {@code ExplosionCreator.composeEffectSmall} particle composite is not ported
 * here - that helper class (164 lines, purely client-side cosmetic VFX, no gameplay effect) does
 * not exist in this port and is out of this package's scope; the real block-removal and
 * damage/knockback behavior is unaffected.
 */
public class BlockChargeC4 extends BlockChargeBase {

    public BlockChargeC4(Properties properties) {
        super(properties);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;

        safe = true;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        safe = false;

        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        ExplosionVNT xnt = new ExplosionVNT(level, x, y, z, 15F, detonator);
        xnt.setBlockAllocator(new BlockAllocatorStandard(32));
        xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
        xnt.setEntityProcessor(new EntityProcessorStandard());
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();

        return BombReturnCode.DETONATED;
    }
}
