package com.hbm.items;

import com.hbm.render.anim.BusAnimation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Interface for items with a custom held-item animation.
 *
 * Depends on com.hbm.render.anim.BusAnimation, client-rendering content out of this agent's
 * scope; that class does not exist in this tree yet.
 */
public interface IAnimatedItem {

    @OnlyIn(Dist.CLIENT)
    BusAnimation getAnimation(CompoundTag data, ItemStack stack);
}
