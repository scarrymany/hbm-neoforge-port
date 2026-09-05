package com.hbm.blocks.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.CapacitorBlockEntity;
import com.hbm.blockentity.machine.StorageBlockEntities;
import com.hbm.blocks.ILookOverlay;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Directional HE capacitor, ported from CE's {@code com.hbm.blocks.machine.MachineCapacitor} (read
 * in full). Unlike {@link BatteryBlock}, uses a full 6-direction {@link BlockStateProperties#FACING}
 * (CE's {@code BlockDirectional.FACING}), matching CE's own placement (a capacitor is aimed at
 * whatever it should discharge into, including straight up/down) - see
 * {@link com.hbm.blockentity.machine.CapacitorBlockEntity} for the block entity's full CE-vs-port
 * scope notes, including one documented facing-rotation simplification.
 * printHook Exact CE {@code MachineCapacitor.java:112-128}.
 */
public class CapacitorBlock extends BaseEntityBlock implements ILookOverlay {

    public static final MapCodec<CapacitorBlock> CODEC = simpleCodec(p -> new CapacitorBlock(p, 0L));

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final long maxPower;

    public CapacitorBlock(Properties properties, long maxPower) {
        super(properties);
        this.maxPower = maxPower;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public long getMaxPower() {
        return this.maxPower;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CapacitorBlockEntity(StorageBlockEntities.CAPACITOR_TYPE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        return beType == StorageBlockEntities.CAPACITOR_TYPE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        IPersistentNBT.restoreData(level, pos, stack);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            IPersistentNBT.breakBlock(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        IPersistentNBT.onBlockHarvested(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineCapacitor.java:112-128
        if (!(world.getBlockEntity(pos) instanceof CapacitorBlockEntity battery)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(BobMathUtil.getShortNumber(battery.getPower()) + " / "
                + BobMathUtil.getShortNumber(battery.getMaxPower()) + "HE"));

        double percent = (double) battery.getPower() / (double) battery.getMaxPower();
        int charge = (int) Math.floor(percent * 10_000D);
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
        text.add(Component.literal((charge / 100D) + "%").withColor(color));
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal("+" + BobMathUtil.getShortNumber(battery.powerReceived) + "HE/t")
                        .withStyle(ChatFormatting.RESET)));
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(Component.literal("-" + BobMathUtil.getShortNumber(battery.powerSent) + "HE/t")
                        .withStyle(ChatFormatting.RESET)));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
