package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineTeleporterBlockEntity;
import com.hbm.blocks.ILookOverlay;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code MachineTeleporter} — 1×1, no GUI.
 * TODO(CE: ItemTeleLink.java:38-45): linker item not ported.
 */
public class MachineTeleporterBlock extends BaseEntityBlock implements ILookOverlay {

    public static final MapCodec<MachineTeleporterBlock> CODEC = simpleCodec(MachineTeleporterBlock::new);

    public MachineTeleporterBlock(Properties properties) {
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
        return new MachineTeleporterBlockEntity(DummyableProcessBlockEntities.MACHINE_TELEPORTER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_TELEPORTER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineTeleporterBlockEntity tele)) return;
        List<Component> text = new ArrayList<>();
        text.add(Component.literal(String.format("%,d", tele.power) + " / " + String.format("%,d", MachineTeleporterBlockEntity.maxPower))
                .withStyle(tele.power >= MachineTeleporterBlockEntity.consumption ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (tele.target == null) {
            text.add(Component.literal("No destination set!").withStyle(ChatFormatting.RED));
        } else {
            text.add(Component.literal("Destination: " + tele.target.getX() + " / " + tele.target.getY() + " / " + tele.target.getZ()));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
