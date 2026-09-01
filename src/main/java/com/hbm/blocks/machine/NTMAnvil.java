package com.hbm.blocks.machine;

import com.hbm.inventory.container.AnvilMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code NTMAnvil} ({@code ModBlocks.java}:1094-1105). Facing falling casing + tier tooltip
 * ({@code NTMAnvil.java}:40-62, 136-150, 184-186). Opens {@code ContainerAnvil}/{@code GUIAnvil}.
 */
public class NTMAnvil extends FallingBlock {

    public static final MapCodec<NTMAnvil> CODEC = simpleCodec(NTMAnvil::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final int TIER_IRON = 1;
    public static final int TIER_STEEL = 2;
    public static final int TIER_OIL = 3;
    public static final int TIER_NUCLEAR = 4;
    public static final int TIER_RBMK = 5;
    public static final int TIER_FUSION = 6;
    public static final int TIER_PARTICLE = 7;
    public static final int TIER_GERALD = 8;

    private static final VoxelShape SHAPE_X = Block.box(4.0D, 0.0D, 0.0D, 12.0D, 12.0D, 16.0D);
    private static final VoxelShape SHAPE_Z = Block.box(0.0D, 0.0D, 4.0D, 16.0D, 12.0D, 12.0D);

    public static final Map<Integer, List<NTMAnvil>> TIER_MAP = new HashMap<>();

    public final int tier;

    public NTMAnvil(Properties properties) {
        this(properties, TIER_IRON);
    }

    public NTMAnvil(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        TIER_MAP.computeIfAbsent(tier, k -> new ArrayList<>()).add(this);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int openTier = this.tier;
        player.openMenu(new SimpleMenuProvider(
                (id, inv, ply) -> new AnvilMenu(id, inv, openTier),
                Component.translatable("container.hbm.anvil", openTier)),
                buf -> buf.writeVarInt(openTier));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Tier: " + this.tier).withStyle(ChatFormatting.GOLD));
    }
}
