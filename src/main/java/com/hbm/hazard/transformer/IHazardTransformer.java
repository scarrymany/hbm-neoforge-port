package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A pre/post processing step wrapped around the tag/item/stack merge in {@link com.hbm.hazard.HazardSystem}.
 * Registered transformers run in order before ({@link #transformPre}) and after ({@link #transformPost}) the
 * registry-driven entries are unrolled, so they can inject or scale dynamic hazard sources (NBT-derived bonuses,
 * fluid contents, nested containers, addon hooks, ...).
 */
public interface IHazardTransformer {

    void transformPre(ItemStack stack, List<HazardEntry> entries);

    void transformPost(ItemStack stack, List<HazardEntry> entries);
}
