package com.hbm.blocks.machine.foundry;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.block.IToolable;
import com.hbm.blockentity.machine.foundry.FoundryOutletBlockEntity;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemScraps;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge port of CE {@code FoundryOutlet} - foundry outlet for molten metal extraction.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/FoundryOutlet.java
 * <p>
 * Directional block that extracts molten metal from channels (CE :77-92).
 * Click: scraps set filter, empty hand toggles invertRedstone (CE :116-137).
 * Screwdriver clears filter; hand drill inverts filter (CE :140-157).
 */
public class BlockFoundryOutlet extends Block implements EntityBlock, ICrucibleAcceptor, IToolable, ILookOverlay {

    public static final MapCodec<BlockFoundryOutlet> CODEC = simpleCodec(BlockFoundryOutlet::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Shapes.empty(),
            Shapes.empty(),
            Shapes.box(0.3125, 0, 0.625, 0.6875, 0.5, 1),     // NORTH
            Shapes.box(0.3125, 0, 0, 0.6875, 0.5, 0.375),     // SOUTH
            Shapes.box(0.625, 0, 0.3125, 1, 0.5, 0.6875),     // WEST
            Shapes.box(0, 0, 0.3125, 0.375, 0.5, 0.6875)      // EAST
    };

    public BlockFoundryOutlet(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<BlockFoundryOutlet> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FoundryOutletBlockEntity(pos, state);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPES[state.getValue(FACING).get3DDataValue()];
    }

    @Override
    protected boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundryOutletBlockEntity outlet)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // CE FoundryOutlet.java:121-133 — sneak is a no-op consume
        if (player.isShiftKeyDown()) return ItemInteractionResult.SUCCESS;

        if (stack.getItem() instanceof ItemScraps scraps) {
            outlet.filter = scraps.getMaterial();
            outlet.markAndSync();
            return ItemInteractionResult.SUCCESS;
        }

        if (!stack.isEmpty() && stack.getItem() instanceof TieredItem && outlet.amount > 0) {
            ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(outlet.type, outlet.amount), false);
            if (!player.addItem(scrap)) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap));
            }
            outlet.amount = 0;
            outlet.type = null;
            outlet.markAndSync();
            return ItemInteractionResult.SUCCESS;
        }

        outlet.invertRedstone = !outlet.invertRedstone;
        outlet.markAndSync();
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundryOutletBlockEntity outlet)) return InteractionResult.PASS;

        // CE :129-130 empty-hand toggles invertRedstone
        outlet.invertRedstone = !outlet.invertRedstone;
        outlet.markAndSync();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER && tool != ToolType.HAND_DRILL) return false;
        if (world.isClientSide) return true;

        BlockEntity be = world.getBlockEntity(new BlockPos(x, y, z));
        if (!(be instanceof FoundryOutletBlockEntity outlet)) return false;

        // CE FoundryOutlet.java:142-157
        if (tool == ToolType.SCREWDRIVER) {
            outlet.filter = null;
            outlet.invertFilter = false;
            outlet.markAndSync();
            return true;
        }

        outlet.invertFilter = !outlet.invertFilter;
        outlet.markAndSync();
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundryOutletBlockEntity outlet)) return;

        List<Component> text = new ArrayList<>();
        if (outlet.filter != null) {
            text.add(Component.translatable("foundry.filter", outlet.filter.getName()).withStyle(ChatFormatting.YELLOW));
        }
        if (outlet.invertFilter) {
            text.add(Component.translatable("foundry.invertFilter").withStyle(ChatFormatting.YELLOW));
        }
        if (outlet.invertRedstone) {
            text.add(Component.translatable("foundry.inverted").withStyle(ChatFormatting.DARK_GREEN));
        }
        if (text.isEmpty()) return;

        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xFF4000, 0x401000, text);
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FoundryOutletBlockEntity outlet && outlet.amount > 0) {
                ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(outlet.type, outlet.amount), false);
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap));
                outlet.amount = 0;
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        return false;
    }

    @Override
    public Mats.MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        return stack;
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        return te instanceof ICrucibleAcceptor && ((ICrucibleAcceptor) te).canAcceptPartialFlow(world, pos, side, stack);
    }

    @Override
    public Mats.MaterialStack flow(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof ICrucibleAcceptor) return ((ICrucibleAcceptor) te).flow(world, pos, side, stack);
        return stack;
    }
}
