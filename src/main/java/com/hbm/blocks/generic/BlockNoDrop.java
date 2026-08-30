package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Marker block that drops nothing, ported from CE's {@code BlockNoDrop}.
 */
public class BlockNoDrop extends BlockBase {

    public BlockNoDrop(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
