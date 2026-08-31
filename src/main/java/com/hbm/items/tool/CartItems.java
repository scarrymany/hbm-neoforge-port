package com.hbm.items.tool;

import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Minimal, real item registrations backing {@code com.hbm.entity.cart}'s
 * {@code EntityMinecartNTM#getCartItem()} drop-on-death hook (Phase 4,
 * {@code docs/phase4/entities_vehicles_aircraft.md}'s minecart section).
 * <p>
 * <b>Scope, stated explicitly.</b> CE's real placement/drop item for this whole family is {@code
 * com.hbm.items.tool.ItemModMinecart} - a single item with 5 CE metadata subtypes (one per {@code
 * EnumMinecart} cart type) x 4 cosmetic base skins, plus a full 1.12 {@code ModelBakeEvent}
 * dynamic-model-baking pipeline (layered base+overlay textures per skin) and a {@code useOn} that
 * places the matching cart entity on a rail. None of that item-side machinery is ported here - it is
 * real, un-owned {@code items/tool} scope (dynamic model baking in particular has no 1.21 render-side
 * equivalent yet anywhere in this port, matching this port's established "defer 1.12 dynamic
 * item-model baking to Phase 5" precedent). This class supplies exactly what the entity family
 * (this package's actual named scope) needs to compile and function end-to-end for its own real
 * behavior (dying and dropping something real, preserving a custom name) - 5 plain, undecorated
 * items, one per CE {@code EnumMinecart} constant, matching this port's discrete-item-per-variant
 * convention (see {@code com.hbm.itempool.ItemPool}'s own class javadoc for the same convention
 * applied to CE's other metadata-subtype families). A rail-placement {@code useOn}/creative-tab
 * item is a real, separate, un-owned follow-up - see this package's own {@code knownGaps}.
 */
public final class CartItems {

    public static DeferredItem<Item> CART_ORE;
    public static DeferredItem<Item> CART_POWDER;
    public static DeferredItem<Item> CART_SEMTEX;
    public static DeferredItem<Item> CART_CRATE;
    public static DeferredItem<Item> CART_DESTROYER;

    private CartItems() {
    }

    public static void registerAll() {
        CART_ORE = ModItems.ITEMS.register("cart_ntm_ore", () -> new Item(new Item.Properties().stacksTo(1)));
        CART_POWDER = ModItems.ITEMS.register("cart_ntm_powder", () -> new Item(new Item.Properties().stacksTo(1)));
        CART_SEMTEX = ModItems.ITEMS.register("cart_ntm_semtex", () -> new Item(new Item.Properties().stacksTo(1)));
        CART_CRATE = ModItems.ITEMS.register("cart_ntm_crate", () -> new Item(new Item.Properties().stacksTo(1)));
        CART_DESTROYER = ModItems.ITEMS.register("cart_ntm_destroyer", () -> new Item(new Item.Properties().stacksTo(1)));
    }
}
