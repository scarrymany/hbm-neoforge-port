package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Ported from CE's {@code BlockReeds}: a decorative reed/cane block that only survives on top of
 * water. CE backed this with a dedicated tile entity ({@code TileEntityReeds}) purely to store
 * nothing of consequence for rendering purposes, and rendered it through a bespoke
 * {@code BlockReedsBakedModel} (3-part bottom/mid/top texture stack) registered via CE's
 * runtime-model-baking system. Per this area's ground rules neither survives: no tile entity is
 * created, and the block gets whatever datagen model {@code ModBlockStateProvider}'s default
 * cube-all path produces (the same simplification already accepted for the sibling
 * {@link BlockDeadPlant}/{@link BlockNTMFlower}/{@link BlockTallPlant} cross-shaped plants in this
 * area). CE's item drop ({@code Items.STICK} rather than itself) is preserved.
 */
public class BlockReeds extends BushBlock {

    public static final MapCodec<BlockReeds> CODEC = simpleCodec(BlockReeds::new);

    public BlockReeds(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BlockReeds> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.WATER);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.STICK));
    }
}
