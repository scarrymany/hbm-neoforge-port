package com.hbm.blocks;

import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.interfaces.ICopiable;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.world.gen.nbt.INBTBlockTransformable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Ported from CE's {@code BlockDummyable}: the shared base for every multiblock's non-core "dummy"
 * blocks. Every Phase 2 multiblock machine builds on this contract, so it is ported as faithfully
 * as the 1.12 -> 1.21 API gap allows rather than redesigned.
 * <p>
 * CE packed rotation/orphan/core information into a single 0-15 metadata value on one
 * {@code PropertyInteger}. Block metadata itself is gone, but a single 0-15 {@link IntegerProperty}
 * is still a completely ordinary blockstate property, so that exact encoding is preserved bit for
 * bit: 0-5 dummy-to-core direction, 6-11 the same with the "extra" flag set, 12-15 the core's own
 * facing (rendering only). Preserving the numeric encoding (rather than switching to a
 * {@code DirectionProperty} + a separate enum property) keeps every offset/bitmask a concrete
 * multiblock subclass computes against CE source directly portable.
 * <p>
 * {@link Direction#from3DDataValue(int)} / {@link Direction#get3DDataValue()} enumerate DOWN, UP,
 * NORTH, SOUTH, WEST, EAST in that exact order - identical to 1.12 Forge's {@code ForgeDirection} for
 * indices 0-5 - so every meta<->direction conversion below carries over without a translation table.
 * <p>
 * Not ported: CE's {@code ModelBakeEvent}/baked-model client rendering
 * ({@code bakeModel}/{@code registerModel}/{@code registerSprite}/{@code getStateMapper}) and the
 * GL11 immediate-mode placement preview ({@code drawPlacementHighlight}) have no 1.21 equivalent.
 * {@link #getAllDimensions()} and {@link #getAABBExtras()} are kept as the data contract a future
 * rendering area needs to rebuild that preview on the modern pipeline.
 */
public abstract class BlockDummyable extends BaseEntityBlock implements ICustomBlockHighlight, ICopiable, INBTBlockTransformable {

    /// BLOCK METADATA ///
    // 0-5   dummy rotation  (for dummy neighbor / core-search checks)
    // 6-11  extra           (6 rotations with flag, for pipe connectors and the like)
    // 12-15 block rotation  (core only, for rendering the block entity)
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    // meta offset from dummy range to core rotation range
    public static final int offset = 10;
    // meta offset from dummy range to extra range
    public static final int extra = 6;

    public static boolean safeRem = false;

    public final List<AABB> bounding = new ArrayList<>();

    protected BlockDummyable(Properties properties) {
        super(properties.isSuffocating(BlockDummyable::never).isViewBlocking(BlockDummyable::never));
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));
    }

    private static boolean never(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    protected int getMaxCoreSearchSteps() {
        return 512;
    }

    protected boolean isSameMultiblock(Block other) {
        return other == this;
    }

    /**
     * Walks from {@code pos} towards the core along each dummy's encoded direction, exactly as
     * CE's {@code findCoreSerialized} did, bounded by {@link #getMaxCoreSearchSteps()} against a
     * broken/circular chain.
     */
    @Nullable
    public BlockPos findCore(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());

        for (int steps = 0, max = getMaxCoreSearchSteps(); steps < max; steps++) {
            BlockState state = level.getBlockState(scratch);
            if (!isSameMultiblock(state.getBlock())) return null;

            int meta = state.getValue(META);
            if (meta >= 12) return scratch.immutable();

            if (meta >= extra) meta -= extra;
            scratch.move(Direction.from3DDataValue(meta).getOpposite());
        }

        return null;
    }

    @Nullable
    public BlockEntity findCoreBlockEntity(Level level, BlockPos pos) {
        BlockPos corePos = findCore(level, pos);
        return corePos == null ? null : level.getBlockEntity(corePos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide || safeRem) return;
        cascadeOrphans(level, pos, state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        cascadeOrphans(level, pos, state);
    }

    private boolean isOrphan(BlockGetter level, BlockPos pos, BlockState state) {
        int meta = state.getValue(META);
        if (meta >= 12) return false; // core, never an orphan by this check

        if (meta >= extra) meta -= extra;
        BlockPos towardsCore = pos.relative(Direction.from3DDataValue(meta).getOpposite());
        return !isSameMultiblock(level.getBlockState(towardsCore).getBlock());
    }

    /**
     * Iterative (non-recursive) orphan cascade. {@link #safeRem} suppresses re-entry so
     * {@code removeBlock}'s neighbor notifications don't recurse back into
     * {@link #neighborChanged}; freshly orphaned neighbors are queued manually instead. Kept
     * iterative deliberately: a recursive walk over a long dummy chain would blow the stack.
     */
    private void cascadeOrphans(Level level, BlockPos start, BlockState startState) {
        if (startState.getBlock() != this || !isOrphan(level, start, startState)) return;

        safeRem = true;
        try {
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);

            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() != this || !isOrphan(level, pos, state)) continue;

                level.removeBlock(pos, false);
                for (Direction dir : Direction.values()) {
                    queue.add(pos.relative(dir));
                }
            }
        } finally {
            safeRem = false;
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(placer instanceof Player player)) return;

        safeRem = true;
        level.removeBlock(pos, false);
        safeRem = false;

        int placementOffset = -getOffset();
        BlockPos adjustedPos = pos.above(getHeightOffset());

        int rotationIndex = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        Direction dir = switch (rotationIndex) {
            case 0 -> Direction.from3DDataValue(2); // NORTH
            case 1 -> Direction.from3DDataValue(5); // EAST
            case 2 -> Direction.from3DDataValue(3); // SOUTH
            default -> Direction.from3DDataValue(4); // WEST
        };
        dir = getDirModified(dir);

        if (!checkRequirement(level, adjustedPos, dir, placementOffset)) {
            if (!player.hasInfiniteMaterials()) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();
                Item item = this.asItem();

                if (mainHand.is(item) && mainHand.getCount() < mainHand.getMaxStackSize()) {
                    mainHand.grow(1);
                } else if (offHand.is(item) && offHand.getCount() < offHand.getMaxStackSize()) {
                    offHand.grow(1);
                } else if (mainHand.isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(this));
                } else if (offHand.isEmpty()) {
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(this));
                } else {
                    player.getInventory().add(new ItemStack(this));
                }
            }
            return;
        }

        if (!level.isClientSide) {
            BlockPos corePos = adjustedPos.relative(dir, placementOffset);
            int meta = getMetaForCore(level, corePos, player, dir.get3DDataValue() + offset);
            level.setBlock(corePos, this.defaultBlockState().setValue(META, meta), 3);
            IPersistentNBT.restoreData(level, corePos, stack);
            fillSpace(level, adjustedPos, dir, placementOffset);
        }

        level.scheduleTick(pos, this, 1);
        level.scheduleTick(pos, this, 2);

        super.setPlacedBy(level, pos, state, placer, stack);
    }

    /**
     * A bit more advanced than {@link #getDirModified}, but it is important that the resulting
     * direction meta stays in the core range. Using the "extra" metas here is technically possible
     * but requires care: it must avoid a recursive loop in the core finder and make sure the block
     * entity uses the right meta.
     */
    protected int getMetaForCore(Level level, BlockPos pos, Player player, int original) {
        return original;
    }

    /**
     * Allows subclasses to fix/limit the effective placement direction (e.g. a multiblock that can
     * only ever face two ways instead of four).
     */
    protected Direction getDirModified(Direction dir) {
        return dir;
    }

    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        return MultiblockHandlerXR.checkSpace(level, placedPos.relative(dir, placementOffset), getDimensions(), placedPos, dir);
    }

    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        MultiblockHandlerXR.fillSpace(level, placedPos.relative(dir, placementOffset), getDimensions(), this, dir);
    }

    /**
     * Shared right-click-to-open behavior for concrete multiblocks: sneaking passes the click
     * through (matching CE), otherwise the core's block entity is opened as a menu if it exposes
     * one. Left for subclasses to call from their own {@code useWithoutItem} override, matching
     * CE's contract of leaving the actual open-gui decision to the concrete block.
     */
    protected InteractionResult standardOpenBehavior(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!player.isShiftKeyDown()) {
            BlockPos corePos = findCore(level, pos);
            if (corePos == null) return InteractionResult.FAIL;

            if (level.getBlockEntity(corePos) instanceof MenuProvider menu) {
                player.openMenu(new SimpleMenuProvider(menu, menu.getDisplayName()), corePos);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.SUCCESS;
    }

    /** "Upgrades" a regular dummy block to one with the extra flag set. */
    public void makeExtra(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != this) return;

        int meta = state.getValue(META);
        if (meta > 5) return;

        safeRem = true;
        level.setBlock(pos, state.setValue(META, meta + extra), 3);
        safeRem = false;
    }

    public void removeExtra(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != this) return;

        int meta = state.getValue(META);
        if (meta <= 5 || meta >= 12) return;

        safeRem = true;
        level.setBlock(pos, state.setValue(META, meta - extra), 3);
        safeRem = false;
    }

    public boolean hasExtra(int meta) {
        return meta > 5 && meta < 12;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            Containers.dropContentsOnDestroy(state, newState, level, pos);

            int meta = state.getValue(META);
            if (meta < 12 && !safeRem) {
                if (meta >= extra) meta -= extra;
                BlockPos corePos = findCore(level, pos.relative(Direction.from3DDataValue(meta).getOpposite()));
                if (corePos != null) {
                    level.removeBlock(corePos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public boolean useDetailedHitbox() {
        return !bounding.isEmpty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!useDetailedHitbox()) return super.getShape(state, level, pos, context);

        BlockPos corePos = findCore(level, pos);
        if (corePos == null) return super.getShape(state, level, pos, context);

        Direction rotation = getRotationFromState(level.getBlockState(corePos));
        Vec3 coreOffset = Vec3.atLowerCornerOf(corePos.subtract(pos));

        VoxelShape combined = Shapes.empty();
        for (AABB aabb : bounding) {
            AABB rotated = getAABBRotationOffset(aabb, coreOffset.x + 0.5, coreOffset.y, coreOffset.z + 0.5, rotation);
            combined = Shapes.or(combined, Shapes.create(rotated));
        }
        return combined;
    }

    private Direction getRotationFromState(BlockState coreState) {
        int meta = coreState.getValue(META);
        return Direction.from3DDataValue(meta - offset).getClockWise(Direction.Axis.Y);
    }

    /**
     * The UP/DOWN branch below is unreachable in practice: core rotation metas (12-15) only ever
     * decode to the four horizontal directions (see {@link #getRotationFromState}), so this is
     * only ever called with a horizontal {@code dir}. Kept as a safe identity fallback rather than
     * throwing.
     */
    public static AABB getAABBRotationOffset(AABB aabb, double x, double y, double z, Direction dir) {
        AABB rotated = switch (dir) {
            case NORTH -> new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
            case EAST -> new AABB(-aabb.maxZ, aabb.minY, aabb.minX, -aabb.minZ, aabb.maxY, aabb.maxX);
            case SOUTH -> new AABB(-aabb.maxX, aabb.minY, -aabb.maxZ, -aabb.minX, aabb.maxY, -aabb.minZ);
            case WEST -> new AABB(aabb.minZ, aabb.minY, -aabb.maxX, aabb.maxZ, aabb.maxY, -aabb.minX);
            default -> aabb;
        };
        return rotated.move(x, y, z);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldDrawHighlight(Level level, BlockPos pos) {
        return !bounding.isEmpty();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawHighlight(RenderHighlightEvent.Block event, Level level, BlockPos pos) {
        BlockPos corePos = findCore(level, pos);
        if (corePos == null) return;

        Vec3 camera = event.getCamera().getPosition();
        float expand = 0.002F;

        Direction rotation = getRotationFromState(level.getBlockState(corePos));

        PoseStack poseStack = event.getPoseStack();
        VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(RenderType.lines());

        for (AABB aabb : bounding) {
            AABB transformed = getAABBRotationOffset(aabb.inflate(expand), 0, 0, 0, rotation)
                    .move(corePos.getX() - camera.x + 0.5, corePos.getY() - camera.y, corePos.getZ() - camera.z + 0.5);
            LevelRenderer.renderLineBox(poseStack, vertexConsumer, transformed, 0.0F, 0.0F, 0.0F, 0.4F);
        }
    }

    @Override
    @Nullable
    public CompoundTag getSettings(Level level, BlockPos pos) {
        BlockPos corePos = findCore(level, pos);
        if (corePos == null) return null;

        return level.getBlockEntity(corePos) instanceof ICopiable copiable ? copiable.getSettings(level, corePos) : null;
    }

    @Override
    public void pasteSettings(CompoundTag tag, int index, Level level, Player player, BlockPos pos) {
        BlockPos corePos = findCore(level, pos);
        if (corePos == null) return;

        if (level.getBlockEntity(corePos) instanceof ICopiable copiable) {
            copiable.pasteSettings(tag, index, level, player, corePos);
        }
    }

    @Override
    @Nullable
    public String[] infoForDisplay(Level level, BlockPos pos) {
        BlockPos corePos = findCore(level, pos);
        if (corePos == null) return null;

        return level.getBlockEntity(corePos) instanceof ICopiable copiable ? copiable.infoForDisplay(level, corePos) : null;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation == Rotation.NONE) return state;

        int meta = state.getValue(META);
        boolean isCoreRotation = meta >= 12;
        boolean isExtra = !isCoreRotation && meta >= extra;

        int dirIndex = meta;
        if (isCoreRotation) dirIndex -= offset;
        else if (isExtra) dirIndex -= extra;

        Direction dir = Direction.from3DDataValue(dirIndex);
        Direction rotatedDir = dir.getAxis() == Direction.Axis.Y ? dir : rotation.rotate(dir);
        int rotatedIndex = rotatedDir.get3DDataValue();

        if (isCoreRotation) rotatedIndex += offset;
        else if (isExtra) rotatedIndex += extra;

        return state.setValue(META, rotatedIndex);
    }

    /**
     * @return an int array with six fields, the amount of dummy blocks in each direction around
     * the core: UP, DOWN, FORWARD, BACKWARD, LEFT, RIGHT.
     */
    public abstract int[] getDimensions();

    public abstract int getOffset();

    public int getHeightOffset() {
        return 0;
    }

    public int[][] getAllDimensions() {
        return new int[][]{getDimensions()};
    }

    public double[][] getAABBExtras() {
        return new double[0][0];
    }
}
