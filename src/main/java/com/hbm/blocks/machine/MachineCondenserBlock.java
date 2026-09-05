package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.CondenserBlockEntity;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** CE {@code MachineCondenser} — 1×1 spentsteam → water. printHook Exact CE {@code :41-56}. */
public class MachineCondenserBlock extends BaseEntityBlock implements ILookOverlay {

    public static final MapCodec<MachineCondenserBlock> CODEC = simpleCodec(MachineCondenserBlock::new);

    public MachineCondenserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CondenserBlockEntity.cube(DummyableProcessBlockEntities.MACHINE_CONDENSER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_CONDENSER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof CondenserBlockEntity condenser) {
            player.openMenu(condenser, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineCondenser.java:41-56 — green input / red output, fill/max no %,d
        if (!(world.getBlockEntity(pos) instanceof CondenserBlockEntity condenser)) return;

        List<Component> text = new ArrayList<>();
        List<FluidTankNTM> tanks = condenser.getAllTanks();
        for (int i = 0; i < tanks.size(); i++) {
            FluidTankNTM tank = tanks.get(i);
            text.add(Component.literal(i < 1 ? "-> " : "<- ")
                    .withStyle(i < 1 ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(Component.empty().withStyle(ChatFormatting.RESET)
                            .append(tank.getTankType().getLocalizedName())
                            .append(Component.literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB"))));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
