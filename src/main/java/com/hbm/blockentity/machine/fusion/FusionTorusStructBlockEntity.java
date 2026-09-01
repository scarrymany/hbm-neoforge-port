package com.hbm.blockentity.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.fusion.FusionBlocks;
import com.hbm.blocks.machine.fusion.MachineFusionTorusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntityFusionTorusStruct} — every 20t checks flattened fusion_component_1/2/3. */
public class FusionTorusStructBlockEntity extends BlockEntity implements ITickableBE {

    public FusionTorusStructBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (level.getGameTime() % 20 != 0) return;

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < MachineFusionTorusBlock.LAYOUT[0].length; x++) {
                for (int z = 0; z < MachineFusionTorusBlock.LAYOUT[0][0].length; z++) {
                    int ly = y > 2 ? 4 - y : y;
                    int i = MachineFusionTorusBlock.LAYOUT[ly][x][z];
                    if (i == 0) continue;
                    if (x == 7 && y == 0 && z == 7) continue;
                    if (!cbr(i, x - 7, y, z - 7)) return;
                }
            }
        }

        MachineFusionTorusBlock block = FusionBlocks.FUSION_TORUS.get();
        BlockDummyable.safeRem = true;
        level.setBlock(worldPosition, block.defaultBlockState().setValue(BlockDummyable.META, 12), 3);
        block.formFromStruct(level, worldPosition);
        BlockDummyable.safeRem = false;
    }

    private boolean cbr(int meta, int x, int y, int z) {
        Block expected = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("hbm:fusion_component_" + meta));
        return level.getBlockState(worldPosition.offset(x, y, z)).is(expected);
    }
}
