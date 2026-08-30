package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE: {@code BlockMutatorFire} - 1-in-3 chance to ignite a crater position that ended up air, provided
 * the block directly below it is solid. CE's {@code isOpaqueCube()} check becomes
 * {@code BlockState#isSolidRender} (the closest surviving 1.21.1 equivalent for "is this a normal
 * full solid block face" - confirmed against Neo Edition's real, compiling port of this same class).
 */
public class BlockMutatorFire implements IBlockMutator {

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos) {
    }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {

        BlockState state = explosion.world.getBlockState(pos);
        BlockPos below = pos.below();
        BlockState belowState = explosion.world.getBlockState(below);

        if (state.isAir() && belowState.isSolidRender(explosion.world, below) && explosion.world.getRandom().nextInt(3) == 0) {
            explosion.world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
        }
    }
}
