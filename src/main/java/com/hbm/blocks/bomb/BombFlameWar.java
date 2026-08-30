package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BombFlameWar} (40 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. CE's own {@code ExplosionChaos}
 * dependency (all three calls - {@code explode}/{@code spawnExplosion}/{@code flameDeath}) is not
 * ported (out of this task's read scope, flagged as a forward reference by the research report);
 * per this task's explicit instruction this is approximated with a single equivalent-size
 * {@link ExplosionVNT} standard explosion so the block remains a real, functioning bomb rather than
 * a dead stub, clearly documented as a non-faithful substitute for the exact CE numbers/behavior.
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
            // TODO(ExplosionChaos, forward reference): CE calls ExplosionChaos.explode(world,
            // detonator, x, y, z, 15), .spawnExplosion(world, detonator, x, y, z, 75) and
            // .flameDeath(world, detonator, pos, 100) here - none of that class is ported. This is
            // a documented approximation, not the real CE numbers.
            ExplosionVNT vnt = new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15F, detonator);
            vnt.makeStandard();
            vnt.explode();
        }
        return BombReturnCode.DETONATED;
    }
}
