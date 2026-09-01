package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.CyberCrabBlockEntity;
import com.hbm.blocks.generic.Phase8Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

/**
 * CE {@code BlockCybercrab} — registry id {@code meteor_spawner}. Drops air. MODEL.
 */
public class BlockCybercrab extends BaseEntityBlock {

    public static final MapCodec<BlockCybercrab> CODEC = simpleCodec(BlockCybercrab::new);

    public BlockCybercrab(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CyberCrabBlockEntity(Phase8Blocks.METEOR_SPAWNER_ENTITY_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == Phase8Blocks.METEOR_SPAWNER_ENTITY_TYPE.get() ? ITickableBE.ticker() : null;
    }
}
