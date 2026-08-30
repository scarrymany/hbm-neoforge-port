package com.hbm.blocks.bomb;

import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.interfaces.IBomb;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BombFloat} (59 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. Two registered variants
 * ({@link BombBlocks#FLOAT_BOMB}/{@link BombBlocks#EMP_BOMB}) differentiated by identity, matching
 * CE. The {@code emp_bomb} branch is fully real ({@link ExplosionNukeGeneric#empBlast} and
 * {@link EntityEMPBlast} are both already committed in this port's foundation wave); the plain
 * {@code float_bomb} branch's {@code ExplosionChaos.floater}/{@code move} dependency is not ported
 * (out of this task's read scope, flagged as a forward reference by the research report) and is
 * approximated here with a documented {@link ExplosionVNT} standin per this task's explicit
 * instruction.
 */
public class BombFloat extends Block implements IBomb {

    public BombFloat(Properties properties) {
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
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HBMSoundHandler.sparkShoot.get(), SoundSource.BLOCKS,
                5.0F, level.getRandom().nextFloat() * 0.2F + 0.9F);

        if (!level.isClientSide()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

            if (this == BombBlocks.FLOAT_BOMB.get()) {
                // TODO(ExplosionChaos, forward reference): CE calls ExplosionChaos.floater(world,
                // detonator, pos, 15, 50) then .move(world, pos, 15, 0, 50, 0) here - neither
                // ExplosionChaos nor its floating-island-relocation behavior is ported. Approximated
                // with an equivalent-size standard explosion so the block remains a functioning bomb.
                ExplosionVNT vnt = new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15F, detonator);
                vnt.makeStandard();
                vnt.explode();
            }
            if (this == BombBlocks.EMP_BOMB.get()) {
                ExplosionNukeGeneric.empBlast(level, detonator, pos.getX(), pos.getY(), pos.getZ(), 50);
                level.addFreshEntity(EntityEMPBlast.create(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 50));
            }
        }

        return BombReturnCode.DETONATED;
    }
}
