package com.hbm.blocks.machine.rbmk;

import com.hbm.api.rbmk.RBMKDials;
import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.items.machine.rbmk.RBMKItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Shared base for every RBMK reactor column - ported from CE's {@code RBMKBase} (255 lines, read in
 * full). Per this task's instructions and the research report's own headline finding: RBMK columns
 * are <b>not</b> one big multiblock, each column is its own independent 1x1xN
 * {@link BlockDummyable} (N = {@link RBMKDials#getColumnHeight} + 1, a forward-referenced
 * config/gamerule dial) - RBMKBase "contributes nothing new to how multiblocks are validated" beyond
 * making that height dynamic, which is exactly what {@link #checkRequirement}/{@link #fillSpace}
 * below override {@link BlockDummyable}'s fixed-array contract to do (mirroring CE's own
 * {@code getDimensions(World)} overload existing alongside the required fixed
 * {@code getDimensions()}).
 * <p>
 * <b>Lid state reuses the core-rotation meta range</b>, exactly like CE: every RBMK column is placed
 * with a fixed "no lid" rotation ({@link #getDirModified}), and the meta 12-15 range that would
 * otherwise encode facing instead encodes {@link #DIR_NO_LID}/{@link #DIR_NORMAL_LID}/
 * {@link #DIR_GLASS_LID} - toggled by a screwdriver interaction (not ported in this pass - see
 * {@link com.hbm.items.machine.rbmk.RBMKItems#RBMK_LID}/{@code RBMK_LID_GLASS} for the two lid items
 * this repurposed meta range refers to).
 */
public abstract class RBMKBaseBlock extends BlockDummyable {

    public static boolean dropLids = true;
    public static boolean digamma = false;

    public static final Direction DIR_NO_LID = Direction.NORTH;
    public static final Direction DIR_NORMAL_LID = Direction.EAST;
    public static final Direction DIR_GLASS_LID = Direction.SOUTH;

    protected RBMKBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 0, 0, 0};
    }

    /**
     * CE: {@code RBMKBase.getDimensions(World)} - the real, dynamic column height. Called from
     * {@link #checkRequirement} while placing, which (per {@link BlockDummyable#setPlacedBy}) runs on
     * both sides - {@link RBMKDials}'s own contract ("never dereferenced, passing null is always
     * safe") means falling back to {@code null} for a client {@link Level} is exactly as correct as a
     * real {@link ServerLevel} would be.
     */
    public int[] getDimensions(Level level) {
        ServerLevel serverLevel = level instanceof ServerLevel sl ? sl : null;
        return new int[]{RBMKDials.getColumnHeight(serverLevel), 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        return MultiblockHandlerXR.checkSpace(level, placedPos.relative(dir, placementOffset), getDimensions(level), placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        MultiblockHandlerXR.fillSpace(level, placedPos.relative(dir, placementOffset), getDimensions(level), this, dir);
    }

    /** CE: {@code RBMKBase.getDirModified} - every column is always placed with the "no lid" rotation. */
    @Override
    protected Direction getDirModified(Direction dir) {
        return DIR_NO_LID;
    }

    public boolean hasLid(Level level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        if (core == null) return true;
        return level.getBlockEntity(core) instanceof RBMKBaseBlockEntity rbmk && rbmk.hasLid();
    }

    /**
     * Shared right-click-to-open behavior for every GUI-bearing column - CE: {@code RBMKBase.openInv}.
     * Sneaking passes through; otherwise opens the core's {@link MenuProvider}, matching
     * {@link BlockDummyable#standardOpenBehavior} but resolving the core's own block entity instead
     * of assuming it implements {@code MenuProvider} directly (several columns, e.g. moderator/
     * absorber/reflector/blank/cooler, have none).
     */
    public boolean openInv(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return true;

        BlockPos core = findCore(level, pos);
        if (core == null) return false;

        BlockEntity te = level.getBlockEntity(core);
        if (!(te instanceof RBMKBaseBlockEntity)) return false;

        if (!player.isShiftKeyDown() && te instanceof MenuProvider menu) {
            player.openMenu(new SimpleMenuProvider(menu, menu.getDisplayName()), core);
        }

        return true;
    }

    /**
     * Every GUI-bearing column (rod/control/boiler/outgasser/heater/storage) opens through this one
     * override - columns with no {@link MenuProvider} block entity (moderator/absorber/reflector/
     * blank/cooler) simply do nothing, matching CE's {@code RBMKBase.openInv} for those same columns.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openInv(level, pos, player) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel && dropLids) {
            int meta = state.getValue(META);
            int height = RBMKDials.getColumnHeight(serverLevel);
            BlockPos spawnPos = pos.above(height);

            if (meta == DIR_NORMAL_LID.get3DDataValue() + offset) {
                net.minecraft.world.level.block.Block.popResource(level, spawnPos, new ItemStack(RBMKItems.RBMK_LID.get()));
            }
            if (meta == DIR_GLASS_LID.get3DDataValue() + offset) {
                net.minecraft.world.level.block.Block.popResource(level, spawnPos, new ItemStack(RBMKItems.RBMK_LID_GLASS.get()));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    public static Integer metaToLid(int meta) {
        if (meta - offset == DIR_NORMAL_LID.get3DDataValue()) return 1;
        if (meta - offset == DIR_GLASS_LID.get3DDataValue()) return 2;
        if (meta - offset == DIR_NO_LID.get3DDataValue()) return 0;
        return null;
    }
}
