package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;

/**
 * Block that can host a beacon base, ported from CE's {@code BlockBeaconable}. Modern Minecraft
 * decides beacon-base eligibility from the {@code minecraft:beacon_base_blocks} block tag rather
 * than a per-block method override (CE's own {@code isBeaconBase} hook no longer exists on 1.21's
 * {@code Block}); this class carries no code, and every concrete instance (e.g. {@code block_cadmium},
 * {@code block_bismuth}) must additionally be added to that tag via datagen for the beacon-base
 * behavior to actually apply - a datapack-side follow-up, not a Java gap.
 */
public class BlockBeaconable extends BlockBase {

    public BlockBeaconable(Properties properties) {
        super(properties);
    }
}
