package com.hbm.blocks.generic;

import com.hbm.api.block.IDrillInteraction;
import com.hbm.api.block.IMiningDrill;
import com.hbm.blocks.IOreType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from CE's {@code BlockCluster extends BlockNTMOre implements IDrillInteraction}: a
 * mining-drill-only ore variant that drops nothing on an ordinary break (matching CE's
 * {@code getItemDropped} returning {@code Items.AIR}) and only yields its {@link IOreType} drop
 * through {@link IMiningDrill} interaction once the drill meets the rating threshold.
 */
public class BlockCluster extends BlockNTMOre implements IDrillInteraction {

    private static final int MIN_DRILL_RATING = 30;

    public BlockCluster(Properties properties, @Nullable IOreType oreType) {
        super(properties, oreType);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Nullable
    private ItemStack rollDrop(RandomSource rand, BlockState state) {
        return oreType == null ? null : oreType.getDropFunction().apply(state, rand);
    }

    @Override
    public boolean canBreak(Level world, int x, int y, int z, BlockState state, IMiningDrill drill) {
        return drill.getDrillRating() >= MIN_DRILL_RATING;
    }

    @Override
    public ItemStack extractResource(Level world, int x, int y, int z, BlockState state, IMiningDrill drill) {
        if (drill.getDrillRating() < MIN_DRILL_RATING) {
            return ItemStack.EMPTY;
        }
        ItemStack drop = rollDrop(world.getRandom(), state);
        return drop == null ? ItemStack.EMPTY : drop;
    }

    @Override
    public float getRelativeHardness(Level world, int x, int y, int z, BlockState state, IMiningDrill drill) {
        return state.getDestroySpeed(world, new BlockPos(x, y, z));
    }
}
