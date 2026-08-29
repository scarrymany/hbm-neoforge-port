package com.hbm.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

public interface IBlockSideRotation {
    int getRotationFromSide(BlockGetter world, BlockPos pos, Direction side);

    // 0 1 3 2 becomes 0 2 3 1
    // I want to smoke that swedish kush because it clearly makes you fucking stupid
    static int topToBottom(int topRotation) {
        return switch (topRotation) {
            case 1 -> 2;
            case 2 -> 1;
            default -> topRotation;
        };
    }

    static boolean isOpposite(int from, int to) {
        return switch (from) {
            case 0 -> to == 1;
            case 1 -> to == 0;
            case 2 -> to == 3;
            case 3 -> to == 2;
            case 4 -> to == 5;
            case 5 -> to == 4;
            default -> false;
        };
    }
}
