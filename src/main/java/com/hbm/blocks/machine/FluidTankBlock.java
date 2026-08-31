package com.hbm.blocks.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.FluidTankBlockEntity;
import com.hbm.blockentity.machine.StorageBlockEntities;
import com.hbm.inventory.container.FluidTankMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

/**
 * Single-block TE-backed fluid storage tank - see {@link FluidTankBlockEntity}'s javadoc for why this
 * is a distinct, deliberately-scoped-down id ({@code machine_fluidtank_basic}) rather than a port of
 * CE's real 5x5-multiblock {@code machine_fluidtank}.
 */
public class FluidTankBlock extends BaseEntityBlock {

    public static final MapCodec<FluidTankBlock> CODEC = simpleCodec(FluidTankBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public FluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(StorageBlockEntities.FLUID_TANK_TYPE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        return beType == StorageBlockEntities.FLUID_TANK_TYPE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        IPersistentNBT.restoreData(level, pos, stack);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            IPersistentNBT.breakBlock(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        IPersistentNBT.onBlockHarvested(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Non-sneak click opens {@link FluidTankMenu} (same confirmed
     * {@code player.openMenu(MenuProvider, BlockPos)} shape as {@link CrateBlock#useWithoutItem});
     * sneak-right-click-with-empty-hand instead cycles the tank's receive/both/send/disabled mode -
     * see {@link FluidTankBlockEntity}'s javadoc on why this substitutes for a GUI mode-toggle button
     * (no server-bound GUI-button packet infrastructure exists yet in this port). Mirrors Neo
     * Edition's own real {@code MachineFluidTankBlock#useWithoutItem} split (non-sneak opens the GUI,
     * sneak does an alternate action) confirmed against a real compiling class.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank)) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            tank.cycleMode();
        } else {
            player.openMenu(new SimpleMenuProvider((id, inv, ply) -> new FluidTankMenu(id, inv, tank), tank.getDisplayName()), pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank)) return 0;
        return tank.getTank().getRedstoneComparatorPower();
    }
}
