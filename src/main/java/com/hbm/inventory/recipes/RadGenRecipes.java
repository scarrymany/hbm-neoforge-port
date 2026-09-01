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
        for (ItemWasteShort.WasteClass waste : ItemWasteShort.WasteClass.VALUES) {
            recipes.put(new ComparableStack(SpecialItems.nuclearWasteShort(waste).get()),
                    new RadGenFuel(1500, 30 * 60 * 20, ItemStack.EMPTY));
        }
        for (ItemWasteLong.WasteClass waste : ItemWasteLong.WasteClass.VALUES) {
            recipes.put(new ComparableStack(SpecialItems.nuclearWasteLong(waste).get()),
                    new RadGenFuel(500, 2 * 60 * 60 * 20, ItemStack.EMPTY));
        }
        // CE :250 gem_rad → diamond
        Item gem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "gem_rad"));
        if (gem != Items.AIR) {
            recipes.put(new ComparableStack(gem), new RadGenFuel(25_000, 30 * 60 * 20, new ItemStack(Items.DIAMOND)));
        }
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
