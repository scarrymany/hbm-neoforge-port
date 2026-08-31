package com.hbm.items.armor;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Registered {@code Item}s for {@link ItemModCharm} - {@code protection_charm}/{@code meteor_charm},
 * CE's real registry names ({@code ModItems.java:245-246}). Lives outside the shared
 * {@code ModItems}/{@code ModBlocks} files per this wave's per-package {@code registerAll()}
 * convention (see e.g. {@code com.hbm.items.weapon.grenade.GrenadeItems}'s own javadoc for the same
 * rationale) - {@code com.hbm.items.ModItems.register} needs one added call to reach this class (see
 * this package's wiringSnippets).
 */
public final class ModCharmItems {

    private ModCharmItems() {
    }

    public static final DeferredItem<ItemModCharm> PROTECTION_CHARM =
            ModItems.ITEMS.register("protection_charm", () -> new ItemModCharm(new Item.Properties(), false));
    public static final DeferredItem<ItemModCharm> METEOR_CHARM =
            ModItems.ITEMS.register("meteor_charm", () -> new ItemModCharm(new Item.Properties(), true));

    public static void registerAll() {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, PROTECTION_CHARM);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, METEOR_CHARM);
    }
}
