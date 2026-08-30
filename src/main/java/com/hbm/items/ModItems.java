package com.hbm.items;

import com.hbm.items.special.BedrockOreComponents;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.tool.ToolDataComponents;
import com.hbm.items.tool.ToolItems;
import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry skeleton for Phase 1 to populate with CE's ~440 item entries.
 *
 * CE's ModItems built each Item via "public static final Item x = new XSubclass(...).setX()"
 * construction-as-registration, collected into a plain ALL_ITEMS list and registered in one pass
 * by registerItems(). NeoForge replaces both the construction-as-registration pattern and the
 * ALL_ITEMS bookkeeping with DeferredRegister.Items: each entry becomes
 * "ITEMS.register(name, () -> new XSubclass(new Item.Properties()))", and DeferredRegister
 * itself tracks what was registered - there is no ALL_ITEMS equivalent to maintain.
 *
 * Pattern confirmed against the Neo Edition reference's NtmItems
 * (DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
 *  DeferredItem<Item> X = ITEMS.register("x", () -> new Item(new Item.Properties()));).
 *
 * Creative tab placement is deliberately NOT part of this class: CE baked setCreativeTab into
 * every item constructor call, but 1.21 assigns creative tab contents via a
 * BuildCreativeModeTabContentsEvent listener owned elsewhere. Phase 1 item entries should not
 * try to bolt a setCreativeTab-shaped API back onto ItemBase.
 */
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MainRegistry.MODID);

    public static void register(IEventBus modEventBus) {
        MaterialItemGenerator.registerAll();
        PlateCrystalWasteItems.registerAll();
        BilletPowderItems.registerAll();
        IngotNuggetItems.registerAll();
        BedrockOreItems.bootstrap();
        BedrockOreComponents.register(modEventBus);
        ToolItems.registerAll();
        ToolDataComponents.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
