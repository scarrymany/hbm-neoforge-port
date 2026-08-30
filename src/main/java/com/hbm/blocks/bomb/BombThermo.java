package com.hbm.blocks.bomb;

import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BombThermo} (67 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. Two registered variants
 * ({@link BombBlocks#THERM_ENDO}/{@link BombBlocks#THERM_EXO}) differentiated by identity. CE's
 * {@code ExplosionThermo.freeze}/{@code freezer} (endothermic) and {@code scorch}/
 * {@code setEntitiesOnFire} (exothermic) calls are not ported (out of this task's read scope,
 * flagged as a forward reference by the research report) and are left as documented TODOs; the
 * real vanilla {@code world.createExplosion(detonator, x, y, z, 5.0F, true)} tail call CE always
 * performs regardless of variant is fully ported and fires for both.
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

        if (this == BombBlocks.THERM_ENDO.get()) {
            // TODO(ExplosionThermo, forward reference): CE calls ExplosionThermo.freeze(world,
            // detonator, x, y, z, 15) and .freezer(world, x, y, z, 20) here - not ported.
        }
        if (this == BombBlocks.THERM_EXO.get()) {
            // TODO(ExplosionThermo, forward reference): CE calls ExplosionThermo.scorch(world,
            // detonator, x, y, z, 15) and .setEntitiesOnFire(world, x, y, z, 20) here - not ported.
        }

        level.explode(detonator, pos.getX(), pos.getY(), pos.getZ(), 5.0F, true, Level.ExplosionInteraction.TNT);
        return BombReturnCode.DETONATED;
    }
}
