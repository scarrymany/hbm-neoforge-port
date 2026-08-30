package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BlockChargeSemtex} (55 lines, read in full). No
 * entity/player processor is set, so it does no entity damage (matches CE's own tooltip claim);
 * drops every affected block with Fortune III. See {@link BlockChargeC4}'s javadoc for the
 * un-ported {@code ExplosionCreator} particle-composite note.
 */
public class BlockChargeSemtex extends BlockChargeBase {

    public BlockChargeSemtex(Properties properties) {
        super(properties);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;

        safe = true;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        safe = false;

        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        ExplosionVNT xnt = new ExplosionVNT(level, x, y, z, 10F);
        xnt.setBlockAllocator(new BlockAllocatorStandard(32));
        xnt.setBlockProcessor(new BlockProcessorStandard().setAllDrop().setFortune(3));
        xnt.explode();

        return BombReturnCode.DETONATED;
    }
}
