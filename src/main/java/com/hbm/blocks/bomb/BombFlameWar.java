package com.hbm.blocks.bomb;

import com.hbm.explosion.ExplosionChaos;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exact CE {@code BombFlameWar.java:31-37}: {@code explode}/{@code spawnExplosion}/{@code flameDeath}.
 */
public class BombFlameWar extends Block implements IBomb {

    public BombFlameWar(Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (!level.isClientSide()) {
            ExplosionChaos.explode(level, detonator, pos.getX(), pos.getY(), pos.getZ(), 15);
            ExplosionChaos.spawnExplosion(level, detonator, pos.getX(), pos.getY(), pos.getZ(), 75);
            ExplosionChaos.flameDeath(level, detonator, pos, 100);
        }
        return BombReturnCode.DETONATED;
    }
}
