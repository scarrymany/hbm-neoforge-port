package com.hbm.items.tool;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Registered {@code Item} for {@link ItemMeteorRemote} - CE's real registry name
 * ({@code meteor_remote}). Lives outside the shared {@code ModItems} file per this wave's
 * per-package {@code registerAll()} convention (see e.g.
 * {@code com.hbm.items.weapon.grenade.GrenadeItems}'s own javadoc for the same rationale) -
 * {@code com.hbm.items.ModItems.register} needs one added call to reach this class (see this
 * package's wiringSnippets).
 */
public final class MeteorToolItems {

    private MeteorToolItems() {
    }

    public static final DeferredItem<ItemMeteorRemote> METEOR_REMOTE =
            ModItems.ITEMS.register("meteor_remote", () -> new ItemMeteorRemote(new Item.Properties().stacksTo(1).durability(2)));

    public static void registerAll() {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, METEOR_REMOTE);
    }
}
