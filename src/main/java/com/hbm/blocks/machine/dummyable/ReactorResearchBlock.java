package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.ReactorResearchBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code ReactorResearch} — Dummyable {2,0,0,0,0,0} offset 0.
 * TODO(CE: ReactorResearch.java:36): TileEntityProxyCombo(false,true,true) on extras.
 * TODO(CE: ReactorResearch.java:46): BossSpawnHandler.markFBI.
 * TODO(CE: RenderSmallReactor.java:16): TESR rods.
 */
public class ReactorResearchBlock extends BlockDummyable {

    public ReactorResearchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new ReactorResearchBlockEntity(DummyableProcessBlockEntities.REACTOR_RESEARCH.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.REACTOR_RESEARCH.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            FluidState neighbor = level.getFluidState(pos.relative(dir));
            if (!neighbor.is(net.minecraft.tags.FluidTags.WATER)) continue;
            double ix = pos.getX() + 0.5 + dir.getStepX() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepX();
            double iy = pos.getY() + 0.5 + rand.nextDouble() - 0.5;
            double iz = pos.getZ() + 0.5 + dir.getStepZ() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepZ();
            level.addParticle(ParticleTypes.BUBBLE, ix, iy, iz, 0.0, 0.2, 0.0);
        }
    }
}
