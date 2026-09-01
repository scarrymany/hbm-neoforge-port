package com.hbm.inventory.recipes;

import com.hbm.config.VersatileConfig;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.special.ItemWasteLong;
import com.hbm.items.special.ItemWasteShort;
import com.hbm.items.special.SpecialItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code StorageDrumRecipes.java}:20-58. Explicit {@code recipes.put} per waste class
 * (CE loops 5 long + 8 short + 2 mercury).
 */
public final class StorageDrumRecipes {

    public static final Map<ComparableStack, WasteData> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    public record WasteData(ItemStack output, int chance, int liquid, int gas) {
    }

    private StorageDrumRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        int longChance = VersatileConfig.getLongDecayChance();
        int shortChance = VersatileConfig.getShortDecayChance();

        for (ItemWasteLong.WasteClass waste : ItemWasteLong.WasteClass.VALUES) {
            recipes.put(new ComparableStack(SpecialItems.nuclearWasteLong(waste).get()),
                    new WasteData(new ItemStack(SpecialItems.nuclearWasteLongDepleted(waste).get()),
                            longChance, waste.liquid, waste.gas));
            recipes.put(new ComparableStack(SpecialItems.nuclearWasteLongTiny(waste).get()),
                    new WasteData(new ItemStack(SpecialItems.nuclearWasteLongDepletedTiny(waste).get()),
                            (int) (longChance * 0.1), (int) (waste.liquid * 0.1), (int) (waste.gas * 0.1)));
        }

        for (ItemWasteShort.WasteClass waste : ItemWasteShort.WasteClass.VALUES) {
            recipes.put(new ComparableStack(SpecialItems.nuclearWasteShort(waste).get()),
                    new WasteData(new ItemStack(SpecialItems.nuclearWasteShortDepleted(waste).get()),
                            shortChance, waste.liquid, waste.gas));
            recipes.put(new ComparableStack(SpecialItems.nuclearWasteShortTiny(waste).get()),
                    new WasteData(new ItemStack(SpecialItems.nuclearWasteShortDepletedTiny(waste).get()),
                            (int) (shortChance * 0.1), (int) (waste.liquid * 0.1), (int) (waste.gas * 0.1)));
        }

        // CE :56 — ModItems.ingot_au198 → bottle_mercury
        recipes.put(new ComparableStack(IngotNuggetItems.INGOT_AU198.get()),
                new WasteData(new ItemStack(BilletPowderItems.BOTTLE_MERCURY.get()),
                        (int) (shortChance * 0.01), 500, 500));
        // CE :57 — nugget_au198 → ModItems.ingot_mercury (registry id nugget_mercury)
        recipes.put(new ComparableStack(IngotNuggetItems.NUGGET_AU198.get()),
                new WasteData(new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get()),
                        (int) (shortChance * 0.001), 50, 50));

        recipes.entrySet().removeIf(e -> e.getValue().output.isEmpty()
                || e.getValue().output.getItem() == Items.AIR
                || e.getKey().toStack().isEmpty()
                || e.getKey().toStack().getItem() == Items.AIR);
    }

    public static WasteData getWaste(ItemStack in) {
        if (in == null || in.isEmpty()) return null;
        register();
        return recipes.get(new ComparableStack(in));
    }

    public static boolean isInput(ItemStack in) {
        return getWaste(in) != null;
    }

    @SuppressWarnings("unused")
    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }
}
