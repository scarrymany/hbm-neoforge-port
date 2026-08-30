package com.hbm.blocks.network;

import com.hbm.api.block.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.blocks.network.BlockConveyor} (read in full) - the plain visible
 * conveyor. CE's {@code getPickBlock}/{@code getItemDropped} return a damage-valued
 * {@code ItemConveyorWand}; that item is not ported yet anywhere in this port (confirmed by a fresh
 * search - it needs 4 distinct post-flattening items, one per conveyor variant, an item-side gap
 * {@code docs/phase2/blocks_network_conveyor_crane.md} explicitly leaves to whoever owns that item).
 * Until then this block simply drops/picks itself, like every other block registered through
 * {@code ModBlocks.BLOCKS} ({@link com.hbm.blocks.datagen.ModBlockLootTableProvider} generates a
 * {@code dropSelf} loot table for every entry automatically - no override needed here).
 */
public class BlockConveyor extends BlockConveyorBendable {

    public BlockConveyor(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ, InteractionHand hand,
                            IToolable.ToolType tool) {
        if (tool != IToolable.ToolType.SCREWDRIVER) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        if (!player.isShiftKeyDown()) {
            world.setBlock(pos, state.rotate(Rotation.CLOCKWISE_90), 3);
        } else {
            CurveType curve = state.getValue(CURVE);
            Direction facing = state.getValue(FACING);
            if (curve == CurveType.RIGHT) {
                BlockState liftState = ConveyorBlocks.CONVEYOR_LIFT.get().defaultBlockState().setValue(FACING, facing);
                world.setBlock(pos, liftState, 3);
            } else {
                CurveType newCurve = (curve == CurveType.STRAIGHT) ? CurveType.LEFT : CurveType.RIGHT;
                world.setBlock(pos, state.setValue(CURVE, newCurve), 3);
            }
        }
        return true;
    }
}
