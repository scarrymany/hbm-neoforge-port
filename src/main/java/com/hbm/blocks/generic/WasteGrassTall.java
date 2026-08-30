package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code WasteGrassTall}: contaminated tall-grass decoration that may only be
 * placed on {@link PlantBlocks#isWasteGround waste earth/mycelium}. CE's 0-6 "which texture
 * variant" metadata property is dropped for the same reason documented on {@link WasteEarth} (CE's
 * own model JSONs never actually branched on it), and the vanilla-item drop-suppression
 * ({@code getItemDropped} returning {@code Items.AIR}) is expressed as {@code Properties.noLootTable()}
 * at construction time.
 */
public class WasteGrassTall extends BushBlock {

    public static final MapCodec<WasteGrassTall> CODEC = simpleCodec(WasteGrassTall::new);

    public WasteGrassTall(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends WasteGrassTall> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return PlantBlocks.isWasteGround(state.getBlock());
    }
}
