package com.hbm.blocks.machine.foundry;

import com.hbm.blockentity.machine.foundry.FoundryMoldBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge port of CE {@code FoundryMold} - small foundry mold holder for single-mold casting.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/FoundryMold.java
 * <p>
 * Mold shape: bottom + 4 walls, half-height (CE :28-33).
 */
public class BlockFoundryMold extends BlockFoundryCastingBase {

    public static final MapCodec<BlockFoundryMold> CODEC = simpleCodec(BlockFoundryMold::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0),         // bottom
            Shapes.box(0.0, 0.125, 0.0, 1.0, 0.5, 0.125),       // north wall
            Shapes.box(0.0, 0.125, 0.875, 1.0, 0.5, 1.0),       // south wall
            Shapes.box(0.0, 0.125, 0.125, 0.125, 0.5, 0.875),   // west wall
            Shapes.box(0.875, 0.125, 0.125, 1.0, 0.5, 0.875)    // east wall
    );

    public BlockFoundryMold(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<BlockFoundryMold> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FoundryMoldBlockEntity(pos, state);
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public double getPH() {
        return 0.25D;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return (tickerLevel, tickerPos, tickerState, tickerBlockEntity) -> {
            if (tickerBlockEntity instanceof FoundryMoldBlockEntity be) {
                be.tick();
            }
        };
    }
}
