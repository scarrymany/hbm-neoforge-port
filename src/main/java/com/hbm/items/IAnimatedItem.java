package com.hbm.items;

import com.hbm.render.anim.BusAnimation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Interface for items with a custom held-item animation.
 *
 * Returns a {@link BusAnimation} (ported in full, along with its {@link
 * com.hbm.render.anim.BusAnimationSequence}/{@link com.hbm.render.anim.BusAnimationKeyframe}
 * support classes - see that class's own javadoc). No implementor is committed in this tree yet:
 * {@code ItemGrenadeUniversal}'s and {@code ItemChainsaw}'s own javadocs both document their
 * CE {@code IAnimatedItem}/{@code getAnimation()} logic as deferred client-rendering scope, not
 * a missing dependency.
 */
public interface IAnimatedItem {

    @OnlyIn(Dist.CLIENT)
    BusAnimation getAnimation(CompoundTag data, ItemStack stack);
}
