package com.hbm.explosion.vanillant.standard;

import com.hbm.blocks.generic.PlantBlocks;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import com.hbm.interfaces.Untested;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

/**
 * CE: {@code BlockAllocatorGlyphidDig} - CE marks this class {@code @Untested} itself; a
 * {@link BlockAllocatorBulkie}-shaped digging allocator with one extra CE-specific veto (never dig
 * through {@code ModBlocks.glyphid_spawner}) and CE's own already-1.12-outdated comment noting the
 * per-block-destroy-check method it wanted no longer existed even then ("Another removed method. May
 * cause differences in behavior").
 */
@Untested
public class BlockAllocatorGlyphidDig implements IBlockAllocator {

    protected double maximum;
    protected int resolution;

    public BlockAllocatorGlyphidDig(double maximum) {
        this(maximum, 16);
    }

    public BlockAllocatorGlyphidDig(double maximum, int resolution) {
        this.resolution = resolution;
        this.maximum = maximum;
    }

    @Override
    @Untested
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

                        double currentX = x;
                        double currentY = y;
                        double currentZ = z;

                        double dist = 0;

                        for (float stepSize = 0.3F; dist <= explosion.size; ) {

                            double deltaX = currentX - x;
                            double deltaY = currentY - y;
                            double deltaZ = currentZ - z;
                            dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                            BlockPos pos = new BlockPos(Mth.floor(currentX), Mth.floor(currentY), Mth.floor(currentZ));

                            if (!level.isInWorldBounds(pos)) break;

                            BlockState state = level.getBlockState(pos);

                            if (!state.isAir()) {
                                float blockResistance = state.getBlock().getExplosionResistance();
                                // CE: never dig through ModBlocks.glyphid_spawner (BlockAllocatorGlyphidDig.java:67)
                                if (this.maximum < blockResistance || state.is(PlantBlocks.GLYPHID_SPAWNER.get())) {
                                    break;
                                }
                            }

                            affectedBlocks.add(pos);

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
