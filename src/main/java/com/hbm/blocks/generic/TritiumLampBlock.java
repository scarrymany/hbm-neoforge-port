package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * CE {@code TritiumLamp} — RS-toggled on/off pair. Spotlight beam skipped (no ISpotlight port).
 */
public class TritiumLampBlock extends BlockBase {

    private final boolean lit;
    private final Supplier<DeferredBlock<TritiumLampBlock>> sibling;

    public TritiumLampBlock(Properties properties, boolean lit, Supplier<DeferredBlock<TritiumLampBlock>> sibling) {
        super(properties);
        this.lit = lit;
        this.sibling = sibling;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) scheduleToggle(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide) scheduleToggle(level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean powered = level.hasNeighborSignal(pos);
        if (lit != powered) {
            level.setBlock(pos, sibling.get().get().defaultBlockState(), 2);
        }
    }

    private void scheduleToggle(Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        if (lit && !powered) level.scheduleTick(pos, this, 4);
        else if (!lit && powered) level.setBlock(pos, sibling.get().get().defaultBlockState(), 2);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        DeferredBlock<TritiumLampBlock> other = sibling.get();
        return new ItemStack(lit ? other.get() : this);
    }
}
