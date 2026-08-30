package com.hbm.blocks.generic;

import com.hbm.blocks.PlantEnums.EnumDeadPlantType;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code BlockDeadPlant}: a purely decorative dead-plant set with no growth
 * behavior. CE modelled this as a {@code BlockPlantEnumMeta<EnumDeadPlantType>} with metadata
 * variants; each {@link EnumDeadPlantType} constant is its own registered block here (see
 * {@link PlantBlocks}). CE also overrode {@code dropBlockAsItemWithChance} to a no-op (these blocks
 * never drop anything); the modern equivalent is {@code Properties.noLootTable()}, applied when the
 * block is constructed rather than in this class.
 */
public class BlockDeadPlant extends BushBlock {

    public static final MapCodec<BlockDeadPlant> CODEC = simpleCodec(BlockDeadPlant::new);

    public final EnumDeadPlantType type;

    public BlockDeadPlant(Properties properties) {
        this(properties, EnumDeadPlantType.GENERIC);
    }

    public BlockDeadPlant(Properties properties, EnumDeadPlantType type) {
        super(properties);
        this.type = type;
    }

    @Override
    protected MapCodec<? extends BlockDeadPlant> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return PlantBlocks.isDeadPlantGround(state.getBlock());
    }
}
