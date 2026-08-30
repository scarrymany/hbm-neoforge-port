package com.hbm.blocks.generic;

import com.hbm.blocks.IOreType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code BlockDepthOre}: a {@link BlockDepth} that additionally drops through the
 * {@link IOreType} drop-function pattern instead of a plain self-drop, mirroring
 * {@link BlockNTMOre}'s drop mechanism on top of the depth-stone base.
 */
public class BlockDepthOre extends BlockDepth {

    @Nullable
    protected final IOreType oreType;

    public BlockDepthOre(Properties properties, @Nullable IOreType oreType) {
        super(properties);
        this.oreType = oreType;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (oreType == null) {
            return super.getDrops(state, params);
        }

        RandomSource rand = params.getLevel().getRandom();
        int count = oreType.getQuantityFunction().apply(state, 0, rand);
        List<ItemStack> drops = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = oreType.getDropFunction().apply(state, rand);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
        return drops;
    }
}
