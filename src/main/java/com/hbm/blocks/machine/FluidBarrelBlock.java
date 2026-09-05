package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.FluidBarrelBlockEntity;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** CE {@code BlockFluidBarrel} — 1×1 fluid barrel. Sneak fluid-ID Exact CE {@code :189-210}. Conn-state visuals skipped. */
public class FluidBarrelBlock extends BaseEntityBlock {

    public static final MapCodec<FluidBarrelBlock> CODEC = simpleCodec(p -> new FluidBarrelBlock(p, 16_000, Kind.STEEL));

    public enum Kind { PLASTIC, CORRODED, IRON, STEEL, TCALLOY, ANTIMATTER }

    public final int capacity;
    public final Kind kind;

    public FluidBarrelBlock(Properties properties, int capacity, Kind kind) {
        super(properties);
        this.capacity = capacity;
        this.kind = kind;
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
        return new FluidBarrelBlockEntity(DummyableProcessBlockEntities.FLUID_BARREL.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.FLUID_BARREL.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Exact CE BlockFluidBarrel.java:189-210 — corroded no-op; sneak + ID → tankNew
        if (kind == Kind.CORRODED) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (player.isShiftKeyDown() && !stack.isEmpty() && stack.getItem() instanceof IItemFluidIdentifier ident) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity barrel) {
                var type = ident.getType(level, pos, stack);
                barrel.tank.setTankType(type);
                barrel.setChanged();
                player.displayClientMessage(Component.literal("Changed type to ")
                        .append(type.getLocalizedName())
                        .append(Component.literal("!"))
                        .withStyle(ChatFormatting.YELLOW), false);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Exact CE BlockFluidBarrel.java:189 — corroded does not open GUI
        if (kind == Kind.CORRODED) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity be) {
            player.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity be) {
            return be.tank.getRedstoneComparatorPower();
        }
        return 0;
    }
}
