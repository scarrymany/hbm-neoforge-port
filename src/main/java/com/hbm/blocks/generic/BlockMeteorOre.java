package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums.EnumMeteorType;
import net.minecraft.world.level.block.Block;

/**
 * Ported from CE's {@code BlockMeteorOre}: a meteorite ore-type family with no behavior beyond the
 * variant identity itself (CE's subclass added nothing over {@code BlockEnumMeta}). One
 * {@link EnumMeteorType} constant becomes one registered block (see {@link OreMineralBlocks}).
 */
public class BlockMeteorOre extends Block {

    public final EnumMeteorType type;

    public BlockMeteorOre(Properties properties, EnumMeteorType type) {
        super(properties);
        this.type = type;
    }
}
