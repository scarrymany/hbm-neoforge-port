package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.HeaterFireboxBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
import java.util.Locale;

/** CE {@code HeaterFirebox} — Dummyable {0,0,1,1,1,1} offset 1. printHook Exact CE {@code :61-78}. */
public class HeaterFireboxBlock extends BlockDummyable implements ILookOverlay {

    public HeaterFireboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new HeaterFireboxBlockEntity(DummyableProcessBlockEntities.HEATER_FIREBOX.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.HEATER_FIREBOX.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE HeaterFirebox.java:61-78 — heatEnergy TU + red burnHeat TU/t, %,d
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof HeaterFireboxBlockEntity heater)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(String.format(Locale.US, "%,d", heater.heatEnergy) + " TU"));
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(Component.literal(String.format(Locale.US, "%,d", heater.burnHeat) + " TU/t")
                        .withStyle(ChatFormatting.RESET)));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
