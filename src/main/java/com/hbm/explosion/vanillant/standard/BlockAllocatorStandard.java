package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

/**
 * CE: {@code BlockAllocatorStandard} - reimplements vanilla {@code Explosion}'s own hollow-cube-shell
 * raycast almost verbatim: {@code resolution}^3 rays fired from a hollow cube shell (only the
 * boundary cells of the {@code resolution}^3 grid are used, giving an even sphere-ish spread of ray
 * directions), each stepping outward in 0.3-block increments while eating a power budget against the
 * traversed blocks' explosion resistance.
 * <p>
 * CE's own extra {@code Entity#canExplosionDestroyBlock} veto (an additional per-block "can this block
 * be destroyed regardless of remaining power" hook) has no surviving equivalent in modern vanilla -
 * confirmed by Neo Edition's real, compiling 1.21.1 port of this same class dropping it entirely, not
 * a CE behavior this port chose to skip.
 * <p>
 * Unlike CE (and unlike this method's own 1.12 self), the affected set is not also copied into
 * {@code explosion.compat.getToBlow()} here - {@link com.hbm.explosion.vanillant.ExplosionVNT#explode()}
 * is now the single place that syncs the compat adapter's block list, once, with the final
 * post-{@code IBlockProcessor} set (see that class's javadoc for why CE's own mid-explode
 * {@code Explosion} reconstruction is no longer needed in 1.21.1's API shape).
 */
public class BlockAllocatorStandard implements IBlockAllocator {

    protected int resolution;

    public BlockAllocatorStandard() {
        this(16);
    }

    public BlockAllocatorStandard(int resolution) {
        this.resolution = resolution;
    }

    @Override
    public HashSet<BlockPos> allocate(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {

        HashSet<BlockPos> affectedBlocks = new HashSet<>();

        for (int i = 0; i < this.resolution; ++i) {
            for (int j = 0; j < this.resolution; ++j) {
                for (int k = 0; k < this.resolution; ++k) {

                    if (i == 0 || i == this.resolution - 1 || j == 0 || j == this.resolution - 1 || k == 0 || k == this.resolution - 1) {

                        double d0 = ((float) i / ((float) this.resolution - 1.0F) * 2.0F - 1.0F);
                        double d1 = ((float) j / ((float) this.resolution - 1.0F) * 2.0F - 1.0F);
                        double d2 = ((float) k / ((float) this.resolution - 1.0F) * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;

                        float powerRemaining = size * (0.7F + level.random.nextFloat() * 0.6F);
                        double currentX = x;
                        double currentY = y;
                        double currentZ = z;

                        for (float stepSize = 0.3F; powerRemaining > 0.0F; powerRemaining -= stepSize * 0.75F) {

                            int blockX = Mth.floor(currentX);
                            int blockY = Mth.floor(currentY);
                            int blockZ = Mth.floor(currentZ);
                            BlockPos pos = new BlockPos(blockX, blockY, blockZ);

                            if (!level.isInWorldBounds(pos)) break;

                            BlockState state = level.getBlockState(pos);

                            if (!state.isAir()) {
                                float blockResistance = state.getExplosionResistance(level, pos, explosion.compat);
                                powerRemaining -= (blockResistance + 0.3F) * stepSize;
                            }

                            if (powerRemaining > 0.0F) {
                                affectedBlocks.add(pos);
                            }

                            currentX += d0 * (double) stepSize;
                            currentY += d1 * (double) stepSize;
                            currentZ += d2 * (double) stepSize;
                        }
                    }
                }
            }
        }

        return affectedBlocks;
    }
}
