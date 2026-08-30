package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums.PlatemetalType;
import net.minecraft.world.level.block.Block;

/**
 * Stacked metal plate decoration, ported from CE's {@code BlockPlatemetal}. CE hand-rolled its own
 * {@code PropertyInteger META} (0 through {@code PlatemetalType.VALUES.length - 1}) purely to pick
 * one of fifteen plate-color textures per item-metadata value - there was never any placement-time
 * behavior tied to it. Per this port's flattening rule (the same one already used for
 * {@link BlockMeteorOre}/{@link BlockResourceStone}/{@link BlockStalagmite}), each
 * {@link PlatemetalType} constant becomes its own registered block built from this one class,
 * carrying its variant as a plain constructor-time field rather than a real NeoForge blockstate
 * {@code IntegerProperty} - there is no per-instance behavioral difference to key off of, so a real
 * block-state property would only add an axis with no gameplay meaning.
 */
public class BlockPlatemetal extends Block {

    public final PlatemetalType type;

    public BlockPlatemetal(Properties properties, PlatemetalType type) {
        super(properties);
        this.type = type;
    }
}
