package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineAutosawBlockEntity;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

/**
 * CE {@code MachineAutosaw} — 1×1 BlockContainer, not Dummyable.
 * Held fluid-ID Exact CE {@code MachineAutosaw.java:72-91}.
 * Screwdriver suspend Exact CE {@code :101-123}.
 * Overlay Exact CE {@code :125-151}. Tooltip Exact CE {@code :153-161}.
 */
public class MachineAutosawBlock extends BaseEntityBlock implements IToolable, ILookOverlay, ITooltipProvider {

    public static final MapCodec<MachineAutosawBlock> CODEC = simpleCodec(MachineAutosawBlock::new);

    public MachineAutosawBlock(Properties properties) {
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
        return new MachineAutosawBlockEntity(DummyableProcessBlockEntities.MACHINE_AUTOSAW.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_AUTOSAW.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Exact CE MachineAutosaw.java:72-91 — !sneak + IItemFluidIdentifier + acceptedFuels
        if (!player.isShiftKeyDown() && !stack.isEmpty() && stack.getItem() instanceof IItemFluidIdentifier ident) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineAutosawBlockEntity saw) {
                var type = ident.getType(level, pos, stack);
                if (MachineAutosawBlockEntity.acceptedFuel(type)) {
                    saw.tank.setTankType(type);
                    saw.setChanged();
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
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineAutosawBlockEntity saw) {
            player.openMenu(saw, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (!(world.getBlockEntity(new BlockPos(x, y, z)) instanceof MachineAutosawBlockEntity saw)) return false;
        // Exact CE MachineAutosaw.java:119-122
        saw.toggleSuspended();
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addStandardInfo(tooltip);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineAutosaw.java:125-151
        if (!(world.getBlockEntity(pos) instanceof MachineAutosawBlockEntity saw)) return;

        List<Component> text = new ArrayList<>();
        text.add(saw.tank.getTankType().getLocalizedName().copy()
                .append(Component.literal(": " + saw.tank.getFill() + "/" + saw.tank.getMaxFill() + "mB")));
        if (saw.isSuspended) {
            text.add(Component.literal("! ")
                    .append(Component.translatable("tile.machine_autosaw.suspended"))
                    .append(Component.literal(" !"))
                    .withStyle(ChatFormatting.RED));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
