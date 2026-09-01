package com.hbm.blocks.generic;

import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * CE {@code BlockLanternBehemoth} — Dummyable {4,0,0,0,0,0}. Drop is AIR.
 * Torch-repair / Bobmazon reputation skipped (IRepairable not wired on this Dummyable yet).
 */
public class BlockLanternBehemoth extends BlockDummyable {

    public BlockLanternBehemoth(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new LanternBehemothBlockEntity(pos, state) : null;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
