package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums.LightstoneType;
import net.minecraft.world.level.block.Block;

/**
 * Decorative light-emitting stone family, ported from CE's generic {@code BlockLightstone<E>}. CE's
 * type parameter only ever backed one concrete instance ({@code lightstone}, keyed off
 * {@link LightstoneType}), so the port drops the now-pointless generic and specializes directly on
 * that enum. Per this port's flattening rule each {@link LightstoneType} constant becomes its own
 * registered block built from this one class (see {@link GenericBlocks}); CE's per-variant
 * "top-and-bottom vs. all-sides" texture split (the {@code i >= 3} branch in CE's
 * {@code generateBlockFrames}) is a datagen model concern, not a Java behavior, under this port's
 * ground rule.
 */
public class BlockLightstone extends Block {

    public final LightstoneType type;

    public BlockLightstone(Properties properties, LightstoneType type) {
        super(properties);
        this.type = type;
    }
}
