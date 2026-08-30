package com.hbm.blocks.generic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Ported from CE's {@code BlockMushHuge}: the cap/stem blocks of the huge-mushroom structure CE's
 * {@code HugeMush} world-gen feature builds (not itself ported here, see {@link BlockMush}). Drops
 * 0-2 {@link BlockMush} items (CE's {@code rand.nextInt(10) - 7}, clamped to non-negative).
 */
public class BlockMushHuge extends Block {

    public BlockMushHuge(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        RandomSource random = params.getLevel().getRandom();
        int count = Math.max(0, random.nextInt(10) - 7);
        return count == 0 ? List.of() : List.of(new ItemStack(PlantBlocks.MUSH.get(), count));
    }
}
