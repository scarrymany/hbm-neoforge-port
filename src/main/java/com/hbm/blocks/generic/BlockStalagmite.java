package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums.EnumStalagmiteType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from CE's {@code BlockStalagmite}: a passable, non-full cave decoration that requires
 * solid ground on the side it grows from and breaks (dropping its raw material) when that support
 * disappears. CE registered this class twice with different registry names ({@code stalagmite}
 * growing up off the floor, {@code stalactite} hanging down off the ceiling) and told the two
 * apart with a {@code this == ModBlocks.stalagmite} identity check; the port replaces that with an
 * explicit {@code hangsFromCeiling} constructor flag. Each of the two base names carries one
 * {@link EnumStalagmiteType} variant per registered block (see {@link OreMineralBlocks}).
 *
 * <p>CE's silk-touch/raw-material drop split ({@code sulfur} for {@code SULFUR}, powdered asbestos
 * for {@code ASBESTOS}) is loot-table content in modern Minecraft; there is no Java
 * {@code getDrops} hook left to hang it on. Leave that split to this block's loot table.
 */
public class BlockStalagmite extends Block {

    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);

    public final EnumStalagmiteType type;
    public final boolean hangsFromCeiling;

    public BlockStalagmite(Properties properties, EnumStalagmiteType type, boolean hangsFromCeiling) {
        super(properties);
        this.type = type;
        this.hangsFromCeiling = hangsFromCeiling;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos supportPos = hangsFromCeiling ? pos.above() : pos.below();
        Direction supportFace = hangsFromCeiling ? Direction.DOWN : Direction.UP;
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, supportFace);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathType) {
        return false;
    }
}
