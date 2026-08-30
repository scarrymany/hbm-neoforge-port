package com.hbm.blocks.generic;

import com.hbm.config.GeneralConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code WasteMycelium extends WasteEarth}: spreads onto adjacent dirt/grass/
 * mycelium/waste-earth blocks (gated by {@link GeneralConfig#ENABLE_MYCELIUM_SPREAD}, CE's
 * {@code enableMycelium}) in addition to the base {@link WasteEarth#randomTick} auto-cleanup.
 * {@code waste_mycelium} has real {@code _side}/{@code _top} texture assets, so it renders via the
 * inherited {@link WasteEarth#registerModel} (kind {@link Kind#WASTE} - vanilla-dirt bottom,
 * exactly like vanilla's own Mycelium block), no override needed here.
 * <p>
 * Not ported: the {@code TOWN_AURA} ambient particle CE spawned every {@code animateTick}, since
 * that vanilla 1.12 particle (villager-happy sparkle) has no direct 1.21 equivalent tied to a
 * ground block in the same way; the radiation-on-walk effect is dropped for the same
 * missing-radiation-system reason documented on {@link WasteEarth}.
 */
public class WasteMycelium extends WasteEarth {

    public WasteMycelium(Properties properties) {
        super(properties, Kind.WASTE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (GeneralConfig.ENABLE_MYCELIUM_SPREAD.get()) {
            spread(level, pos, random);
        }
        super.randomTick(state, level, pos, random);
    }

    private void spread(ServerLevel level, BlockPos pos, RandomSource random) {
        List<BlockPos> validPositions = new ArrayList<>();
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    BlockPos adjacent = pos.offset(x, y, z);
                    Block adjacentBlock = level.getBlockState(adjacent).getBlock();
                    BlockState aboveAdjacent = level.getBlockState(adjacent.above());
                    if (!aboveAdjacent.canOcclude() && isSpreadableGround(adjacentBlock)) {
                        validPositions.add(adjacent);
                    }
                }
            }
        }
        if (!validPositions.isEmpty()) {
            BlockPos target = validPositions.get(random.nextInt(validPositions.size()));
            level.setBlock(target, this.defaultBlockState(), 3);
        }
    }

    private boolean isSpreadableGround(Block block) {
        return block == Blocks.DIRT
                || block == Blocks.GRASS_BLOCK
                || block == Blocks.MYCELIUM
                || block == PlantBlocks.WASTE_EARTH.get();
    }
}
