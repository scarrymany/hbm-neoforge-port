package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.inventory.RecipesCommon.MetaBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * CE: {@code BlockMutatorBulkie} - if a still-solid affected block sits at (or beyond) the very edge
 * of the blast radius ({@code distance >= size - 0.5}), replace it in place with a configured
 * "scorched shell" block instead of leaving it untouched, before any removal happens.
 * <p>
 * CE's {@code Vec3NT} distance helper is inlined as plain {@link Vec3} math (no CE-specific vector
 * utility class exists in this port, and none is needed for a single length() call).
 * <p>
 * Note: CE's own solidity gate here reads the block state actually present at {@code pos} (the
 * parameter passed in by {@code BlockProcessorStandard}), not the mutator's own configured
 * replacement state - preserved faithfully. (Neo Edition's own port of this class checks its
 * replacement state's solidity instead, which would make the gate constant regardless of what block
 * is actually at {@code pos}; not followed here, per this port's ground rule of consulting Neo Edition
 * for API shape only, never behavior.)
 */
public class BlockMutatorBulkie implements IBlockMutator {

    protected MetaBlock metaBlock;

    public BlockMutatorBulkie(String loc) {
        this(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(loc)).map(Block::defaultBlockState).orElse(Blocks.STONE.defaultBlockState()));
    }

    public BlockMutatorBulkie(Block block) {
        this(block.defaultBlockState());
    }

    public BlockMutatorBulkie(BlockState state) {
        this.metaBlock = new MetaBlock(state.getBlock(), state);
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos) {

        if (!state.isSolidRender(explosion.world, pos)) return;

        Vec3 vec = new Vec3(pos.getX() + 0.5 - explosion.posX, pos.getY() + 0.5 - explosion.posY, pos.getZ() + 0.5 - explosion.posZ);

        if (vec.length() >= explosion.size - 0.5) {
            explosion.world.setBlock(pos, metaBlock.state, 3);
        }
    }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
    }
}
