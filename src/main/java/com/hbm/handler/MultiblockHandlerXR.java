package com.hbm.handler;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.handler.MultiblockHandlerXR}. Signatures are dictated by the
 * port's already-written {@link BlockDummyable#checkRequirement} / {@link BlockDummyable#fillSpace}
 * call sites and confirmed as a real NeoForge 1.21 shape by Neo Edition's own class of the same
 * name (identical {@code checkSpace}/{@code fillSpace}/{@code rotate} parameter lists).
 * <p>
 * Deliberately diverges from Neo Edition's body in two ways, per this port's package-naming/behavior
 * decisions (see {@code docs/phase2/multiblock_framework.md}):
 * <ul>
 *   <li>{@code fillSpace} writes {@link BlockDummyable#META} (CE's single 0-15 metadata encoding,
 *   which this port's {@code BlockDummyable} preserves bit-for-bit) rather than Neo Edition's
 *   {@code FACING}/{@code TYPE} blockstate property pair.</li>
 *   <li>{@code checkSpace} keeps CE's player-eye-position safety check
 *   ({@link Library#checkForPlayerEyePositions}), which Neo Edition's version drops.</li>
 * </ul>
 * CE's {@code emptySpace} is not ported: CE itself marks it {@code @Deprecated} and logs
 * "shouldn't even be executed" the one time it would run, so there is no live caller to preserve.
 */
public class MultiblockHandlerXR {

    // when looking north
    //                                              U  D  N  S  W  E
    public static int[] uni = new int[]{3, 0, 4, 4, 4, 4};

    /**
     * @param level        the level the multiblock is being placed in
     * @param corePos      the position the core block will occupy
     * @param dim          {UP, DOWN, NORTH, SOUTH, WEST, EAST} dummy counts around the core
     * @param placedPos    the position the player's placement click actually targeted (counts as
     *                     unoccupied even though the block there hasn't been replaced yet)
     * @param dir          the direction the multiblock is being placed in
     * @return true if every position the multiblock needs is replaceable and no non-creative,
     * non-spectator player would be trapped inside it
     */
    public static boolean checkSpace(Level level, BlockPos corePos, int[] dim, BlockPos placedPos, Direction dir) {
        if (dim == null || dim.length != 6) return false;

        int count = 0;

        int[] rot = rotate(dim, dir);

        int x = corePos.getX();
        int y = corePos.getY();
        int z = corePos.getZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int a = x - rot[4]; a <= x + rot[5]; a++) {
            for (int b = y - rot[1]; b <= y + rot[0]; b++) {
                for (int c = z - rot[2]; c <= z + rot[3]; c++) {
                    pos.set(a, b, c);

                    // if the position matches the just placed block, the space counts as unoccupied
                    if (pos.equals(placedPos)) continue;

                    if (!level.getBlockState(pos).canBeReplaced()) {
                        return false;
                    }

                    count++;

                    if (count > 2000) {
                        System.out.println("checkspace: ded " + a + " " + b + " " + c + " " + x + " " + y + " " + z);
                        return false;
                    }
                }
            }
        }

        AABB aabb = new AABB(
                x - rot[4], y - rot[1], z - rot[2],
                x + rot[5] + 1, y + rot[0] + 1, z + rot[3] + 1);

        return Library.checkForPlayerEyePositions(level, aabb);
    }

    /**
     * Fills every dummy position around {@code corePos} with {@code block}'s default state, each
     * carrying {@link BlockDummyable#META} set to the direction pointing away from the core (i.e.
     * the opposite of the direction {@link BlockDummyable#findCore} needs to walk to reach it).
     * {@link BlockDummyable#safeRem} is held for the whole call so {@code setBlock}'s neighbor
     * notifications don't trigger the not-yet-fully-placed dummies' own orphan-cascade check.
     */
    public static void fillSpace(Level level, BlockPos corePos, int[] dim, Block block, Direction dir) {
        if (dim == null || dim.length != 6) return;

        int count = 0;

        int[] rot = rotate(dim, dir);

        int x = corePos.getX();
        int y = corePos.getY();
        int z = corePos.getZ();

        BlockDummyable.safeRem = true;

        for (int a = x - rot[4]; a <= x + rot[5]; a++) {
            for (int b = y - rot[1]; b <= y + rot[0]; b++) {
                for (int c = z - rot[2]; c <= z + rot[3]; c++) {

                    Direction facing;

                    if (b < y) {
                        facing = Direction.DOWN;
                    } else if (b > y) {
                        facing = Direction.UP;
                    } else if (a < x) {
                        facing = Direction.WEST;
                    } else if (a > x) {
                        facing = Direction.EAST;
                    } else if (c < z) {
                        facing = Direction.NORTH;
                    } else if (c > z) {
                        facing = Direction.SOUTH;
                    } else {
                        continue;
                    }

                    BlockPos dummyPos = new BlockPos(a, b, c);
                    level.setBlock(dummyPos, block.defaultBlockState().setValue(BlockDummyable.META, facing.get3DDataValue()), 3);

                    count++;

                    if (count > 2000) {
                        System.out.println("fillspace: ded " + a + " " + b + " " + c + " " + x + " " + y + " " + z);

                        BlockDummyable.safeRem = false;
                        return;
                    }
                }
            }
        }
        BlockDummyable.safeRem = false;
    }

    /**
     * Rotates a {UP, DOWN, NORTH, SOUTH, WEST, EAST} dimension array (as measured looking north,
     * i.e. facing {@link Direction#SOUTH}) to face {@code dir}. Pure coordinate-swap arithmetic,
     * identical in CE and Neo Edition.
     */
    @Nullable
    public static int[] rotate(@Nullable int[] dim, Direction dir) {

        if (dim == null) return null;

        if (dir == Direction.SOUTH) return dim;

        if (dir == Direction.NORTH) {
            //                 U       D       N       S       W       E
            return new int[]{dim[0], dim[1], dim[3], dim[2], dim[5], dim[4]};
        }

        if (dir == Direction.EAST) {
            //                 U       D       N       S       W       E
            return new int[]{dim[0], dim[1], dim[5], dim[4], dim[2], dim[3]};
        }

        if (dir == Direction.WEST) {
            //                 U       D       N       S       W       E
            return new int[]{dim[0], dim[1], dim[4], dim[5], dim[3], dim[2]};
        }

        return dim;
    }
}
