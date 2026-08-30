package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Collections;
import java.util.List;

/**
 * Decorative dungeon-ruin brick, ported from CE's {@code BlockRedBrick}. CE's {@code META}
 * {@code PropertyInteger} only ever stored the placement facing (set once in
 * {@code getStateForPlacement} from the placer's direction, read only by the renderer) - it is not
 * a content variant, so it survives as a real {@code DirectionProperty} block-state property rather
 * than being flattened into per-direction registry entries. CE's {@code getItemDropped} returning
 * {@code Items.AIR} (this block never drops itself) is preserved via an explicit empty
 * {@link #getDrops} override, independent of whatever loot table datagen eventually assigns it.
 */
public class BlockRedBrick extends BlockBase {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public BlockRedBrick(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return Collections.emptyList();
    }
}
