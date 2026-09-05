package com.hbm.blockentity.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.entity.mob.Phase4BossEntityTypes2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * CE {@code TileEntityCyberCrab}: 1/400 tick, air above, &lt;7 crabs in 11×5×11, 1/5 tesla.
 */
public class CyberCrabBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    public CyberCrabBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (level.random.nextInt(400) != 0) return;
        if (!level.hasChunksAt(worldPosition.offset(-5, -1, -5), worldPosition.offset(5, 3, 5))) return;
        if (!level.getBlockState(worldPosition.above()).isAir()) return;

        AABB box = new AABB(worldPosition.getX() - 5, worldPosition.getY() - 1, worldPosition.getZ() - 5,
                worldPosition.getX() + 5, worldPosition.getY() + 3, worldPosition.getZ() + 5);
        if (level.getEntitiesOfClass(EntityCyberCrab.class, box).size() >= 7) return;

        Entity crab = level.random.nextInt(5) == 0
                ? new EntityTeslaCrab(Phase4BossEntityTypes2.TESLA_CRAB.get(), level)
                : new EntityCyberCrab(Phase4BossEntityTypes2.CYBER_CRAB.get(), level);
        crab.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);
        level.addFreshEntity(crab);
    }
}
