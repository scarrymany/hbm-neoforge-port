package com.hbm.blocks.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.BatteryBlockEntity;
import com.hbm.blockentity.machine.StorageBlockEntities;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.container.BatteryMenu;
import com.hbm.lib.InventoryHelper;
import com.hbm.lib.Library;
import com.hbm.util.TagsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-block HE battery, ported from CE's {@code com.hbm.blocks.machine.MachineBattery} (read in
 * full). One instance per grade (potato/normal/lithium/schrabidium/dineutronium), each with its own
 * {@code maxPower} - see {@link com.hbm.blockentity.machine.BatteryBlockEntity} for the block
 * entity's full CE-vs-port scope notes.
 * printHook Exact CE {@code MachineBattery.java:221-241}. MachineFENSU is unregistered — stay skipped.
 * addInformation Exact CE {@code MachineBattery.java:191-218} (item tooltip, not GUI).
 */
public class BatteryBlock extends BaseEntityBlock implements ILookOverlay {

    public static final MapCodec<BatteryBlock> CODEC = simpleCodec(p -> new BatteryBlock(p, 0L));

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final long maxPower;

    public BatteryBlock(Properties properties, long maxPower) {
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
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBlockEntity(StorageBlockEntities.BATTERY_TYPE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        return beType == StorageBlockEntities.BATTERY_TYPE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        IPersistentNBT.restoreData(level, pos, stack);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
                InventoryHelper.dropInventoryItems(level, pos, battery.inventory);
            }
            IPersistentNBT.breakBlock(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        IPersistentNBT.onBlockHarvested(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Opens {@link BatteryMenu} on a non-sneaking click - matching CE's own
     * {@code MachineBattery.onBlockActivated} (sneaking does nothing special, just doesn't open the
     * GUI, same as here returning {@code PASS}).
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            player.openMenu(new SimpleMenuProvider((id, inv, ply) -> new BatteryMenu(id, inv, battery), battery.getDisplayName()), pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    /** Immediate redstone-state pickup, matching CE's own {@code neighborChanged} override; the 20-tick poll in {@code BatteryBlockEntity#updateEntity} is CE's own belt-and-suspenders backstop, kept too. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            battery.setIndirectlyPowered(level.hasNeighborSignal(pos));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BatteryBlockEntity battery)) return 0;
        return (int) battery.getPowerRemainingScaled(15L);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineBattery.java:221-241
        if (!(world.getBlockEntity(pos) instanceof BatteryBlockEntity battery)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(Library.getShortNumber(battery.getPower()) + "/"
                + Library.getShortNumber(getMaxPower()) + " HE"));
        if (battery.delta == 0) {
            text.add(Component.literal("-- ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("0HE/s").withStyle(ChatFormatting.RESET)));
        } else if (battery.delta > 0) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(Library.getShortNumber(battery.delta) + "HE/s")
                            .withStyle(ChatFormatting.RESET)));
        } else {
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(Library.getShortNumber(-battery.delta) + "HE/s")
                            .withStyle(ChatFormatting.RESET)));
        }
        double frac = (double) battery.getPower() / (double) getMaxPower();
        text.add(Component.literal("    " + Library.getPercentage(frac) + "%")
                .withColor(Library.getColorProgress(frac)));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE MachineBattery.java:191-218
        long charge = 0L;
        if (TagsUtil.hasCustomData(stack)) {
            CompoundTag nbt = TagsUtil.getCustomData(stack);
            if (nbt.contains(IPersistentNBT.NBT_PERSISTENT_KEY)) {
                charge = nbt.getCompound(IPersistentNBT.NBT_PERSISTENT_KEY).getLong("power");
            }
        }

        if (charge == 0L) {
            tooltip.add(Component.literal("0").withStyle(ChatFormatting.RED)
                    .append(Component.literal("/" + Library.getShortNumber(this.maxPower) + "HE ")
                            .withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal("(0.0%)").withStyle(ChatFormatting.RED)));
        } else {
            double percent = Math.round(charge * 1000L / this.maxPower) * 0.1D;
            ChatFormatting color = ChatFormatting.YELLOW;
            ChatFormatting color2 = ChatFormatting.GOLD;
            if (percent < 25) {
                color = ChatFormatting.RED;
                color2 = ChatFormatting.DARK_RED;
            } else if (percent >= 75) {
                color = ChatFormatting.GREEN;
                color2 = ChatFormatting.DARK_GREEN;
            }
            tooltip.add(Component.literal(Library.getShortNumber(charge)).withStyle(color)
                    .append(Component.literal("/" + Library.getShortNumber(this.maxPower) + "HE ")
                            .withStyle(color2))
                    .append(Component.literal("(" + percent + "%)").withStyle(color)));
        }
    }
}
