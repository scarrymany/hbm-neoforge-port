package com.hbm.blocks.generic;

import com.hbm.config.GeneralConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from CE's {@code BlockMush}: a small mushroom that can only grow on
 * {@link PlantBlocks#isWasteGround waste earth/mycelium}, occasionally converts the waste-earth
 * ground beneath it into mycelium, and bonemeals into a huge-mushroom structure via CE's
 * {@code HugeMush} world-gen feature class.
 * <p>
 * That world-gen feature is not part of this block-registration pass (it is a self-contained
 * structure-placement algorithm belonging with the rest of Phase 2+ world generation, not the
 * block class itself), so {@link #isValidBonemealTarget} always returns {@code false} for now -
 * the bonemeal hook is inert until that feature is ported, matching this area's established
 * treatment of other Phase-2-blocked hooks (e.g. {@code IRadResistantBlock}). The ground-check and
 * mycelium-spread behavior are fully self-contained and preserved.
 */
public class BlockMush extends BushBlock implements BonemealableBlock {

    private static final AABB MUSHROOM_AABB = new AABB(0.3D, 0.0D, 0.3D, 0.7D, 0.4D, 0.7D);

    public static final MapCodec<BlockMush> CODEC = simpleCodec(BlockMush::new);

    public BlockMush(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BlockMush> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return PlantBlocks.isWasteGround(state.getBlock());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.create(MUSHROOM_AABB);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        if (GeneralConfig.ENABLE_MYCELIUM_SPREAD.get() && PlantBlocks.isWasteGround(level.getBlockState(pos.below()).getBlock()) && random.nextInt(5) == 0) {
            level.setBlock(pos.below(), PlantBlocks.WASTE_MYCELIUM.get().defaultBlockState(), 3);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.4F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        // no-op until CE's HugeMush world-gen feature is ported - see class javadoc.
    }
}
