package com.hbm.blocks.bomb;

import com.hbm.entity.item.EntityTNTPrimedBase;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Ported from CE's {@code com.hbm.blocks.bomb.BlockTNT} (21 lines, read in full). */
public class BlockTNT extends BlockTNTBase {

    public BlockTNT(Properties properties) {
        super(properties);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        level.explode(entity, x, y, z, 10F, true, Level.ExplosionInteraction.TNT);
    }
}
