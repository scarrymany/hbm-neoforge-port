package com.hbm.util;

import net.minecraft.core.Direction;

public class FacingUtil {
    public static float getPitch(Direction facing) {
        if (facing == Direction.UP) return (float) Math.PI * -0.5F;
        if (facing == Direction.DOWN) return (float) Math.PI * 0.5F;
        return 0;
    }

    public static float getYaw(Direction facing) {
        if (facing == Direction.NORTH) return (float) Math.PI * 0.5f;
        if (facing == Direction.SOUTH) return (float) Math.PI * -0.5f;
        if (facing == Direction.WEST) return (float) Math.PI;
        return 0;
    }
}
