package com.hbm.blocks.generic;

import com.hbm.items.BilletPowderItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Smoldering ground, ported from CE's {@code BlockSmolder}: sets entities on fire and spawns
 * lava/flame particles when the space above is open, and drops fire powder instead of itself.
 * Fully self-contained.
 */
public class BlockSmolder extends Block {

    public BlockSmolder(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(BilletPowderItems.POWDER_FIRE.get()));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 1.1, pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                    0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 1.1, pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.igniteForSeconds(3);
        super.stepOn(level, pos, state, entity);
    }
}
