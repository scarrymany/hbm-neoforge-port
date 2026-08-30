package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.inventory.RecipesCommon.MetaBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE: {@code BlockMutatorDebris} - after a crater block ends up air, if any of its 6 face-neighbors is
 * still a normal solid block of a <em>different</em> type than the configured debris block, replace
 * this position with that debris block (a decorative "rubble scattered near the crater edge" effect).
 * CE's {@code Block + int meta} pair becomes this port's already-established
 * {@link MetaBlock}({@link Block}, {@link BlockState}) pair (see that class's own javadoc - the 1.21.1
 * replacement for CE's metadata system, no {@code META_POOLS} interning needed).
 */
public class BlockMutatorDebris implements IBlockMutator {

    protected MetaBlock metaBlock;

    public BlockMutatorDebris(Block block) {
        this(block.defaultBlockState());
    }

    public BlockMutatorDebris(String loc) {
        this(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(loc)).map(Block::defaultBlockState).orElse(Blocks.STONE.defaultBlockState()));
    }

    public BlockMutatorDebris(BlockState state) {
        this.metaBlock = new MetaBlock(state.getBlock(), state);
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState blockState, BlockPos pos) {
    }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
        Level level = explosion.world;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState state = level.getBlockState(neighborPos);
            Block adjacentBlock = state.getBlock();

            if (state.isSolidRender(level, neighborPos) && (adjacentBlock != metaBlock.block || !state.equals(metaBlock.state))) {
                level.setBlock(pos, metaBlock.state, 3);
                return;
            }
        }
    }
}
