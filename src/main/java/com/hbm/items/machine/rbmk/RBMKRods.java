package com.hbm.items.machine.rbmk;

import com.hbm.api.rbmk.IRBMKFluxReceiver.NType;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc;
import com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Every concrete RBMK fuel rod, ported verbatim (yield/stats/function/heat/melting point/tint
 * constants) from CE's {@code ModItems.java} lines ~2050-2305 (31 rods + the debug-only
 * {@code rbmk_fuel_test}), each pairing 1:1 with an already-registered
 * {@code MachineItems.registerRbmkPellet(...)} pellet id (Phase 1, {@code MachineItems.java} ~415-450)
 * via {@link ItemRBMKRod}'s {@code pelletId} string - decoupled from a direct field reference on
 * purpose (see {@link ItemRBMKRod}'s own javadoc on why the pellet is referenced by registry name,
 * not by a shared static field this class doesn't own).
 * <p>
 * CE's {@code rbmk_fuel_uzh} exists as an item but was never added to {@code RBMKFuelRecipes}'
 * pellet-recycling table either (a real CE quirk, not a porting slip) - preserved exactly: registered
 * here, but intentionally absent from {@link com.hbm.inventory.recipes.machine.rbmk.RBMKFuelRecipes}.
 */
public final class RBMKRods {

    private static final int TINT_URANIUM = 0x868D82;
    private static final int TINT_NEPTUNIUM = 0x757E73;
    private static final int TINT_PLUTONIUM = 0x656E6B;
    private static final int TINT_AMERICIUM = 0xA88A8F;
    private static final int TINT_THORIUM = 0x665448;
    private static final int TINT_ZIRCONIUM = 0xAAA36A;
    private static final int TINT_SCHRABIDIUM = 0x2D9A94;
    private static final int TINT_POLONIUM = 0x563A26;
    private static final int TINT_RADIUM = 0xB3B6AD;
    private static final int TINT_AUSTRALIUM = 0xFFEE00;
    private static final int TINT_FLASHGOLD = 0xDC9613;
    private static final int TINT_FLASHLEAD = 0x7B7B87;
    private static final int TINT_BALEFIRE = 0xB2FF1B;
    private static final int TINT_DRX = 0xD77276;

    public static DeferredItem<Item> UEU, MEU, HEU233, HEU235, UZH, THMEU, LEP, MEP, HEP239, HEP241,
            LEA, MEA, HEA241, HEA242, MEN, HEN, MOX, LES, MES, HES, LEAUS, HEAUS, RA226BE, PO210BE,
            PU238BE, BALEFIRE_GOLD, FLASHLEAD, ZFB_BISMUTH, ZFB_PU241, ZFB_AM_MIX, BALEFIRE, DRX, TEST;

    /**
     * Rod-registry-name -> pellet-registry-name, populated by {@link #rod}. The real
     * {@code com.hbm.items.machine.ItemRBMKRod} (landed concurrently by the sibling
     * {@code rbmk_core_logic} package - see this class's own javadoc) takes an
     * {@code ItemRBMKPellet} object reference at construction time, which this registry does not
     * have a static handle to (Phase 1's {@code MachineItems.registerRbmkPellet} never exposed one -
     * see this class's javadoc); {@link ItemRBMKRod#pellet} is therefore left {@code null} here, and
     * {@link com.hbm.inventory.recipes.machine.rbmk.RBMKFuelRecipes} resolves the pellet by registry
     * name through this map instead, at actual recycling time (always safe - long after all
     * registration is done).
     */
    private static final Map<String, String> PELLET_IDS = new HashMap<>();

    private RBMKRods() {
    }

    /** @return the pellet registry id (namespace-less) a given rod registry name recycles into, or {@code null}. */
    public static String getPelletId(String rodRegistryName) {
        return PELLET_IDS.get(rodRegistryName);
    }

    public static void registerAll() {
        UEU = rod("rbmk_fuel_ueu", "rbmk_pellet_ueu", "Unenriched Uranium", r -> r
                .setYield(100_000_000D).setStats(15).setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(0.65).setMeltingPoint(2865).setTint(TINT_URANIUM));

        MEU = rod("rbmk_fuel_meu", "rbmk_pellet_meu", "Medium Enriched Uranium-235", r -> r
                .setYield(100_000_000D).setStats(20).setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(0.65).setMeltingPoint(2865).setTint(TINT_URANIUM));

        HEU233 = rod("rbmk_fuel_heu233", "rbmk_pellet_heu233", "Highly Enriched Uranium-233", r -> r
                .setYield(100_000_000D).setStats(27.5D).setFunction(EnumBurnFunc.LINEAR)
                .setHeat(1.25D).setMeltingPoint(2865).setTint(TINT_URANIUM));

        HEU235 = rod("rbmk_fuel_heu235", "rbmk_pellet_heu235", "Highly Enriched Uranium-235", r -> r
                .setYield(100_000_000D).setStats(50).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setMeltingPoint(2865).setTint(TINT_URANIUM));

        UZH = rod("rbmk_fuel_uzh", "rbmk_pellet_uzh", "Uranium Zirconium Hydride", r -> r
                .setYield(50_000_000D).setStats(30).setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.GENTLE_SLOPE).setHeat(0.75).setHeatCoeff(1_000D, 500D)
                .setDiffusion(0.1D).setMeltingPoint(1845).setTint(0x7077AF));

        THMEU = rod("rbmk_fuel_thmeu", "rbmk_pellet_thmeu", "Thorium with MEU Driver Fuel", r -> r
                .setYield(100_000_000D).setStats(20).setFunction(EnumBurnFunc.PLATEU)
                .setDepletionFunction(EnumDepleteFunc.BOOSTED_SLOPE).setHeat(0.65D).setMeltingPoint(3350).setTint(TINT_THORIUM));

        LEP = rod("rbmk_fuel_lep", "rbmk_pellet_lep", "Low Enriched Plutonium-239", r -> r
                .setYield(100_000_000D).setStats(35).setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(0.75D).setMeltingPoint(2744).setTint(TINT_PLUTONIUM));

        MEP = rod("rbmk_fuel_mep", "rbmk_pellet_mep", "Medium Enriched Plutonium-239", r -> r
                .setYield(100_000_000D).setStats(35).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setMeltingPoint(2744).setTint(TINT_PLUTONIUM));

        HEP239 = rod("rbmk_fuel_hep", "rbmk_pellet_hep239", "Highly Enriched Plutonium-239", r -> r
                .setYield(100_000_000D).setStats(30).setFunction(EnumBurnFunc.LINEAR)
                .setHeat(1.25D).setMeltingPoint(2744).setTint(TINT_PLUTONIUM));

        HEP241 = rod("rbmk_fuel_hep241", "rbmk_pellet_hep241", "Highly Enriched Plutonium-241", r -> r
                .setYield(100_000_000D).setStats(40).setFunction(EnumBurnFunc.LINEAR)
                .setHeat(1.75D).setMeltingPoint(2744).setTint(TINT_PLUTONIUM));

        LEA = rod("rbmk_fuel_lea", "rbmk_pellet_lea", "Low Enriched Americium-242", r -> r
                .setYield(100_000_000D).setStats(60, 10).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(1.5D).setMeltingPoint(2386).setTint(TINT_AMERICIUM));

        MEA = rod("rbmk_fuel_mea", "rbmk_pellet_mea", "Medium Enriched Americium-242", r -> r
                .setYield(100_000_000D).setStats(35D, 20).setFunction(EnumBurnFunc.ARCH)
                .setHeat(1.75D).setMeltingPoint(2386).setTint(TINT_AMERICIUM));

        HEA241 = rod("rbmk_fuel_hea241", "rbmk_pellet_hea241", "Highly Enriched Americium-241", r -> r
                .setYield(100_000_000D).setStats(65, 15).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setHeat(1.85D).setMeltingPoint(2386).setNeutronTypes(NType.FAST, NType.FAST).setTint(TINT_AMERICIUM));

        HEA242 = rod("rbmk_fuel_hea242", "rbmk_pellet_hea242", "Highly Enriched Americium-242", r -> r
                .setYield(100_000_000D).setStats(45).setFunction(EnumBurnFunc.LINEAR)
                .setHeat(2D).setMeltingPoint(2386).setTint(TINT_AMERICIUM));

        MEN = rod("rbmk_fuel_men", "rbmk_pellet_men", "Medium Enriched Neptunium-237", r -> r
                .setYield(100_000_000D).setStats(30).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(0.75).setMeltingPoint(2800)
                .setNeutronTypes(NType.ANY, NType.FAST).setTint(TINT_NEPTUNIUM));

        HEN = rod("rbmk_fuel_hen", "rbmk_pellet_hen", "Highly Enriched Neptunium-237", r -> r
                .setYield(100_000_000D).setStats(40).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setMeltingPoint(2800).setNeutronTypes(NType.FAST, NType.FAST).setTint(TINT_NEPTUNIUM));

        MOX = rod("rbmk_fuel_mox", "rbmk_pellet_mox", "Mixed LEU & LEP Oxide", r -> r
                .setYield(100_000_000D).setStats(40).setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setMeltingPoint(2815).setTint(TINT_URANIUM));

        LES = rod("rbmk_fuel_les", "rbmk_pellet_les", "Low Enriched Schrabidium-326", r -> r
                .setYield(100_000_000D).setStats(50).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setHeat(1.25D).setMeltingPoint(2500).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(TINT_SCHRABIDIUM));

        MES = rod("rbmk_fuel_mes", "rbmk_pellet_mes", "Medium Enriched Schrabidium-326", r -> r
                .setYield(100_000_000D).setStats(75D).setFunction(EnumBurnFunc.ARCH)
                .setHeat(1.5D).setMeltingPoint(2750).setTint(TINT_SCHRABIDIUM));

        HES = rod("rbmk_fuel_hes", "rbmk_pellet_hes", "Highly Enriched Schrabidium-326", r -> r
                .setYield(100_000_000D).setStats(90).setFunction(EnumBurnFunc.LINEAR)
                .setDepletionFunction(EnumDepleteFunc.LINEAR).setHeat(1.75D).setMeltingPoint(3000).setTint(TINT_SCHRABIDIUM));

        LEAUS = rod("rbmk_fuel_leaus", "rbmk_pellet_leaus", "Low Enriched Australium (Tasmanite)", r -> r
                .setYield(100_000_000D).setStats(30).setFunction(EnumBurnFunc.SIGMOID)
                .setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(0.05D, 50D).setHeat(1.5D)
                .setMeltingPoint(7029).setTint(TINT_AUSTRALIUM));

        HEAUS = rod("rbmk_fuel_heaus", "rbmk_pellet_heaus", "Highly Enriched Australium (Ayerite)", r -> r
                .setYield(100_000_000D).setStats(35).setFunction(EnumBurnFunc.LINEAR)
                .setXenon(0.05D, 50D).setHeat(1.5D).setMeltingPoint(5211).setTint(TINT_AUSTRALIUM));

        RA226BE = rod("rbmk_fuel_ra226be", "rbmk_pellet_ra226be", "Radium-226 & Beryllium Neutron Source", r -> r
                .setYield(100_000_000D).setStats(0D, 20).setFunction(EnumBurnFunc.PASSIVE)
                .setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(0.0D, 50D).setHeat(0.035D)
                .setDiffusion(0.5D).setMeltingPoint(700).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(TINT_RADIUM));

        PO210BE = rod("rbmk_fuel_po210be", "rbmk_pellet_po210be", "Polonium-210 & Beryllium Neutron Source", r -> r
                .setYield(25_000_000D).setStats(0D, 50).setFunction(EnumBurnFunc.PASSIVE)
                .setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(0.0D, 50D).setHeat(0.1D)
                .setDiffusion(0.05D).setMeltingPoint(1287).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(TINT_POLONIUM));

        PU238BE = rod("rbmk_fuel_pu238be", "rbmk_pellet_pu238be", "Plutonium-238 & Beryllium Neutron Source", r -> r
                .setYield(50_000_000D).setStats(40, 40).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setHeat(0.1D).setDiffusion(0.05D).setMeltingPoint(1287).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(TINT_PLUTONIUM));

        BALEFIRE_GOLD = rod("rbmk_fuel_balefire_gold", "rbmk_pellet_balefire_gold", "Antihydrogen in a Magnetized Gold-198 Lattice", r -> r
                .setYield(100_000_000D).setStats(50, 10).setFunction(EnumBurnFunc.ARCH)
                .setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(0.0D, 50D).setMeltingPoint(2000).setTint(TINT_FLASHGOLD));

        FLASHLEAD = rod("rbmk_fuel_flashlead", "rbmk_pellet_flashlead", "Antihydrogen confined by a Magnetized Gold-198 & Lead-209 Lattice", r -> r
                .setYield(250_000_000D).setStats(40, 50).setFunction(EnumBurnFunc.ARCH)
                .setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(0.0D, 50D).setMeltingPoint(2050).setTint(TINT_FLASHLEAD));

        ZFB_BISMUTH = rod("rbmk_fuel_zfb_bismuth", "rbmk_pellet_zfb_bismuth", "Zirconium Fast Breeder - LEU/HEP-241 -> Bi", r -> r
                .setYield(50_000_000D).setStats(20).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setHeat(1.75D).setMeltingPoint(2744).setTint(TINT_ZIRCONIUM));

        ZFB_PU241 = rod("rbmk_fuel_zfb_pu241", "rbmk_pellet_zfb_pu241", "Zirconium Fast Breeder - HEU-235/HEP-240 -> Pu241", r -> r
                .setYield(50_000_000D).setStats(20).setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setMeltingPoint(2865).setTint(TINT_ZIRCONIUM));

        ZFB_AM_MIX = rod("rbmk_fuel_zfb_am_mix", "rbmk_pellet_zfb_am_mix", "Zirconium Fast Breeder - HEP-241 -> HEA", r -> r
                .setYield(50_000_000D).setStats(20).setFunction(EnumBurnFunc.LINEAR)
                .setHeat(1.75D).setMeltingPoint(2744).setTint(TINT_ZIRCONIUM));

        BALEFIRE = rod("rbmk_fuel_balefire", "rbmk_pellet_balefire", "Draconic Flames", r -> r
                .setYield(100_000_000D).setStats(100, 35).setFunction(EnumBurnFunc.LINEAR)
                .setXenon(0.0D, 50D).setHeat(3D).setMeltingPoint(3652).setTint(TINT_BALEFIRE));

        DRX = rod("rbmk_fuel_drx", "rbmk_pellet_drx", "§kcan't you hear, can't you hear the thunder?", r -> r
                .setYield(10_000_000D).setStats(1000, 10).setFunction(EnumBurnFunc.QUADRATIC)
                .setHeat(0.1D).setMeltingPoint(100_000).setTint(TINT_DRX));

        // Debug-only fuel with no pellet (CE: new ItemRBMKRod("THE VOICES", "rbmk_fuel_test"), no
        // recycling recipe). Kept out of the creative tab - it exists purely so world-edited/legacy
        // saves referencing it don't crash on load.
        TEST = ModItems.ITEMS.register("rbmk_fuel_test", () -> (ItemRBMKRod) new ItemRBMKRod("THE VOICES", new Item.Properties())
                .setYield(1_000_000D).setStats(100).setFunction(EnumBurnFunc.EXPERIMENTAL)
                .setHeat(1.0D).setMeltingPoint(100_000));
    }

    private interface RodConfigurer {
        ItemRBMKRod configure(ItemRBMKRod rod);
    }

    private static DeferredItem<Item> rod(String name, String pelletId, String fullName, RodConfigurer configurer) {
        PELLET_IDS.put(name, pelletId);
        DeferredItem<Item> item = ModItems.ITEMS.register(name,
                () -> configurer.configure(new ItemRBMKRod(fullName, new Item.Properties())));
        CreativeTabContents.add(ModCreativeTabs.CONTROL, item);
        return item;
    }
}
