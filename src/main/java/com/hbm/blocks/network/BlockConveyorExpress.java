package com.hbm.blocks.network;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.blocks.network.BlockConveyorExpress} (read in full) - a 3x speed
 * variant of {@link BlockConveyorBendable}. See {@link BlockConveyor}'s javadoc for the
 * {@code ItemConveyorWand} pick/drop deferral.
 */
public class BlockConveyorExpress extends BlockConveyorBendable {

    public BlockConveyorExpress(Properties properties) {
        super(properties);
    }

    @Override
    public Vec3 getTravelLocation(Level world, int x, int y, int z, Vec3 itemPos, double speed) {
        return super.getTravelLocation(world, x, y, z, itemPos, speed * 3);
    }
}
