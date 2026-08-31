package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;

/**
 * Ported from CE's {@code WasteSand extends BlockFalling}: contaminated falling-sand reskins
 * ({@code waste_trinitite}/{@code waste_trinitite_red}). CE's identity-checked radiation potion
 * effect ({@code HbmPotion.radiation}) and trinitite-item drop substitution are both dropped for
 * now: the radiation potion depends on the not-yet-ported radiation content system (see
 * {@link WasteEarth}'s javadoc for the same call), and no {@code trinitite} item exists yet in
 * this port to substitute in ({@code com.hbm.items.ModItems.trinitite} in CE) - both self-drop
 * like a plain falling block until those areas exist. CE's 0-6 "which texture variant" metadata
 * property is dropped for the same reason documented on {@link WasteEarth}.
 */
public class WasteSand extends FallingBlock {

    public static final MapCodec<WasteSand> CODEC = simpleCodec(WasteSand::new);

    public WasteSand(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WasteSand> codec() {
        return CODEC;
    }
}
