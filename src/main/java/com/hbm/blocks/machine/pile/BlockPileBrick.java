package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.blockentity.machine.pile.PileBaseBlockEntity;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity.PileOrientation;
import com.hbm.blocks.generic.BlockFlammable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * CE {@code BlockPileBrick} ({@code BlockPileBrick.java}). Flammable 30/5, hardness 5 / 10.
 * HAND_DRILL on a side face scans MIN/MAX 5/15 and converts the volume to {@code pile_block}.
 */
public class BlockPileBrick extends BlockFlammable implements IToolable {

    public static final int MIN_V_SIZE = 5;
    public static final int MIN_H_SIZE = 5;
    public static final int MAX_V_SIZE = 15;
    public static final int MAX_H_SIZE = 15;

    public BlockPileBrick(BlockBehaviour.Properties properties) {
        super(properties, 30, 5);
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.HAND_DRILL) return false;
        if (side == Direction.DOWN || side == Direction.UP) return false;
        if (world.isClientSide) return true;

        Direction dir = side.getOpposite();
        Direction dirLeft = dir.getCounterClockWise();

        int negHeight = 0;
        int posHeight = 0;
        int left = 0;
        int right = 0;
        int depth = 0;

        for (int i = 1; i <= MAX_V_SIZE - 1; i++) {
            if (world.getBlockState(new BlockPos(x, y + i, z)).getBlock() != this) break;
            posHeight = i;
        }
        for (int i = 1; i <= MAX_V_SIZE - posHeight - 1; i++) {
            if (world.getBlockState(new BlockPos(x, y - i, z)).getBlock() != this) break;
            negHeight = i;
        }
        for (int i = 1; i <= MAX_H_SIZE - 1; i++) {
            if (world.getBlockState(new BlockPos(x + dirLeft.getStepX() * i, y, z + dirLeft.getStepZ() * i)).getBlock() != this) break;
            left = i;
        }
        for (int i = 1; i <= MAX_H_SIZE - left - 1; i++) {
            if (world.getBlockState(new BlockPos(x - dirLeft.getStepX() * i, y, z - dirLeft.getStepZ() * i)).getBlock() != this) break;
            right = i;
        }
        for (int i = 1; i <= MAX_H_SIZE; i++) {
            if (world.getBlockState(new BlockPos(x + dir.getStepX() * i, y, z + dir.getStepZ() * i)).getBlock() != this) break;
            depth = i;
        }

        if (posHeight + negHeight + 1 < MIN_V_SIZE) {
            BlockPile.sendError(player, "Height too low (<" + MIN_V_SIZE + ")");
            return true;
        }
        if (left + right + 1 < MIN_H_SIZE) {
            BlockPile.sendError(player, "Width too low (<" + MIN_H_SIZE + ")");
            return true;
        }
        if (depth + 1 < MIN_H_SIZE) {
            BlockPile.sendError(player, "Depth too low (<" + MIN_H_SIZE + ")");
            return true;
        }
        if (posHeight == 0 || negHeight == 0 || left == 0 || right == 0) {
            BlockPile.sendError(player, "Core cannot be on an edge");
            return true;
        }

        for (int h = -negHeight; h <= posHeight; h++) {
            for (int v = -left; v <= right; v++) {
                for (int d = 0; d <= depth; d++) {
                    BlockPos iPos = new BlockPos(
                            x - dirLeft.getStepX() * v + dir.getStepX() * d,
                            y + h,
                            z - dirLeft.getStepZ() * v + dir.getStepZ() * d);
                    if (world.getBlockState(iPos).getBlock() != this) {
                        BlockPile.sendError(player, "Graphite block missing");
                        return true;
                    }
                }
            }
        }

        for (int h = -negHeight; h <= posHeight; h++) {
            for (int v = -left; v <= right; v++) {
                for (int d = 0; d <= depth; d++) {
                    BlockPos iPos = new BlockPos(
                            x - dirLeft.getStepX() * v + dir.getStepX() * d,
                            y + h,
                            z - dirLeft.getStepZ() * v + dir.getStepZ() * d);

                    if (iPos.getX() == x && iPos.getY() == y && iPos.getZ() == z) {
                        world.setBlock(iPos, PileBlocks.PILE_BLOCK.get().defaultBlockState()
                                .setValue(BlockPile.META, BlockPile.META_CORE), 3);
                        if (world.getBlockEntity(iPos) instanceof PileCoreBlockEntity core) {
                            core.orientation = PileOrientation.getOrientation(dir);
                            core.setupSize(posHeight, negHeight, left, right, depth + 1);
                        }
                    } else {
                        int edgeCount = 0;
                        if (h == -negHeight || h == posHeight) edgeCount++;
                        if (v == -left || v == right) edgeCount++;
                        if (d == 0 || d == depth) edgeCount++;
                        boolean isEdge = edgeCount > 1;
                        world.setBlock(iPos, PileBlocks.PILE_BLOCK.get().defaultBlockState()
                                .setValue(BlockPile.META, isEdge ? BlockPile.META_EDGE : BlockPile.META_DUMMY), 3);
                        if (world.getBlockEntity(iPos) instanceof PileBaseBlockEntity pile) {
                            pile.setCore(x, y, z);
                        }
                    }
                }
            }
        }

        return true;
    }
}
