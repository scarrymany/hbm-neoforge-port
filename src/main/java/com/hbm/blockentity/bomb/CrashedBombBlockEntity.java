package com.hbm.blockentity.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.bomb.CrashedBombBlock.EnumDudType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Ported from CE's {@code TileEntityCrashedBomb} (62 lines, read in full) - passive-irradiation tick
 * for dud/salvage nuke wreckage, every 2 ticks, scaled by {@link EnumDudType}
 * ({@code BALEFIRE}/{@code NUKE}/{@code SALTED}; {@code CONVENTIONAL} does nothing, matching CE's own
 * {@code switch} which has no {@code CONVENTIONAL} case). Uses {@link ContaminationUtil#contaminate}
 * directly (already ported), per this task's instructions.
 */
public class CrashedBombBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    private final EnumDudType dudType;

    public CrashedBombBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, EnumDudType dudType) {
        super(type, pos, state);
        this.dudType = dudType;
    }

    public EnumDudType getDudType() {
        return dudType;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide()) return;
        if (level.getGameTime() % 2 != 0) return;

        switch (dudType) {
            case BALEFIRE -> affectEntities((entity, intensity) ->
                    ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, 1F * intensity), 15D);
            case NUKE -> affectEntities((entity, intensity) ->
                    ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, 0.25F * intensity), 10D);
            case SALTED -> affectEntities((entity, intensity) ->
                    ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, 0.5F * intensity), 10D);
            case CONVENTIONAL -> {
                // no-op, matches CE's switch (no CONVENTIONAL case)
            }
        }
    }

    private void affectEntities(BiConsumer<LivingEntity, Float> effect, double range) {
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(worldPosition).inflate(range));
        for (LivingEntity entity : list) {
            double dx = entity.getX() - (worldPosition.getX() + 0.5);
            double dy = (entity.getY() + entity.getBbHeight() / 2) - (worldPosition.getY() + 0.5);
            double dz = entity.getZ() - (worldPosition.getZ() + 0.5);
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > range) continue;
            float intensity = (float) (1D - dist / range);
            effect.accept(entity, intensity);
        }
    }
}
