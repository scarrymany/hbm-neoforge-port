package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * CE {@code WasteDrumRecipes.java}:32-50. Hot waste → cooled waste.
 * Each {@code recipes.put} is a census site.
 */
public final class WasteDrumRecipes {

    public static final Map<ComparableStack, ItemStack> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private WasteDrumRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE WasteDrumRecipes.java:33-48
        recipes.put(cmp("waste_natural_uranium_hot"), stack("waste_natural_uranium"));
        recipes.put(cmp("waste_uranium_hot"), stack("waste_uranium"));
        recipes.put(cmp("waste_thorium_hot"), stack("waste_thorium"));
        recipes.put(cmp("waste_mox_hot"), stack("waste_mox"));
        recipes.put(cmp("waste_plutonium_hot"), stack("waste_plutonium"));
        recipes.put(cmp("waste_u233_hot"), stack("waste_u233"));
        recipes.put(cmp("waste_u235_hot"), stack("waste_u235"));
        recipes.put(cmp("waste_schrabidium_hot"), stack("waste_schrabidium"));
        recipes.put(cmp("waste_zfb_mox_hot"), stack("waste_zfb_mox"));
        recipes.put(cmp("waste_plate_u233_hot"), stack("waste_plate_u233"));
        recipes.put(cmp("waste_plate_u235_hot"), stack("waste_plate_u235"));
        recipes.put(cmp("waste_plate_mox_hot"), stack("waste_plate_mox"));
        recipes.put(cmp("waste_plate_pu239_hot"), stack("waste_plate_pu239"));
        recipes.put(cmp("waste_plate_sa326_hot"), stack("waste_plate_sa326"));
        recipes.put(cmp("waste_plate_ra226be_hot"), stack("waste_plate_ra226be"));
        recipes.put(cmp("waste_plate_pu238be_hot"), stack("waste_plate_pu238be"));

        // :50 PWR hot → depleted
        for (EnumPWRFuel pwr : EnumPWRFuel.VALUES) {
            String slug = pwr.name().toLowerCase(Locale.ROOT);
            recipes.put(cmp("pwr_fuel_hot_" + slug), stack("pwr_fuel_depleted_" + slug));
        }

        recipes.entrySet().removeIf(e -> e.getValue().isEmpty() || e.getValue().getItem() == Items.AIR
                || e.getKey().toStack().isEmpty() || e.getKey().toStack().getItem() == Items.AIR);
    }

    public static ItemStack getOutput(ItemStack in) {
        if (in == null || in.isEmpty()) return ItemStack.EMPTY;
        register();
        ItemStack out = recipes.get(new ComparableStack(in));
        return out == null ? ItemStack.EMPTY : out.copy();
    }

    public static boolean isInput(ItemStack in) {
        if (in == null || in.isEmpty()) return false;
        register();
        return recipes.containsKey(new ComparableStack(in));
    }

    private static ComparableStack cmp(String id) {
        return new ComparableStack(item(id));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(String id) {
        Item i = item(id);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i);
    }
}
