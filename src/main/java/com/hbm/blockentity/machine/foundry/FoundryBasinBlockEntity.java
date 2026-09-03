package com.hbm.blockentity.machine.foundry;

import com.hbm.blockentity.ITickableBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NeoForge port of CE {@code TileEntityFoundryBasin} - foundry basin for catching molten metal.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityFoundryBasin.java
 * <p>
 * Basin size: 1 (CE :12)
 */
public class FoundryBasinBlockEntity extends FoundryCastingBaseBlockEntity implements ITickableBE {

    public FoundryBasinBlockEntity(BlockPos pos, BlockState state) {
        super(FoundryBlockEntities.FOUNDRY_BASIN_BE_TYPE.get(), pos, state);
    }

    @Override
    public int getMoldSize() {
        return 1;
    }

    @Override
    public void updateEntity() {
        tick();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof FoundryBasinBlockEntity be) {
            be.tick();
        }
    }
}
