package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CE {@code AnvilRecipes.java}. Smithing {@code :59-131} + construction {@code :140+}.
 * Rows whose I/O is AIR are skipped — no fake ids. Cited skips stay (hot/mold/cyanide/rename,
 * Mats shell/pipe autogen, recycling of unregistered machines).
 */
public final class AnvilRecipes {

    public static final List<AnvilSmithingRecipe> SMITHING = new ArrayList<>();
    public static final List<AnvilConstructionRecipe> CONSTRUCTION = new ArrayList<>();

    private static boolean registered;

    private AnvilRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        registerSmithing();
        registerConstruction();
    }

    public static List<AnvilSmithingRecipe> getSmithing() {
        register();
        return SMITHING;
    }

    public static List<AnvilConstructionRecipe> getConstruction() {
        register();
        return CONSTRUCTION;
    }

    /**
     * CE {@code :59-73} anvil upgrades (iron/lead × 9 targets) + {@code :96} gunmetal.
     * SKIP {@code :75-93} hot ({@code AnvilSmithingHotRecipe}), {@code :98-127} mold,
     * {@code :93-94} wings/flask, {@code :129-130} cyanide/rename —
     * TODO(CE: AnvilRecipes.java:75-130).
     */
    private static void registerSmithing() {
        String[] bases = {"anvil_iron", "anvil_lead"};
        for (String base : bases) {
            smith(1, stack("anvil_steel"), cmp(base), tag("ingots/steel", 10));
            smith(1, stack("anvil_desh"), cmp(base), tag("ingots/desh", 10));
            smith(1, stack("anvil_saturnite"), cmp(base), tag("ingots/saturnite", 10));
            smith(1, stack("anvil_ferrouranium"), cmp(base), cmp("ingot_ferrouranium", 10));
            smith(1, stack("anvil_bismuth_bronze"), cmp(base), tag("ingots/bismuth_bronze", 10));
            smith(1, stack("anvil_arsenic_bronze"), cmp(base), tag("ingots/arsenic_bronze", 10));
            smith(1, stack("anvil_schrabidate"), cmp(base), tag("ingots/schrabidate", 10));
            smith(1, stack("anvil_dnt"), cmp(base), tag("ingots/dineutronium", 10));
            smith(1, stack("anvil_osmiridium"), cmp(base), tag("ingots/osmiridium", 10));
        }
        smith(1, stack("ingot_gunmetal"), tag("ingots/copper"), tag("ingots/aluminum"));
    }

    private static void registerConstruction() {
        // CE :142-154 plates (Press already stamps these; anvil is the CE second path)
        plate("ingots/iron", "plate_iron");
        plate("ingots/gold", "plate_gold");
        plate("ingots/titanium", "plate_titanium");
        plate("ingots/aluminum", "plate_aluminium");
        plate("ingots/steel", "plate_steel");
        plate("ingots/lead", "plate_lead");
        plate("ingots/copper", "plate_copper");
        plate("ingots/gunmetal", "plate_gunmetal");
        plate("ingots/weaponsteel", "plate_weaponsteel");
        plate("ingots/saturnite", "plate_saturnite");
        plate("ingots/dura_steel", "plate_dura_steel");
        plate("ingots/schrabidium", "plate_schrabidium");
        plate("ingots/cmb_steel", "plate_combine_steel");

        // CE :156-160 wire autogen — same Mats WIRE set Press uses
        for (NTMMaterial mat : Mats.orderedList) {
            if (!mat.getAutogen().contains(MaterialShapes.WIRE)) continue;
            Item wire = item(MaterialShapes.WIRE.buildRegistryName(mat));
            if (wire == Items.AIR) continue;
            construct(4, new ItemStack(wire, 8), tag("ingots/" + mat.getRegistryName()));
        }

        // CE :162-166 dust → gem
        construct(3, new ItemStack(Items.COAL), tag("dusts/coal"));
        construct(3, new ItemStack(Items.QUARTZ), tag("dusts/quartz"));
        construct(3, new ItemStack(Items.LAPIS_LAZULI), tag("dusts/lapis"));
        construct(3, new ItemStack(Items.DIAMOND), tag("dusts/diamond"));
        construct(3, new ItemStack(Items.EMERALD), tag("dusts/emerald"));

        registerConstructionRecipes();
    }

    /** CE {@code registerConstructionRecipes} :176-566 — machines / deco / coils / armor / fuel plates. */
    private static void registerConstructionRecipes() {
        // :182-188 deco cubes
        construct(1, stack("deco_aluminium", 4), tag("ingots/aluminum"));
        construct(1, stack("deco_beryllium", 4), tag("ingots/beryllium"));
        construct(1, stack("deco_lead", 4), tag("ingots/lead"));
        construct(1, stack("deco_red_copper", 4), tag("ingots/red_copper"));
        construct(1, stack("deco_steel", 4), tag("ingots/steel"));
        construct(1, stack("deco_titanium", 4), tag("ingots/titanium"));
        construct(1, stack("deco_tungsten", 4), tag("ingots/tungsten"));
        construct(1, stack("deco_asbestos", 4), cmp("ingot_asbestos"));

        // :205-217 coils / motors
        construct(1, stack("coil_copper_torus"), cmp("coil_copper", 2));
        construct(1, stack("coil_gold_torus"), cmp("coil_gold", 2));
        construct(1, stack("motor", 2), tag("plates/iron", 2), cmp("coil_copper"), cmp("coil_copper_torus"));
        Item goldDense = item(MaterialShapes.DENSEWIRE.buildRegistryName(Mats.MAT_GOLD));
        if (goldDense != Items.AIR) {
            construct(3, stack("motor_desh"), cmp("motor"), cmp("ingot_polymer", 2), tag("ingots/desh", 2),
                    new ComparableStack(goldDense));
        }

        // :219-225 blast furnace
        construct(1, stack("machine_blast_furnace"),
                new ComparableStack(Blocks.STONE_BRICKS, 4),
                cmp("ingot_firebrick", 32),
                tag("plates/copper", 8));

        // :237-243 assembly machine (cheap vacuum-tube path, not expensive-mode analog)
        construct(2, stack("machine_assembly_machine"),
                tag("ingots/steel", 8),
                tag("plates/copper", 4),
                cmp("motor", 2),
                cmp("circuit_vacuum_tube", 4));

        // :262-267 heater_firebox
        construct(2, stack("heater_firebox"),
                new ComparableStack(Blocks.FURNACE),
                tag("plates/steel", 8),
                tag("ingots/copper", 8));

        // :269-274 heater_oven
        construct(2, stack("heater_oven"),
                cmp("ingot_firebrick", 16),
                tag("plates/steel", 4),
                tag("ingots/copper", 8));

        // :276-281 ashpit
        construct(2, stack("machine_ashpit"),
                new ComparableStack(Blocks.STONE, 8),
                tag("plates/steel", 2),
                tag("ingots/iron", 4));

        // :283-289 heater_oilburner
        construct(2, stack("heater_oilburner"),
                cmp("tank_steel", 4),
                tag("ingots/titanium", 12),
                tag("ingots/copper", 8));

        // :291-298 heater_electric
        construct(3, stack("heater_electric"),
                cmp("ingot_polymer", 4),
                tag("ingots/copper", 8),
                tag("plates/steel", 8),
                cmp("coil_tungsten", 8),
                cmp("circuit_basic"));

        // :300-306 heater_heatex
        construct(3, stack("heater_heatex"),
                cmp("ingot_rubber", 4),
                tag("ingots/copper", 16),
                tag("plates/steel", 16));

        // :308-315 furnace_steel
        construct(2, stack("furnace_steel"),
                new ComparableStack(Blocks.STONE_BRICKS, 16),
                tag("ingots/iron", 4),
                tag("plates/steel", 16),
                tag("ingots/copper", 8),
                cmp("steel_grate", 16));

        // :317-323 furnace_combination
        construct(2, stack("furnace_combination"),
                new ComparableStack(Blocks.STONE_BRICKS, 8),
                new ComparableStack(Items.OAK_LOG, 16),
                tag("plates/copper", 2),
                new ComparableStack(Items.BRICK, 16));

        // :325-331 rotary furnace
        construct(2, stack("machine_rotary_furnace"),
                new ComparableStack(Blocks.STONE_BRICKS, 8),
                cmp("ingot_firebrick", 16),
                tag("ingots/iron", 4),
                tag("plates/copper", 8));

        // :333-340 stirling
        construct(2, stack("machine_stirling"),
                new ComparableStack(Items.OAK_PLANKS, 16),
                tag("plates/steel", 6),
                tag("ingots/copper", 8),
                cmp("coil_copper", 4),
                cmp("gear_bronze"));

        // :342-349 stirling steel
        construct(2, stack("machine_stirling_steel"),
                tag("plates/steel", 16),
                tag("ingots/beryllium", 6),
                tag("ingots/copper", 8),
                cmp("coil_gold", 16),
                cmp("gear_steel"));

        // :369-374 crucible
        construct(2, stack("machine_crucible"),
                cmp("ingot_firebrick", 20),
                tag("ingots/copper", 8),
                tag("plates/steel", 8));

        // :376-381 machine_boiler — block not registered. TODO(CE: AnvilRecipes.java:376-381)

        // :383-389 soldering
        construct(2, stack("machine_soldering_station"),
                tag("plates/steel", 2),
                cmp("coil_copper", 4),
                cmp("circuit_vacuum_tube", 2));

        // :391-397 arc welder
        construct(2, stack("machine_arc_welder"),
                tag("plates/steel", 4),
                tag("ingots/tungsten", 8),
                cmp("arc_electrode", 2));

        // :399-404 industrial boiler
        construct(3, stack("machine_industrial_boiler"),
                tag("plates/steel", 8),
                tag("ingots/copper", 8),
                cmp("ingot_polymer", 4));

        // :528-551 armor / desh / bismuth plates
        construct(3, stack("plate_desh", 4),
                tag("ingots/desh", 4),
                tag("dusts/polymer", 2),
                tag("ingots/dura_steel"));
        construct(4, stack("plate_bismuth"),
                cmp("nugget_bismuth", 2),
                tag("billets/uranium238", 2),
                tag("dusts/niobium"));
        construct(2, stack("plate_armor_titanium"),
                tag("plates/titanium", 2),
                tag("ingots/steel"));
        construct(3, stack("plate_armor_ajr", 2),
                tag("plates/iron", 6),
                tag("ingots/niobium"),
                cmp("plate_armor_titanium"));
        construct(4, stack("plate_armor_hev"),
                tag("plates/dura_steel", 4),
                cmp("plate_armor_titanium"),
                tag("wires/tungsten", 8));
        construct(4, stack("plate_armor_lunar"),
                tag("ingots/starmetal"),
                tag("wires/magnetized_tungsten", 8));

        // :560-566 fuel plates
        construct(4, stack("plate_fuel_u233"), cmp("ingot_u233"));
        construct(4, stack("plate_fuel_u235"), cmp("ingot_u235"));
        construct(4, stack("plate_fuel_mox"), cmp("ingot_mox_fuel"));
        construct(4, stack("plate_fuel_pu239"), cmp("ingot_pu239"));
        construct(4, stack("plate_fuel_sa326"), cmp("ingot_schrabidium"));
        construct(4, stack("plate_fuel_ra226be"), cmp("billet_ra226be"));
        construct(4, stack("plate_fuel_pu238be"), cmp("billet_pu238be"));

        // :508-519 barrels
        construct(3, stack("yellow_barrel"),
                cmp("tank_steel"),
                tag("plates/lead", 2),
                cmp("nuclear_waste", 10));
        construct(3, stack("vitrified_barrel"),
                cmp("tank_steel"),
                tag("plates/lead", 2),
                cmp("nuclear_waste_vitrified", 10));
    }

    private static void plate(String ingotTag, String out) {
        construct(3, stack(out), tag(ingotTag));
    }

    private static void smith(int tier, ItemStack out, AStack left, AStack right) {
        if (empty(out) || empty(left) || empty(right)) return;
        SMITHING.add(new AnvilSmithingRecipe(tier, out, left, right));
    }

    private static void construct(int tier, ItemStack out, AStack... in) {
        if (empty(out)) return;
        for (AStack stack : in) {
            if (empty(stack)) return;
        }
        CONSTRUCTION.add(new AnvilConstructionRecipe(in, new AnvilOutput(out)).setTier(tier));
    }

    private static boolean empty(ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.getItem() == Items.AIR;
    }

    private static boolean empty(AStack stack) {
        if (stack == null) return true;
        if (stack instanceof ComparableStack cmp) {
            return cmp.item == null || cmp.item == Items.AIR;
        }
        return false;
    }

    private static OreDictStack tag(String path) {
        return OreDictStack.ofCommonTag(path);
    }

    private static OreDictStack tag(String path, int n) {
        return OreDictStack.ofCommonTag(path, n);
    }

    private static ComparableStack cmp(String id) {
        return new ComparableStack(item(id));
    }

    private static ComparableStack cmp(String id, int n) {
        return new ComparableStack(item(id), n);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(String id) {
        return stack(id, 1);
    }

    private static ItemStack stack(String id, int n) {
        Item i = item(id);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i, n);
    }

    public static final class AnvilConstructionRecipe {
        public final List<AStack> input = new ArrayList<>();
        public final List<AnvilOutput> output = new ArrayList<>();
        public int tierLower;
        public int tierUpper = -1;
        public OverlayType overlay = OverlayType.NONE;

        public AnvilConstructionRecipe(AStack input, AnvilOutput output) {
            this.input.add(input);
            this.output.add(output);
            this.overlay = OverlayType.SMITHING;
        }

        public AnvilConstructionRecipe(AStack[] input, AnvilOutput output) {
            Collections.addAll(this.input, input);
            this.output.add(output);
            this.overlay = OverlayType.CONSTRUCTION;
        }

        public AnvilConstructionRecipe setTier(int tier) {
            this.tierLower = tier;
            return this;
        }

        public AnvilConstructionRecipe setTierRange(int lower, int upper) {
            this.tierLower = lower;
            this.tierUpper = upper;
            return this;
        }

        public boolean isTierValid(int tier) {
            if (this.tierUpper == -1) return tier >= this.tierLower;
            return tier >= this.tierLower && tier <= this.tierUpper;
        }

        public AnvilConstructionRecipe setOverlay(OverlayType overlay) {
            this.overlay = overlay;
            return this;
        }

        public ItemStack getDisplay() {
            if (overlay == OverlayType.RECYCLING) {
                for (AStack stack : input) {
                    if (stack instanceof ComparableStack cmp) return cmp.getStack();
                }
            }
            return output.isEmpty() ? ItemStack.EMPTY : output.get(0).stack.copy();
        }
    }

    public static final class AnvilOutput {
        public final ItemStack stack;
        public final float chance;

        public AnvilOutput(ItemStack stack) {
            this(stack, 1F);
        }

        public AnvilOutput(ItemStack stack, float chance) {
            this.stack = stack;
            this.chance = chance;
        }
    }

    public enum OverlayType {
        NONE,
        CONSTRUCTION,
        RECYCLING,
        SMITHING
    }
}
