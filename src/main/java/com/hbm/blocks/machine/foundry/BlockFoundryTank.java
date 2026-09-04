package com.hbm.blocks.machine.foundry;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blockentity.machine.foundry.FoundryTankBlockEntity;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge port of CE {@code FoundryTank} - multiblock foundry tank for molten metal storage.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/FoundryTank.java
 * <p>
 * Connected-texture multiblock: uses 10 BooleanProperty (up/down/4 sides + 4 outlet directions).
 * Shovel interaction: scoop molten metal as scrap item (CE :140-150).
 */
public class BlockFoundryTank extends Block implements EntityBlock, ICrucibleAcceptor {

    public static final MapCodec<BlockFoundryTank> CODEC = simpleCodec(BlockFoundryTank::new);

    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty OUT_NORTH = BooleanProperty.create("out_north");
    public static final BooleanProperty OUT_SOUTH = BooleanProperty.create("out_south");
    public static final BooleanProperty OUT_EAST = BooleanProperty.create("out_east");
    public static final BooleanProperty OUT_WEST = BooleanProperty.create("out_west");

    public BlockFoundryTank(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(OUT_NORTH, false).setValue(OUT_SOUTH, false)
                .setValue(OUT_EAST, false).setValue(OUT_WEST, false));
    }

    @Override
    protected @NotNull MapCodec<BlockFoundryTank> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, SOUTH, EAST, WEST, OUT_NORTH, OUT_SOUTH, OUT_EAST, OUT_WEST);
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull net.minecraft.world.level.LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos) {
        return getUpdatedState(state, level, currentPos);
    }

    private BlockState getUpdatedState(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state
                .setValue(UP, isTank(level, pos.above()))
                .setValue(DOWN, isTank(level, pos.below()))
                .setValue(NORTH, isTank(level, pos.north()))
                .setValue(SOUTH, isTank(level, pos.south()))
                .setValue(EAST, isTank(level, pos.east()))
                .setValue(WEST, isTank(level, pos.west()))
                .setValue(OUT_NORTH, isOutlet(level, pos, Direction.NORTH))
                .setValue(OUT_SOUTH, isOutlet(level, pos, Direction.SOUTH))
                .setValue(OUT_EAST, isOutlet(level, pos, Direction.EAST))
                .setValue(OUT_WEST, isOutlet(level, pos, Direction.WEST));
    }

    private static boolean isTank(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BlockFoundryTank;
    }

    /** CE FoundryTank.java:84-88 — neighbor is FoundryOutlet facing {@code dir}. */
    private static boolean isOutlet(BlockGetter level, BlockPos pos, Direction dir) {
        BlockState neighbor = level.getBlockState(pos.relative(dir));
        Block block = neighbor.getBlock();
        return block instanceof BlockFoundryOutlet outlet && outlet.getFacing(neighbor) == dir;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FoundryTankBlockEntity(pos, state);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return false;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundryTankBlockEntity tank)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!stack.isEmpty() && stack.getItem() instanceof TieredItem) {
            if (tank.amount > 0) {
                ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(tank.type, tank.amount), false);
                if (!player.addItem(scrap)) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, scrap));
                }
                tank.amount = 0;
                tank.type = null;
                tank.setChanged();
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        return te instanceof ICrucibleAcceptor && ((ICrucibleAcceptor) te).canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof ICrucibleAcceptor) return ((ICrucibleAcceptor) te).pour(world, pos, dX, dY, dZ, side, stack);
        return stack;
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return false;
    }

    @Override
    public Mats.MaterialStack flow(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return stack;
    }
}
