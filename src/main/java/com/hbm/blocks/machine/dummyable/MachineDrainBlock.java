package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineDrainBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.items.machine.IItemFluidIdentifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

/** CE {@code MachineDrain} — Dummyable {0,0,2,0,0,0} offset 0. Held fluid-ID Exact CE {@code :51-70}. printHook Exact CE {@code :80-91}. */
public class MachineDrainBlock extends BlockDummyable implements ILookOverlay {

    public MachineDrainBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 2, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineDrainBlockEntity(DummyableProcessBlockEntities.MACHINE_DRAIN.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_DRAIN.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Exact CE MachineDrain.java:51-70 — !sneak + IItemFluidIdentifier → setTankType
        if (!player.isShiftKeyDown() && !stack.isEmpty() && stack.getItem() instanceof IItemFluidIdentifier ident) {
            if (!level.isClientSide) {
                BlockPos core = findCore(level, pos);
                if (core != null && level.getBlockEntity(core) instanceof MachineDrainBlockEntity drain) {
                    var type = ident.getType(level, core, stack);
                    drain.tank.setTankType(type);
                    drain.setChanged();
                    player.displayClientMessage(Component.literal("Changed type to ")
                            .append(type.getLocalizedName())
                            .append(Component.literal("!"))
                            .withStyle(ChatFormatting.YELLOW), false);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineDrain.java:80-91
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof MachineDrainBlockEntity drain)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(drain.tank.getTankType().getLocalizedName())
                        .append(Component.literal(": " + drain.tank.getFill() + "/" + drain.tank.getMaxFill() + "mB"))));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
