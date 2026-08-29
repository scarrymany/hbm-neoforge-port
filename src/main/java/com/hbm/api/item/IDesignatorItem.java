package com.hbm.api.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IDesignatorItem {

    /**
     * Whether the target is valid
     * @param world for things like restricting dimensions or getting entities
     * @param stack to check components and metadata
     * @param x position of the launch pad
     * @param y position of the launch pad
     * @param z position of the launch pad
     */
    boolean isReady(Level world, ItemStack stack, int x, int y, int z);

    /**
     * The target position if the designator is ready
     * @return the target
     */
    Vec3 getCoords(Level world, ItemStack stack, int x, int y, int z);
}
