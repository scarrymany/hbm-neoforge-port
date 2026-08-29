package com.hbm.api.energymk2;

import com.hbm.api.tile.ILoadedTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * DO NOT USE DIRECTLY! This is simply the common ancestor to providers and receivers, because
 * all this behavior has to be excluded from conductors!
 */
public interface IEnergyHandlerMK2 extends IEnergyConnectorMK2, ILoadedTile {

    long getPower();
    void setPower(long power);
    long getMaxPower();

    boolean particleDebug = false;

    default Vec3 getDebugParticlePosMK2() {
        BlockEntity self = (BlockEntity) this;
        BlockPos pos = self.getBlockPos();
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
    }
}
