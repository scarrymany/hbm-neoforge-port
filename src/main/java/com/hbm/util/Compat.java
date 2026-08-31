package com.hbm.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Narrow gap-fill, not a redesign: {@link com.hbm.api.energymk2.IEnergyReceiverMK2#trySubscribe}
 * and {@link com.hbm.api.fluidmk2.IFluidReceiverMK2#trySubscribe} (both already-shipped Phase 0
 * default methods) call {@code Compat.getBlockEntityStandard(Level, BlockPos)} - both interfaces'
 * own javadoc flags this as "a forward reference - {@code Compat} doesn't exist in this port yet".
 * Confirmed by search: no {@code com.hbm.util.Compat} existed anywhere in the tree, which means
 * BOTH of those already-landed interfaces fail to compile as soon as anything in the module
 * references them - not a hypothetical gap, a real one blocking this Phase 2 power-generation
 * package (every fluid-receiving generator here calls {@code trySubscribe}).
 * <p>
 * This class supplies exactly the one static method both call sites need, doing exactly what CE's
 * own {@code Compat.getTileEntity} did: avoid force-loading/creating a block entity in an unloaded
 * chunk by checking {@link Level#isLoaded(BlockPos)} first. Nothing else from CE's much larger
 * {@code com.hbm.util.Compat} (cross-mod compatibility helpers) is ported - only this one method,
 * named identically so the two existing call sites resolve unchanged.
 */
public final class Compat {

    private Compat() {
    }

    public static final class ModIds {
        public static final String AE2 = "ae2";

        private ModIds() {
        }
    }

    /**
     * @return the block entity at {@code pos}, or {@code null} if the chunk isn't loaded (avoiding
     * a forced chunk load) or no block entity is present.
     */
    public static BlockEntity getBlockEntityStandard(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        return level.getBlockEntity(pos);
    }
}
