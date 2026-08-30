package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Iterator;

/**
 * CE: {@code BlockProcessorNoDamage} - a cosmetic-only block processor: it never drops items, never
 * removes any block itself, and never calls the block-exploded notification hook. Any actual block
 * change is entirely up to the attached {@link IBlockMutator} (e.g. {@code BlockMutatorBulkie}
 * scattering a sparse ring of scorched blocks). {@code affectedBlocks} is cleared at the end purely so
 * the standard SFX implementation doesn't also spawn per-block damage particles for a set nothing was
 * actually removed from - CE's own comment on this line ("tricks the standard SFX...") is preserved.
 * <p>
 * Because {@link IBlockMutator#mutatePre}/{@code mutatePost} calls here are sparse (at most one
 * {@code setBlock} per affected position, only for positions the mutator itself chooses to touch) this
 * class does not need {@code BlockProcessorStandard}'s chunk-batched removal treatment - there is no
 * mass per-position removal loop here to begin with.
 */
public class BlockProcessorNoDamage implements IBlockProcessor {

    protected IBlockMutator convert;

    public BlockProcessorNoDamage() {
    }

    public BlockProcessorNoDamage withBlockEffect(IBlockMutator convert) {
        this.convert = convert;
        return this;
    }

    @Override
    public void process(ExplosionVNT explosion, Level level, double x, double y, double z, HashSet<BlockPos> affectedBlocks) {

        Iterator<BlockPos> iterator = affectedBlocks.iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = level.getBlockState(pos);

            if (!state.isAir()) {
                if (this.convert != null) this.convert.mutatePre(explosion, state, pos);
            }
        }

        if (this.convert != null) {
            iterator = affectedBlocks.iterator();

            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();
                BlockState state = level.getBlockState(pos);

                if (state.isAir()) {
                    this.convert.mutatePost(explosion, pos);
                }
            }
        }

        affectedBlocks.clear();
    }
}
