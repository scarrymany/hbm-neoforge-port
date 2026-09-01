package com.hbm.blocks.fluid;

import com.hbm.blocks.generic.BlockSellafield;
import com.hbm.blocks.generic.WastelandVirusBlocks;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * CE {@code com.hbm.blocks.fluid.ToxicBlock} ({@code ToxicBlock.java}:24-105).
 * <p>
 * Still-block stand-in: CE is {@code BlockFluidClassic} quanta 4. Flow / displace /
 * fog are not ported — TODO(CE: ToxicBlock.java:26-49) fluid displace;
 * TODO(CE: ToxicBlock.java:97-105) fog density 2.0 / color {@code 0x503920}.
 * Collision slow + {@code contaminate RADIATION CREATIVE 1.0F} and neighbor
 * liquid → random sellafield META 5/4/3/2 match CE.
 */
public class ToxicBlock extends Block {

    public ToxicBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // CE setInWeb()
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 1.0F);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (reactToBlocks(level, pos.east())) level.setBlock(pos.east(), randomSellafite(level), 3);
        if (reactToBlocks(level, pos.west())) level.setBlock(pos.west(), randomSellafite(level), 3);
        if (reactToBlocks(level, pos.above())) level.setBlock(pos, randomSellafite(level), 3);
        if (reactToBlocks(level, pos.below())) level.setBlock(pos.below(), randomSellafite(level), 3);
        if (reactToBlocks(level, pos.south())) level.setBlock(pos.south(), randomSellafite(level), 3);
        if (reactToBlocks(level, pos.north())) level.setBlock(pos.north(), randomSellafite(level), 3);
    }

    private static boolean reactToBlocks(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ToxicBlock) return false;
        return !state.getFluidState().isEmpty();
    }

    /** CE {@code getRandomSellafite} ({@code ToxicBlock.java}:76-81). */
    private static BlockState randomSellafite(Level level) {
        RandomSource random = level.getRandom();
        int n = random.nextInt(100);
        int meta;
        if (n < 2) meta = 5;
        else if (n < 20) meta = 4;
        else if (n < 60) meta = 3;
        else meta = 2;
        return WastelandVirusBlocks.SELLAFIELD.get().defaultBlockState().setValue(BlockSellafield.LEVEL, meta);
    }
}
