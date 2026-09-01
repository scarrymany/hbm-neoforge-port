package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** CE {@code TileEntityLanternBehemoth} render bbox. Repair easter-egg not ported. */
public class LanternBehemothBlockEntity extends BlockEntity {

    public LanternBehemothBlockEntity(BlockPos pos, BlockState state) {
        super(GenericDecoBlocks.LANTERN_BEHEMOTH_ENTITY_TYPE.get(), pos, state);
    }

    public AABB getRenderBoundingBox() {
        BlockPos pos = worldPosition;
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 6, pos.getZ() + 1);
    }
}
