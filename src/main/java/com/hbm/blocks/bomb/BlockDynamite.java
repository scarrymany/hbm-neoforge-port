package com.hbm.blocks.bomb;

import com.hbm.entity.item.EntityTNTPrimedBase;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Ported from CE's {@code com.hbm.blocks.bomb.BlockDynamite} (21 lines, read in full). */
public class BlockDynamite extends BlockTNTBase {

    public BlockDynamite(Properties properties) {
        super(properties);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        level.explode(entity, x, y, z, 8F, true, Level.ExplosionInteraction.TNT);
    }
}
