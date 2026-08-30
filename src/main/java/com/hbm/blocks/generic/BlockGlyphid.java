package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

/**
 * Ported from CE's {@code BlockGlyphid}: a purely cosmetic Glyphid-themed wall texture set (no
 * tile entity, no entity import - confirmed by {@code docs/phase1/blocks_generic.md}). CE modelled
 * the 3 variants as one {@code IBlockMulti} block with a {@code PropertyEnum<Type>}; each
 * {@link Type} constant is its own registered block here (see {@link PlantBlocks}).
 * CE's {@code getItemDropped} returning {@code Items.AIR} (never drops itself) is expressed as
 * {@code Properties.noLootTable()} at construction time, matching {@link BlockDeadPlant}'s note.
 */
public class BlockGlyphid extends Block {

    public static final MapCodec<BlockGlyphid> CODEC = simpleCodec(BlockGlyphid::new);

    public final Type type;

    public BlockGlyphid(Properties properties) {
        this(properties, Type.BASE);
    }

    public BlockGlyphid(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    @Override
    protected MapCodec<? extends BlockGlyphid> codec() {
        return CODEC;
    }

    public enum Type {
        BASE, INFESTED, RAD;

        public static final Type[] VALUES = values();
    }
}
