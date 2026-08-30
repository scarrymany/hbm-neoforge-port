package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decorative wood-structure block, ported from CE's {@code BlockWoodStructure}. CE's three enum
 * variants ({@code ROOF}/{@code SCAFFOLD}/{@code CEILING}) never change after placement and are not
 * exposed as separate blockstate content, so per the flattening rule each becomes its own registry
 * entry built from this one class, selected once at construction via {@link Kind} - the same shape
 * this port already used for {@link BlockRailing}'s type field.
 * <p>
 * {@code SCAFFOLD}'s climbability (CE: {@code isLadder}) has no direct 1.21 equivalent - modern
 * climbing is entirely {@code minecraft:climbable} block-tag driven (confirmed against this
 * toolchain's decompiled {@code LadderBlock}/{@code BlockTags}); flagged in the port report as a
 * one-line datapack follow-up rather than guessed at here. Its "solid on top" behavior (CE forced
 * {@code BlockFaceShape.SOLID} for the {@code UP} face only) is reproduced via a dedicated
 * {@link #getBlockSupportShape} override, kept independent of the narrower inset shape used for the
 * block's actual collision/outline.
 */
public class BlockWoodStructure extends Block {

    public enum Kind { ROOF, SCAFFOLD, CEILING }

    private static final VoxelShape ROOF_SHAPE = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape SCAFFOLD_SHAPE = Block.box(1, 0, 1, 15, 16, 15);
    private static final VoxelShape CEILING_SHAPE = Block.box(0, 14, 0, 16, 16, 16);

    private final Kind kind;
    private final VoxelShape shape;

    public BlockWoodStructure(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.shape = switch (kind) {
            case ROOF -> ROOF_SHAPE;
            case SCAFFOLD -> SCAFFOLD_SHAPE;
            case CEILING -> CEILING_SHAPE;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return kind == Kind.SCAFFOLD ? Shapes.block() : shape;
    }

    public Kind getKind() {
        return kind;
    }
}
