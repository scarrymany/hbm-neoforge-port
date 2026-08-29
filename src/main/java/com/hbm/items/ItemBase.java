package com.hbm.items;

import net.minecraft.world.item.Item;

/**
 * Common superclass for hbm items ported from CE.
 *
 * CE's ItemBase mutated a newly built Item after construction (setTranslationKey,
 * setRegistryName, setCreativeTab) and self-registered into ModItems.ALL_ITEMS. None of that
 * survives the port: registry name and translation key come from the DeferredRegister.Items
 * registration call in ModItems, and creative tab placement is a BuildCreativeModeTabContentsEvent
 * concern owned outside the item class. This class only exists so Phase 1 item subclasses have a
 * single, common hbm-specific type to extend instead of vanilla Item directly, matching CE's
 * package layout.
 */
public class ItemBase extends Item {

    public ItemBase(Properties properties) {
        super(properties);
    }
}
