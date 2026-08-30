package com.hbm.blocks.bomb;

import com.hbm.api.block.IExploder;
import com.hbm.api.block.IToolable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.bomb.BombBlockEntities;
import com.hbm.blockentity.bomb.ChargeBlockEntity;
import com.hbm.entity.item.EntityTNTPrimedBase;
import com.hbm.interfaces.IBomb;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BlockChargeBase} (264 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. Directional (6-facing) wall/ceiling/
 * floor-mountable timed charge, paired with {@link ChargeBlockEntity}. Right-click (not sneaking)
 * cycles the timer through {@code {0,100,200,300,600,1200,3600,6000}} ticks; sneak-click arms it if
 * a nonzero timer is set. {@code onScrew(DEFUSER)}: first click while armed disarms; a second click
 * dismantles-and-redrops via {@link #dismantle}. {@link #safe} is CE's own "explosion re-entrancy
 * guard" static flag (set only around this class's own controlled removals) - preserved exactly, a
 * standard CE pattern also used verbatim.
 */
public abstract class BlockChargeBase extends BaseEntityBlock implements IBomb, IToolable, IExploder {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE_UP = Block.box(0, 0, 0, 16, 6, 16);
    private static final VoxelShape SHAPE_DOWN = Block.box(0, 10, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 10, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 6);
    private static final VoxelShape SHAPE_WEST = Block.box(10, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 6, 16, 16);

    /** CE: re-entrancy guard - set only around this class's own controlled block removals, to keep {@link #onRemove} from re-triggering {@link #explode}. */
    public static boolean safe = false;

    protected BlockChargeBase(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** CE: {@code getItemDropped}/{@code quantityDropped} return {@code Items.AIR}/0 - normal mining never returns the charge; {@link #dismantle} is the only real drop path. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
        };
    }

    /** CE: {@code getCollisionBoundingBox} returns {@code NULL_AABB} - non-collidable, matches exactly. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction dir = state.getValue(FACING);
        BlockPos support = pos.relative(dir.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, dir);
    }

    /**
     * CE: {@code neighborChanged} - just {@code worldIn.setBlockToAir(pos)}, with no {@link #safe}
     * guard around it (confirmed by reading the real CE source: only {@link #onScrew}'s DEFUSER
     * branch sets {@code safe} around its own {@link #dismantle} call). Vanilla's block-removal path
     * still runs {@link #onRemove} for the old block whenever a block actually changes, independent of
     * the flags passed to the removal call - so, matching CE exactly, losing support here still lets
     * {@link #onRemove}'s {@code !safe} check fire {@link #explode}: a charge detonates if the block
     * it's mounted on disappears out from under it. A previous version of this method wrapped the
     * removal in {@code safe = true/false}, which suppressed that explosion - a real behavioral
     * deviation from CE, not a deliberate improvement, now corrected.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide() && !state.canSurvive(level, pos)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargeBlockEntity(BombBlockEntities.CHARGE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BombBlockEntities.CHARGE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ChargeBlockEntity charge) {
            charge.placerID = serverPlayer.getUUID();
        }
    }

    /** CE: {@code onBlockActivated} - cycles the timer / arms the charge, regardless of what's held (the DEFUSER/SCREWDRIVER dispatch happens earlier, at the item level, via {@code IToolable}). */
    private InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ChargeBlockEntity charge)) return InteractionResult.PASS;

        if (!charge.started) {
            if (player.isSneaking()) {
                if (charge.timer > 0) {
                    charge.started = true;
                    level.playSound(null, pos, HBMSoundHandler.fstbmbStart.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            } else {
                charge.timer = switch (charge.timer) {
                    case 0 -> 100;
                    case 100 -> 200;
                    case 200 -> 300;
                    case 300 -> 600;
                    case 600 -> 1200;
                    case 1200 -> 3600;
                    case 3600 -> 6000;
                    default -> 0;
                };
                level.playSound(null, pos, HBMSoundHandler.techBoop.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            charge.setChanged();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        activate(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** CE: {@code onScrew(DEFUSER)} - first click disarms, second click dismantles-and-redrops. */
    @Override
    public boolean onScrew(Level level, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
            InteractionHand hand, ToolType tool) {
        if (tool != ToolType.DEFUSER) return false;
        BlockPos pos = new BlockPos(x, y, z);
        if (!(level.getBlockEntity(pos) instanceof ChargeBlockEntity charge)) return false;

        if (charge.started) {
            charge.started = false;
            level.playSound(null, pos, HBMSoundHandler.fstbmbStart.get(), SoundSource.BLOCKS, 10.0F, 1.0F);
            charge.setChanged();
        } else {
            safe = true;
            dismantle(level, pos);
            safe = false;
        }
        return true;
    }

    /** CE: {@code breakBlock} - explodes unless the removal is one of this class's own controlled ones. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && !safe) {
            explode(level, pos, null);
        }
    }

    /** CE: {@code onBlockExploded} - unconditionally primes (fuse 0), no toggle to respect (charges have no "ignite on break" flag). */
    @Override
    public void wasExploded(Level level, BlockPos pos, @Nullable Explosion explosion) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        EntityTNTPrimedBase tntPrimed = new EntityTNTPrimedBase(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                explosion != null ? explosion.getIndirectSourceEntity() : null, state);
        tntPrimed.fuse = 0;
        tntPrimed.detonateOnCollision = false;
        level.addFreshEntity(tntPrimed);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        explode(level, BlockPos.containing(x, y, z), null);
    }

    /** CE: {@code dismantle} - safely pops a clean copy of the item, matching CE's velocity/offset numbers exactly. */
    protected void dismantle(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
        ItemStack item = new ItemStack(this.asItem());
        if (item.isEmpty()) return;

        RandomSource rand = level.getRandom();
        double ox = rand.nextFloat() * 0.6F + 0.2F;
        double oy = rand.nextFloat() * 0.2F + 1.0F;
        double oz = rand.nextFloat() * 0.6F + 0.2F;

        ItemEntity entityItem = new ItemEntity(level, pos.getX() + ox, pos.getY() + oy, pos.getZ() + oz, item);
        double v = 0.05D;
        entityItem.setDeltaMovement(rand.nextGaussian() * v, rand.nextGaussian() * v + 0.2D, rand.nextGaussian() * v);

        if (!level.isClientSide()) level.addFreshEntity(entityItem);
    }
}
