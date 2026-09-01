package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
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
 * CE {@code TileEntityMachineRadGen.java}:236-251 inline {@code fuels} map.
 * Census: {@code recipes.put}. Depleted/tiny outputs skipped (unregistered).
 */
public final class RadGenRecipes {

    public static final Map<ComparableStack, RadGenFuel> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private RadGenRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // CE TileEntityMachineRadGen.java:241-246 — waste families, no depleted output
        recipes.put(shortWaste(ItemWasteShort.WasteClass.URANIUM233), shortFuel());
        recipes.put(shortWaste(ItemWasteShort.WasteClass.URANIUM235), shortFuel());
        recipes.put(shortWaste(ItemWasteShort.WasteClass.NEPTUNIUM), shortFuel());
        recipes.put(shortWaste(ItemWasteShort.WasteClass.PLUTONIUM239), shortFuel());
        recipes.put(shortWaste(ItemWasteShort.WasteClass.PLUTONIUM240), shortFuel());
        recipes.put(shortWaste(ItemWasteShort.WasteClass.PLUTONIUM241), shortFuel());
        recipes.put(shortWaste(ItemWasteShort.WasteClass.AMERICIUM242), shortFuel());
        recipes.put(shortWaste(ItemWasteShort.WasteClass.SCHRABIDIUM), shortFuel());
        recipes.put(longWaste(ItemWasteLong.WasteClass.THORIUM), longFuel());
        recipes.put(longWaste(ItemWasteLong.WasteClass.URANIUM233), longFuel());
        recipes.put(longWaste(ItemWasteLong.WasteClass.URANIUM235), longFuel());
        recipes.put(longWaste(ItemWasteLong.WasteClass.NEPTUNIUM), longFuel());
        recipes.put(longWaste(ItemWasteLong.WasteClass.SCHRABIDIUM), longFuel());
        // CE :250 gem_rad → diamond
        Item gem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "gem_rad"));
        if (gem != Items.AIR) {
            recipes.put(new ComparableStack(gem), new RadGenFuel(25_000, 30 * 60 * 20, new ItemStack(Items.DIAMOND)));
        }
    }

    private static ComparableStack shortWaste(ItemWasteShort.WasteClass waste) {
        return new ComparableStack(SpecialItems.nuclearWasteShort(waste).get());
    }

    private static ComparableStack longWaste(ItemWasteLong.WasteClass waste) {
        return new ComparableStack(SpecialItems.nuclearWasteLong(waste).get());
    }

    private static RadGenFuel shortFuel() {
        return new RadGenFuel(1500, 30 * 60 * 20, ItemStack.EMPTY);
    }

    private static RadGenFuel longFuel() {
        return new RadGenFuel(500, 2 * 60 * 60 * 20, ItemStack.EMPTY);
    }

    public static RadGenFuel getFuel(ItemStack stack) {
        register();
        if (stack == null || stack.isEmpty()) return null;
        return recipes.get(new ComparableStack(stack).makeSingular());
    }

    public static final class RadGenFuel {
        public final int powerPerTick;
        public final int duration;
        public final ItemStack leftover;

        public RadGenFuel(int powerPerTick, int duration, ItemStack leftover) {
            this.powerPerTick = powerPerTick;
            this.duration = duration;
            this.leftover = leftover;
        }
    }
}
