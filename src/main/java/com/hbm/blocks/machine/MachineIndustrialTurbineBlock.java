package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.MachineIndustrialTurbineBlockEntity;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.blockentity.machine.TurbineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
 * Ported from CE's {@code MachineIndustrialTurbine} (regname {@code machine_industrial_turbine}).
 * No GUI, no inventory in CE either - confirmed by source (implements neither {@code IGUIProvider}
 * nor holds an {@code ItemStackHandler}), see {@link MachineIndustrialTurbineBlockEntity}'s javadoc.
 * Compressor lever Exact CE {@code MachineIndustrialTurbine.java:53-78}
 * ({@code chungus_lever} 1.5F/1.0F BLOCKS when {@code !operational}).
 */
public class MachineIndustrialTurbineBlock extends BlockDummyable {

    public MachineIndustrialTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 3, 3, 1, 1};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachineIndustrialTurbineBlockEntity(PowerGenBlockEntities.INDUSTRIAL_TURBINE.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PowerGenBlockEntities.INDUSTRIAL_TURBINE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockPos core = findCore(level, pos);
        if (core == null) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof TurbineBaseBlockEntity entity)) {
            return InteractionResult.PASS;
        }

        Direction dir = Direction.from3DDataValue(level.getBlockState(core).getValue(META) - offset);
        if (pos.getX() == core.getX() + dir.getStepX() * 3
                && pos.getZ() == core.getZ() + dir.getStepZ() * 3
                && pos.getY() == core.getY() + 1) {
            if (!level.isClientSide) {
                if (!entity.operational) {
                    // Exact CE MachineIndustrialTurbine.java:66-68
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            HBMSoundHandler.chungus_lever.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    entity.onLeverPull();
                } else {
                    player.displayClientMessage(Component.literal("Cannot change compressor setting while operational!")
                            .withStyle(ChatFormatting.RED), false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
