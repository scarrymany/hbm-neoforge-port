package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.HexTankBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** CE {@code MachineUF6Tank}/{@code MachinePuF6Tank} — IMultiBlock {0,0,1,0,0,0} as Dummyable. */
public class MachineHexTankBlock extends BlockDummyable {

    private final boolean puf6;

    public MachineHexTankBlock(Properties properties, boolean puf6) {
        super(properties);
        this.puf6 = puf6;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    private Supplier<BlockEntityType<HexTankBlockEntity>> type() {
        return puf6 ? DummyableProcessBlockEntities.MACHINE_PUF6_TANK : DummyableProcessBlockEntities.MACHINE_UF6_TANK;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(META) < 12) return null;
        return puf6
                ? HexTankBlockEntity.puf6(type().get(), pos, state)
                : HexTankBlockEntity.uf6(type().get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == this.type().get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof HexTankBlockEntity be) {
            return be.tank.getRedstoneComparatorPower();
        }
        return 0;
    }
}
