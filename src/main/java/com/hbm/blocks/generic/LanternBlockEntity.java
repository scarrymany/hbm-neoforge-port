package com.hbm.blocks.generic;

import com.hbm.blockentity.ITickableBE;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * CE {@code TileEntityLantern}: every 20t, blindness 5s on glyphids in 7.5 around the lamp head.
 */
public class LanternBlockEntity extends BlockEntity implements ITickableBE {

    public LanternBlockEntity(BlockPos pos, BlockState state) {
        super(GenericDecoBlocks.LANTERN_ENTITY_TYPE.get(), pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        AABB area = new AABB(
                worldPosition.getX() + 0.5, worldPosition.getY() + 5.5, worldPosition.getZ() + 0.5,
                worldPosition.getX() + 0.5, worldPosition.getY() + 5.5, worldPosition.getZ() + 0.5
        ).inflate(7.5);
        List<EntityGlyphid> glyphids = level.getEntitiesOfClass(EntityGlyphid.class, area);
        for (EntityGlyphid glyphid : glyphids) {
            glyphid.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }
    }
}
