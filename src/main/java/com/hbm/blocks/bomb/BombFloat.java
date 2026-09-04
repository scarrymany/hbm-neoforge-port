package com.hbm.blocks.bomb;

import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionNukeGeneric;
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
 * Exact CE {@code BombFloat.java:37-56}. {@code float_bomb}: {@code floater}/{@code move}.
 * {@code emp_bomb}: {@code empBlast} + {@link EntityEMPBlast}.
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
                ExplosionChaos.floater(level, detonator, pos, 15, 50);
                ExplosionChaos.move(level, pos, 15, 0, 50, 0);
            }
            if (this == BombBlocks.EMP_BOMB.get()) {
                ExplosionNukeGeneric.empBlast(level, detonator, pos.getX(), pos.getY(), pos.getZ(), 50);
                level.addFreshEntity(EntityEMPBlast.create(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 50));
            }
        }

        return BombReturnCode.DETONATED;
    }
}
