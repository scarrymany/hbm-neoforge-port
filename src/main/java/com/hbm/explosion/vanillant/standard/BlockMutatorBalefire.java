package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE: {@code BlockMutatorBalefire} - 1-in-3 chance to convert a crater position that ended up air into
 * {@code ModBlocks.balefire} (persistent radioactive fire), provided the block below is solid.
 * <p>
 * {@code ModBlocks.balefire} is not registered in this port yet (no balefire block exists under
 * {@code com.hbm.blocks} as of this pass) - left as a documented forward-reference no-op below rather
 * than invented, per this port's established convention for not-yet-ported block dependencies (see
 * e.g. {@code RBMKBaseBlockEntity#standardMelt}).
 */
public class BlockMutatorBalefire implements IBlockMutator {

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState blockState, BlockPos pos) {
    }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = explosion.world.getBlockState(below);

        if (explosion.world.getBlockState(pos).isAir() && belowState.isSolidRender(explosion.world, below)
                && explosion.world.getRandom().nextInt(3) == 0) {
            // forward reference: com.hbm.blocks.ModBlocks.balefire - not registered in this port yet.
            // CE: explosion.world.setBlockState(pos, ModBlocks.balefire.getDefaultState());
        }
    }
}
