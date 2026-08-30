package com.hbm.blocks.generic;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

/**
 * Colored concrete, ported from CE's {@code BlockConcreteColored}. CE built one
 * {@code BlockEnumMeta<EnumDyeColor>} instance covering all sixteen {@link DyeColor}s via item
 * metadata; per this port's flattening rule each color becomes its own registered block built from
 * this one class (see {@link GenericBlocks} for the sixteen registrations), matching vanilla's own
 * choice to register sixteen separate {@code *_CONCRETE} blocks rather than one metadata block.
 */
public class BlockConcreteColored extends Block {

    public final DyeColor color;

    public BlockConcreteColored(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }
}
