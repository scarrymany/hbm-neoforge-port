package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.HashSet;

/**
 * CE: {@code BlockAllocatorWater} - identical raycast to {@link BlockAllocatorStandard} except fluid
 * blocks are never consumed for power and never added to the affected set (used by underwater/
 * amphibious weapon explosions so they don't instantly boil away the water around them). CE's
 * {@code Material.isLiquid()} check becomes {@code FluidState#isEmpty()} (the 1.21.1 equivalent -
 * confirmed against Neo Edition's real, compiling port of this same class).
 */
public class BlockAllocatorWater implements IBlockAllocator {

    protected int resolution;

    public BlockAllocatorWater(int resolution) {
        this.resolution = resolution;
    }

    @Override
    public HashSet<BlockPos> allocate(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {
        HashSet<BlockPos> affectedBlocks = new HashSet<>();

        for (int i = 0; i < this.resolution; ++i) {
            for (int j = 0; j < this.resolution; ++j) {
                for (int k = 0; k < this.resolution; ++k) {
                    if (i == 0 || i == this.resolution - 1 || j == 0 || j == this.resolution - 1 || k == 0 || k == this.resolution - 1) {
                        double d0 = (float) i / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                        double d1 = (float) j / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                        double d2 = (float) k / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
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
                            FluidState fluid = level.getFluidState(pos);

                            if (!state.isAir() && fluid.isEmpty()) {
                                float blockResistance = state.getExplosionResistance(level, pos, explosion.compat);
                                powerRemaining -= (blockResistance + 0.3F) * stepSize;
                            }

                            if (powerRemaining > 0.0F && fluid.isEmpty()) {
                                affectedBlocks.add(pos);
                            }

                            currentX += d0 * stepSize;
                            currentY += d1 * stepSize;
                            currentZ += d2 * stepSize;
                        }
                    }
                }
            }
        }

        return affectedBlocks;
    }
}
