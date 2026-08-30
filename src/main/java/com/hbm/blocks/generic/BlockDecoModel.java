package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Generic OBJ-modeled deco prop, ported from CE's {@code BlockDecoModel<E>} (a
 * {@code BlockEnumMeta<E>} subclass). CE reuses the base class for two registry entries -
 * {@code deco_computer} (this block) and {@code filing_cabinet} (CE's {@code BlockDecoContainer<E>}
 * subclass instead, a chest-like container with its own inventory TE, out of this simple-deco
 * slice's scope; whoever ports the storage-block family should give it its own home rather than
 * folding it in here). Per the metadata-flattening ground rule this becomes one instance per
 * {@link BlockEnums.DecoComputerEnum} constant (today just {@code IBM_300PL}) instead of CE's single
 * multi-metadata registry entry, with rotation kept as a real {@code FACING} block-state property -
 * the same convention {@link BlockDecoCRT}/{@link BlockDecoToaster} already use in this package.
 * <p>
 * <b>Custom model gap.</b> Per {@link BlockScaffold}'s javadoc precedent: CE's real content here is a
 * bespoke {@code .obj} mesh ({@code models/blocks/puter.obj}, baked via {@code HFRWavefrontObject}/
 * {@code BlockDecoBakedModel}), and NeoForge 1.21 has no confirmed geometry-loader equivalent for
 * that pipeline yet. Rather than guess at an API, this block registers with no
 * {@link com.hbm.blocks.ICustomBlockModelRegister} override, so {@code ModBlockStateProvider}'s
 * default cube-all fallback applies - a plain placeholder cube standing in for the real mesh until
 * whoever lands a NeoForge OBJ/custom-geometry loader ports {@code BlockDecoBakedModel} properly.
 */
public class BlockDecoModel extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final BlockEnums.DecoComputerEnum variant;

    public BlockDecoModel(Properties properties, BlockEnums.DecoComputerEnum variant) {
        super(properties);
        this.variant = variant;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public BlockEnums.DecoComputerEnum getVariant() {
        return variant;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // CE's setBlockBoundsTo(.125F, 0F, 0F, .875F, .875F, .625F) for deco_computer.
        return Block.box(2.0, 0.0, 0.0, 14.0, 14.0, 10.0);
    }
}
