package com.hbm.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DirPos {

    protected Direction dir;
    protected BlockPos pos;

    public DirPos(int x, int y, int z, Direction dir) {
        this.pos = new BlockPos(x, y, z);
        this.dir = dir;
    }

    public DirPos(BlockPos pos, Direction dir) {
        this.pos = pos;
        this.dir = dir;
    }

    public DirPos(BlockEntity be, Direction dir) {
        this.pos = be.getBlockPos();
        this.dir = dir;
    }

    public DirPos(double x, double y, double z, Direction dir) {
        this.pos = BlockPos.containing(x, y, z);
        this.dir = dir;
    }

    public Direction getDir() {
        return this.dir;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public boolean compare(int x, int y, int z) {
        return this.pos.getX() == x && this.pos.getY() == y && this.pos.getZ() == z;
    }
}
