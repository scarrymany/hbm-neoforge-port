package com.hbm.blocks.generic;

import com.hbm.saveddata.TomSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Ported from CE's {@code BlockDirt} ({@code impact_dirt}): scorched/impact-crater dirt that
 * regrows grass once enough light reaches it and the world isn't still ablaze, tracked by
 * {@link TomSaveData}. Drops vanilla dirt rather than itself, exactly like CE.
 * <p>
 * CE weighted the light check by {@code TomSaveData.dust} ({@code max(blockLight, skyLight *
 * (1 - dust))}), a nuclear-winter-simulation nuance with no direct single-call equivalent in
 * modern Minecraft's lighting API; this port uses {@link net.minecraft.world.level.LevelReader#getMaxLocalRawBrightness}
 * (vanilla's own light-weighted-by-time-of-day helper, used for crop growth) as a faithful
 * stand-in for "is it light enough here", and keeps the part of CE's gate that matters for this
 * block's own identity - regrowth is blocked entirely while {@link TomSaveData#fire} is nonzero.
 */
public class BlockDirt extends Block {

    private static final int LIGHT_THRESHOLD = 9;

    public BlockDirt(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TomSaveData data = TomSaveData.forWorld(level);

        int light = level.getMaxLocalRawBrightness(pos.above());
        if (light >= LIGHT_THRESHOLD && data.fire == 0) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            if (level.getBlockState(pos.below()).is(Blocks.DIRT)) {
                level.setBlock(pos.below(), this.defaultBlockState(), 3);
            }
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Blocks.DIRT));
    }
}
