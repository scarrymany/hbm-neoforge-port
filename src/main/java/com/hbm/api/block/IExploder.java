package com.hbm.api.block;

import com.hbm.entity.item.EntityTNTPrimedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

//Original name: IFuckingExplode
//Changed it to be more professional
public interface IExploder {

    //Prevents stack overflows
    void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity);

    default void explodeEntity(Level world, BlockPos pos, @Nullable EntityTNTPrimedBase entity) {
        explodeEntity(world, pos.getX(), pos.getY(), pos.getZ(), entity);
    }

}
