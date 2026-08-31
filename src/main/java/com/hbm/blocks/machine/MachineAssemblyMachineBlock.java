package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.MachineAssemblyMachineBlockEntity;
import com.hbm.blockentity.machine.ProcessingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

/** Ported from CE's {@code MachineAssemblyMachine} (regname {@code machine_assembly_machine}): single-block, not dummyable. */
public class MachineAssemblyMachineBlock extends BaseEntityBlock {

    public static final MapCodec<MachineAssemblyMachineBlock> CODEC = simpleCodec(MachineAssemblyMachineBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public MachineAssemblyMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineAssemblyMachineBlockEntity(ProcessingBlockEntities.MACHINE_ASSEMBLER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ProcessingBlockEntities.MACHINE_ASSEMBLER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineAssemblyMachineBlockEntity assembler) {
            player.openMenu(assembler, pos);
        }
        return InteractionResult.CONSUME;
    }
}
