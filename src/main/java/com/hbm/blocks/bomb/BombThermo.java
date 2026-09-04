package com.hbm.blocks.bomb;

import com.hbm.explosion.ExplosionThermo;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exact CE {@code com.hbm.blocks.bomb.BombThermo}. Identity-split
 * {@link BombBlocks#THERM_ENDO}/{@link BombBlocks#THERM_EXO}. Thermo first, then vanilla
 * {@code createExplosion(detonator, 5.0F, true)}.
 */
public class BombThermo extends Block implements IBomb {

    public BombThermo(Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        // Exact CE BombThermo.java:48-63
        if (this == BombBlocks.THERM_ENDO.get()) {
            ExplosionThermo.freeze(level, detonator, pos.getX(), pos.getY(), pos.getZ(), 15);
            ExplosionThermo.freezer(level, pos.getX(), pos.getY(), pos.getZ(), 20);
        }
        if (this == BombBlocks.THERM_EXO.get()) {
            ExplosionThermo.scorch(level, detonator, pos.getX(), pos.getY(), pos.getZ(), 15);
            ExplosionThermo.setEntitiesOnFire(level, pos.getX(), pos.getY(), pos.getZ(), 20);
        }

        level.explode(detonator, pos.getX(), pos.getY(), pos.getZ(), 5.0F, true, Level.ExplosionInteraction.TNT);
        return BombReturnCode.DETONATED;
    }
}
