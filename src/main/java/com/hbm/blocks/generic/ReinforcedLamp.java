package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Rad-resistant redstone lamp, ported from CE's {@code ReinforcedLamp}. CE's pair of registry
 * entries ({@code reinforced_lamp_off}/{@code reinforced_lamp_on}) swap into each other via a
 * direct static-field reference; the port instead threads the companion block in as a lazy
 * {@link Supplier} (set once by the registration hub after both {@code DeferredBlock}s exist),
 * avoiding a circular static-initialization dependency between the two instances.
 * <p>
 * {@code RadiationSystemNT.markSectionForRebuild} calls are dropped: that system does not exist yet
 * (Phase 2). {@link com.hbm.interfaces.IRadResistantBlock} is implemented as a no-op marker per that
 * interface's own contract, matching the block's real shielding behavior once Phase 2 lands.
 */
public class ReinforcedLamp extends Block implements com.hbm.interfaces.IRadResistantBlock {

    private final boolean isOn;
    private Supplier<? extends Block> companion;

    public ReinforcedLamp(Properties properties, boolean isOn) {
        super(properties);
        this.isOn = isOn;
    }

    /** Set once by the registration hub after both the on/off pair exist. */
    public void setCompanion(Supplier<? extends Block> companion) {
        this.companion = companion;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (this.isOn && !level.hasNeighborSignal(pos)) {
                level.scheduleTick(pos, this, 4);
            } else if (!this.isOn && level.hasNeighborSignal(pos) && companion != null) {
                level.setBlock(pos, companion.get().defaultBlockState(), 2);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (this.isOn && !level.hasNeighborSignal(pos)) {
                level.scheduleTick(pos, this, 4);
            } else if (!this.isOn && level.hasNeighborSignal(pos) && companion != null) {
                level.setBlock(pos, companion.get().defaultBlockState(), 2);
            }
        }
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (this.isOn && !level.hasNeighborSignal(pos) && companion != null) {
            level.setBlock(pos, companion.get().defaultBlockState(), 2);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                        net.minecraft.world.level.LevelReader level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        return new ItemStack(this.isOn && companion != null ? companion.get() : this);
    }
}
