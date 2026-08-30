package com.hbm.items.machine;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Trivial subclass of {@link ItemTurretBiometry} that no-ops block-use. CE's author left a
 * {@code //FIXME...?} marking the split from its parent as unclear, reproduced here unchanged -
 * nothing in either class requires a tile entity.
 */
public class ItemTurretChip extends ItemTurretBiometry {

    public ItemTurretChip(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }
}
