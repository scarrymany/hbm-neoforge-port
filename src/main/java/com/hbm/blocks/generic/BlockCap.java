package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums.EnumBlockCapType;
import net.minecraft.world.level.block.Block;

/**
 * Bottle-cap decorative block, ported from CE's {@code BlockCap}. CE folded all six
 * {@link EnumBlockCapType} textures into one {@code BlockEnumMeta} instance and dropped a large
 * stack of the matching cap item on break; per this port's flattening rule each type becomes its
 * own registered block built from this one class (see {@link GenericBlocks}), and the bonus-item
 * drop is left to this block's loot table - {@link EnumBlockCapType} no longer carries a
 * {@code getDrop()}/{@code getDropCount()} pair (see that enum's own javadoc for why), and there is
 * no Java {@code getDrops} hook left on {@code Block} in modern Minecraft to hang it on regardless.
 */
public class BlockCap extends Block {

    public final EnumBlockCapType type;

    public BlockCap(Properties properties, EnumBlockCapType type) {
        super(properties);
        this.type = type;
    }
}
