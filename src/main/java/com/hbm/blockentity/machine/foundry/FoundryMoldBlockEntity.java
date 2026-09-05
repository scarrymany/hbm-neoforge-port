package com.hbm.blockentity.machine.foundry;

import com.hbm.blockentity.ITickableBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NeoForge port of CE {@code TileEntityFoundryMold} - small mold holder for single-mold casting.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityFoundryMold.java
 * <p>
 * Mold size: 0 (small molds) (CE :14)
 */
public class FoundryMoldBlockEntity extends FoundryCastingBaseBlockEntity implements ITickableBE {

    public FoundryMoldBlockEntity(BlockPos pos, BlockState state) {
        super(FoundryBlockEntities.FOUNDRY_MOLD_BE_TYPE.get(), pos, state);
    }

    @Override
    public int getMoldSize() {
        return 0;
    }

    @Override
    public void updateEntity() {
        tick();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof FoundryMoldBlockEntity be) {
            be.tick();
        }
    }
}
