package com.hbm.blocks.bomb;

import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.DetMiner} (64 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. Redstone-triggered all-drop/no-hurt
 * charge, same {@code ExplosionVNT} approximation of the un-ported {@code ExplosionNT} as
 * {@link BlockChargeMiner} (see that class's javadoc for the exact reasoning).
 */
public class DetMiner extends Block implements IBomb {

    public DetMiner(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;

        level.destroyBlock(pos, false);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        ExplosionVNT explosion = new ExplosionVNT(level, x, y, z, 4F, detonator);
        explosion.setBlockAllocator(new BlockAllocatorStandard());
        explosion.setBlockProcessor(new BlockProcessorStandard().setAllDrop());
        explosion.explode();

        ExplosionLarge.spawnParticles(level, x, y, z, 30);

        return BombReturnCode.DETONATED;
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, @Nullable Explosion explosion) {
        explode(level, pos, explosion != null ? explosion.getIndirectSourceEntity() : null);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }
}
