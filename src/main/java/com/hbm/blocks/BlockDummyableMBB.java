package com.hbm.blocks;

import com.hbm.handler.MultiblockBBHandler;
import com.hbm.handler.MultiblockBBHandler.MultiblockBounds;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code BlockDummyableMBB}: the {@link BlockDummyable} variant for multiblocks
 * whose footprint is a set of arbitrary, possibly non-cubic {@link AABB}s ({@code MultiblockBBHandler})
 * rather than a fixed rectangular {@code int[6]} dimensions array. The box-rasterization math
 * (splitting each rotated footprint box across the block-position grid it overlaps) is pure
 * arithmetic and carries over mechanically once {@link AABB}/{@link Direction} replace 1.12's
 * {@code AxisAlignedBB}/{@code ForgeDirection}.
 */
public abstract class BlockDummyableMBB extends BlockDummyable {

    private static final AABB FULL_BLOCK_AABB = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    protected BlockDummyableMBB(Properties properties) {
        super(properties);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        Map<BlockPos, List<AABB>> footprint = rasterizeFootprint(dir);

        int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
        for (BlockPos relative : footprint.keySet()) {
            BlockPos absolute = placedPos.offset(relative.getX(), relative.getY(), relative.getZ());
            if (absolute.equals(placedPos)) continue;

            if (!level.getBlockState(absolute).canBeReplaced()) return false;

            minX = Math.min(minX, relative.getX());
            minY = Math.min(minY, relative.getY());
            minZ = Math.min(minZ, relative.getZ());
            maxX = Math.max(maxX, relative.getX());
            maxY = Math.max(maxY, relative.getY());
            maxZ = Math.max(maxZ, relative.getZ());
        }

        AABB span = new AABB(
                minX + placedPos.getX(), minY + placedPos.getY(), minZ + placedPos.getZ(),
                maxX + placedPos.getX() + 1, maxY + placedPos.getY() + 1, maxZ + placedPos.getZ() + 1);
        return Library.checkForPlayerEyePositions(level, span);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos corePos = placedPos.relative(dir, placementOffset);

        safeRem = true;
        Map<BlockPos, List<AABB>> footprint = rasterizeFootprint(dir);

        for (BlockPos relative : footprint.keySet()) {
            BlockPos absolute = corePos.offset(relative.getX(), relative.getY(), relative.getZ());
            if (absolute.equals(corePos)) continue;

            Direction facing;
            if (absolute.getY() < corePos.getY()) facing = Direction.DOWN;
            else if (absolute.getY() > corePos.getY()) facing = Direction.UP;
            else if (absolute.getX() < corePos.getX()) facing = Direction.WEST;
            else if (absolute.getX() > corePos.getX()) facing = Direction.EAST;
            else if (absolute.getZ() < corePos.getZ()) facing = Direction.NORTH;
            else if (absolute.getZ() > corePos.getZ()) facing = Direction.SOUTH;
            else continue;

            level.setBlock(absolute, this.defaultBlockState().setValue(META, facing.get3DDataValue()), 3);
        }
        safeRem = false;
    }

    /**
     * Rotates every registered footprint box for {@code dir} and rasterizes it into the set of
     * block positions (relative to the core) it overlaps, each carrying the clamped sub-boxes that
     * fall inside it. Shared between {@link #checkRequirement} and {@link #fillSpace}, which in CE
     * duplicated this loop verbatim.
     */
    private Map<BlockPos, List<AABB>> rasterizeFootprint(Direction dir) {
        MultiblockBounds bounds = MultiblockBBHandler.REGISTRY.get(this);
        Map<BlockPos, List<AABB>> blocks = new HashMap<>();

        for (AABB unrotatedBox : bounds.boxes) {
            AABB box = rotate(unrotatedBox, dir);

            int x1 = Mth.floor(box.minX);
            int x2 = Mth.ceil(box.maxX);
            int y1 = Mth.floor(box.minY);
            int y2 = Mth.ceil(box.maxY);
            int z1 = Mth.floor(box.minZ);
            int z2 = Mth.ceil(box.maxZ);

            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    for (int z = z1; z <= z2; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        List<AABB> blockBBs = blocks.computeIfAbsent(pos, k -> new ArrayList<>());

                        AABB blockBB = clampToPos(box, pos).move(-pos.getX(), -pos.getY(), -pos.getZ());
                        if (volume(blockBB) == 0) {
                            if (blockBBs.isEmpty()) blocks.remove(pos);
                        } else if (FULL_BLOCK_AABB.equals(blockBB)) {
                            blockBBs.add(FULL_BLOCK_AABB);
                        } else {
                            blockBBs.add(blockBB);
                        }
                    }
                }
            }
        }

        return blocks;
    }

    public AABB clampToPos(AABB box, BlockPos pos) {
        return new AABB(
                Mth.clamp(box.minX, pos.getX(), pos.getX() + 1),
                Mth.clamp(box.minY, pos.getY(), pos.getY() + 1),
                Mth.clamp(box.minZ, pos.getZ(), pos.getZ() + 1),
                Mth.clamp(box.maxX, pos.getX(), pos.getX() + 1),
                Mth.clamp(box.maxY, pos.getY(), pos.getY() + 1),
                Mth.clamp(box.maxZ, pos.getZ(), pos.getZ() + 1));
    }

    public double volume(AABB box) {
        return (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
    }

    public static AABB rotate(AABB box, Direction dir) {
        box = box.move(-0.5, 0, -0.5);

        AABB rotated = switch (dir) {
            case SOUTH -> new AABB(box.minZ, box.minY, -box.minX, box.maxZ, box.maxY, -box.maxX);
            case NORTH -> new AABB(-box.minZ, box.minY, box.minX, -box.maxZ, box.maxY, box.maxX);
            case EAST -> new AABB(-box.minX, box.minY, -box.minZ, -box.maxX, box.maxY, -box.maxZ);
            default -> box;
        };

        return rotated.move(0.5, 0, 0.5);
    }
}
