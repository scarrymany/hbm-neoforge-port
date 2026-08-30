package com.hbm.blocks.generic;

import com.hbm.items.ItemEnums.EnumCokeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Coke block (coal/lignite/petroleum coking byproduct), ported from CE's {@code BlockCoke}. CE
 * folded all three {@link EnumCokeType} textures into one {@code BlockEnumMeta} instance; per this
 * port's flattening rule each type becomes its own registered block built from this one class (see
 * {@link GenericBlocks}). The fixed flammability/fire-spread-speed values are real per-block
 * behavior (not model/texture concerns) and are ported as-is - signature confirmed against the Neo
 * Edition reference's own {@code FlammableBlock}.
 */
public class BlockCoke extends Block {

    private static final int FLAMMABILITY = 5;
    private static final int FIRE_SPREAD_SPEED = 10;

    public final EnumCokeType type;

    public BlockCoke(Properties properties, EnumCokeType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return FLAMMABILITY;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return FIRE_SPREAD_SPEED;
    }
}
