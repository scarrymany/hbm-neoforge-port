package com.hbm.blocks.machine.foundry;

import com.hbm.blockentity.machine.foundry.FoundryBasinBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
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
 * NeoForge port of CE {@code FoundryBasin} - foundry basin for catching molten metal.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/FoundryBasin.java
 * <p>
 * Basin shape: bottom + 4 walls (CE :30-35).
 */
public class BlockFoundryBasin extends BlockFoundryCastingBase {

    public static final MapCodec<BlockFoundryBasin> CODEC = simpleCodec(BlockFoundryBasin::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0),         // bottom
            Shapes.box(0.0, 0.125, 0.0, 1.0, 1.0, 0.125),       // north wall
            Shapes.box(0.0, 0.125, 0.875, 1.0, 1.0, 1.0),       // south wall
            Shapes.box(0.0, 0.125, 0.125, 0.125, 1.0, 0.875),   // west wall
            Shapes.box(0.875, 0.125, 0.125, 1.0, 1.0, 0.875)    // east wall
    );

    public BlockFoundryBasin(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<BlockFoundryBasin> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FoundryBasinBlockEntity(pos, state);
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public double getPH() {
        return 0.875D;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return (tickerLevel, tickerPos, tickerState, tickerBlockEntity) -> {
            if (tickerBlockEntity instanceof FoundryBasinBlockEntity be) {
                be.tick();
            }
        };
    }
}
