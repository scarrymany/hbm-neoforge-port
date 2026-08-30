package com.hbm.blocks.bomb;

import com.hbm.entity.item.EntityTNTPrimedBase;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Ported from CE's {@code com.hbm.blocks.bomb.BlockC4} (20 lines, read in full). */
public class BlockC4 extends BlockTNTBase {

    public BlockC4(Properties properties) {
        super(properties);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        level.explode(entity, x, y, z, 15F, true, Level.ExplosionInteraction.TNT);
    }
}
