package com.hbm.items;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;

/**
 * Implemented by an {@link net.minecraft.world.item.Item} subclass that needs model generation
 * beyond {@link ModItemModelProvider}'s default {@code basicItem(...)} call (multi-layer textures,
 * a {@code builtin/entity} renderer, item-property overrides, ...). Confirmed real pattern, ported
 * from the Neo Edition reference's {@code com.hbm.items.ICustomItemModelRegister}.
 *
 * <p>Distinct from the CE-signature {@link IModelRegister} marker (no arguments, does not carry the
 * {@link ItemModelProvider}/{@link ResourceLocation} a NeoForge datagen provider needs to hand back
 * to the item), which is kept untouched for whatever other purpose ported it.
 */
public interface ICustomItemModelRegister {

    void registerItemModel(ItemModelProvider provider, ResourceLocation modelLocation);
}
