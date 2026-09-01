package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineAssemblyFactoryBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code MachineAssemblyFactory} — Dummyable {2,0,2,2,2,2} offset 2. ≠ {@code machine_assembly_machine}.
 * TODO(CE: MachineAssemblyFactory.java:36): TileEntityProxyDyn().inventory().power().fluid() on META≥6.
 * TODO(CE: MachineAssemblyFactory.java:72-99): ILookOverlay cool/IO ports.
 * TODO(CE: RenderAssemblyFactory.java:1): OBJ TESR + AssemfacArm.
 */
public class MachineAssemblyFactoryBlock extends BlockDummyable {

    public MachineAssemblyFactoryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineAssemblyFactoryBlockEntity(DummyableProcessBlockEntities.MACHINE_ASSEMBLY_FACTORY.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_ASSEMBLY_FACTORY.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        FactoryDummyablePorts.fillFactoryExtras(this, level, placedPos, dir, placementOffset);
    }
}
