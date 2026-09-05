package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DeuteriumExtractorBlockEntity;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.Library;
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

/** CE {@code MachineDeuteriumExtractor} — 1×1 water → heavy water. printHook Exact CE {@code :45-63}. */
public class MachineDeuteriumExtractorBlock extends BaseEntityBlock implements ILookOverlay {

    public static final MapCodec<MachineDeuteriumExtractorBlock> CODEC = simpleCodec(MachineDeuteriumExtractorBlock::new);

    public MachineDeuteriumExtractorBlock(Properties properties) {
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
        return DeuteriumExtractorBlockEntity.cube(DummyableProcessBlockEntities.MACHINE_DEUTERIUM_EXTRACTOR.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_DEUTERIUM_EXTRACTOR.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof DeuteriumExtractorBlockEntity be) {
            player.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineDeuteriumExtractor.java:45-63 — short HE + WATER / HEAVYWATER fill/max
        if (!(world.getBlockEntity(pos) instanceof DeuteriumExtractorBlockEntity extractor)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(Library.getShortNumber(extractor.power) + "/"
                + Library.getShortNumber(extractor.getMaxPower()) + " HE"));
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(Fluids.WATER.getLocalizedName())
                        .append(Component.literal(": " + extractor.water.getFill() + "/"
                                + extractor.water.getMaxFill() + "mB"))));
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(Fluids.HEAVYWATER.getLocalizedName())
                        .append(Component.literal(": " + extractor.heavyWater.getFill() + "/"
                                + extractor.heavyWater.getMaxFill() + "mB"))));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
