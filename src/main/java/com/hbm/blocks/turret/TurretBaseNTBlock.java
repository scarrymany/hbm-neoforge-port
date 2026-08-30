package com.hbm.blocks.turret;

import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code TurretBaseNT} - the shared abstract {@link BlockDummyable} casing for
 * every 2x2 multiblock turret (all concrete turrets except the single-block Sentry family and the
 * out-of-scope Arty/HIMARS). Fixed dimensions ({@code {0,0,1,0,1,0}}, offset 0) and low bounding box
 * (0-0.5 high, matching CE's fixed {@code AxisAlignedBB(0,0,0,1,0.5,1)} override) are CE's own exact
 * values, not derived.
 */
public abstract class TurretBaseNTBlock extends BlockDummyable {

    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);

    protected TurretBaseNTBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 0, 1, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }
}
