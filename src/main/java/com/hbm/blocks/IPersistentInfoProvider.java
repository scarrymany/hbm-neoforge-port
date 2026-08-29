package com.hbm.blocks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Tooltip contract for stacks whose extra info is stored in a persistent NBT blob pre-extracted by
 * the caller, ported from CE. Not implemented by anything in this area's scope, so the exact NBT
 * keys this "persistent tag" holds are unknown from here. Per the project's data-components rule,
 * whichever item area first implements this against real NBT keys must revisit the {@link CompoundTag}
 * parameter and replace the underlying storage with a proper {@code DataComponentType}; this
 * interface only carries the tooltip-contract shape forward, matching the confirmed modern
 * {@code appendHoverText} tooltip signature.
 */
public interface IPersistentInfoProvider {

    void addPersistentInfo(ItemStack stack, CompoundTag persistentTag, Player player, List<Component> tooltip,
                            TooltipContext context, TooltipFlag flag);
}
