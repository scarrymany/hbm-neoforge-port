package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code StorageDrumRecipes.java}:20-58. Only rows whose I/O ids exist.
 * Long/short depleted siblings are unregistered → those CE rows stay skipped.
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

        // CE :56-57 — only if both ends exist (bottle_mercury / ingot_mercury are missing)
        putIfBoth("ingot_au198", "bottle_mercury", 2160, 500, 500);
        putIfBoth("nugget_au198", "ingot_mercury", 216, 50, 50);

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

    private static void putIfBoth(String inId, String outId, int chance, int liquid, int gas) {
        Item in = item(inId);
        Item out = item(outId);
        if (in == Items.AIR || out == Items.AIR) return;
        recipes.put(new ComparableStack(in), new WasteData(new ItemStack(out), chance, liquid, gas));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }
}
