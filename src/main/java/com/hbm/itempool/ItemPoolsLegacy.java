package com.hbm.itempool;

import com.hbm.blocks.machine.PWRBlocks;
import com.hbm.blocks.network.energy.EnergyNetworkBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.armor.PoweredArmorItems;
import com.hbm.items.gear.GearItems;
import com.hbm.items.gear.SpecialArmorItems;
import com.hbm.items.machine.MachineItems;
import com.hbm.items.special.ItemCell;
import com.hbm.items.special.SpecialItems;
import com.hbm.items.tool.ItemCanister;
import com.hbm.items.tool.ToolItems;
import com.hbm.items.weapon.sedna.content.GunHeavyItems;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import com.hbm.items.weapon.sedna.content.XFactory12ga;
import com.hbm.items.weapon.sedna.content.XFactory357;
import com.hbm.items.weapon.sedna.content.XFactory40mm;
import com.hbm.items.weapon.sedna.content.XFactoryRocket;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.itempool.ItemPoolsLegacy} (238 lines, read in full from
 * {@code upstream/hbm-ce} via Warfactory-Official/Hbm-s-Nuclear-Tech-CE). The seven loot pools
 * consumed by CE's schematic2java structures ({@code Antenna}/{@code Bunker}/{@code Radio01}/
 * vertibird {@code .nbt}/{@code Spaceship}) and {@code SiloComponent}. Pool <i>name</i> strings are
 * CE's own {@code "POOL_*"} literals — {@code BlockWandLoot} / future {@code .nbt} paste resolve
 * those strings, so they must stay verbatim (unlike {@link ItemPoolsSatellite}'s sat-miner re-key).
 * <p>
 * Meta-discriminated CE entries ({@code circuit}+{@code EnumCircuitType}, {@code casing}+
 * {@code EnumCasingType}, {@code ammo_standard}+{@code EnumAmmo}, {@code battery_pack},
 * {@code bomb_caller} damage, {@code rod}/{@code rod_zirnox} breeding types) are remapped to this
 * port's discrete items where those items exist, or skipped with an inline comment. Weights/min/max
 * are CE's exact numbers. {@code ItemGrenadeUniversal.make(NUKE/...)} is skipped — grenade-shell
 * assembly is not a registered item here.
 * <p>
 * Must run after every item {@code RegisterEvent} (same as {@link ItemPoolsSatellite#init()}).
 */
public final class ItemPoolsLegacy {

    public static final String POOL_GENERIC = "POOL_GENERIC";
    public static final String POOL_ANTENNA = "POOL_ANTENNA";
    public static final String POOL_EXPENSIVE = "POOL_EXPENSIVE";
    public static final String POOL_NUKE_TRASH = "POOL_NUKE_TRASH";
    public static final String POOL_NUKE_MISC = "POOL_NUKE_MISC";
    public static final String POOL_VERTIBIRD = "POOL_VERTIBIRD";
    public static final String POOL_SPACESHIP = "POOL_SPACESHIP";

    private ItemPoolsLegacy() {
    }

    public static void init() {
        ItemPool generic = new ItemPool(POOL_GENERIC);
        generic.pool.addAll(List.of(
                ItemPool.entry(Items.BREAD, 1, 5, 8),
                ItemPool.entry(Items.IRON_INGOT, 2, 6, 10),
                ItemPool.entry(IngotNuggetItems.INGOT_STEEL.get(), 2, 5, 7),
                ItemPool.entry(IngotNuggetItems.INGOT_BERYLLIUM.get(), 1, 2, 4),
                ItemPool.entry(IngotNuggetItems.INGOT_TITANIUM.get(), 1, 1, 3),
                ItemPool.entry(GunPistolItems.GUN_LIGHT_REVOLVER.get(), 1, 1, 3),
                ItemPool.entry(XFactory357.ITEM_M357_SP, 2, 6, 4),
                ItemPool.entry(XFactory12ga.ITEM_G12_BP, 3, 6, 3),
                ItemPool.entry(XFactory40mm.ITEM_G26_FLARE_SUPPLY, 1, 1, 1),
                ItemPool.entry(ToolItems.GUN_KIT_1.get(), 1, 3, 4),
                ItemPool.entry(GunShotgunItems.GUN_MARESLEG.get(), 1, 1, 1),
                ItemPool.entry(GearItems.BOTTLE_OPENER.get(), 1, 1, 2),
                ItemPool.entry(SpecialItems.STEALTH_BOY.get(), 1, 1, 1),
                ItemPool.entry(SpecialArmorItems.GAS_MASK_M65.get(), 1, 1, 2)
        ));
        addHbm(generic, "twinkie", 1, 3, 6);
        addHbm(generic, "circuit_vacuum_tube", 1, 1, 5);
        addHbm(generic, "casing_small", 4, 10, 3);
        addHbm(generic, "casing_shotshell", 4, 10, 3);
        addHbm(generic, "cordite", 4, 6, 5);
        addHbm(generic, "battery_redstone", 1, 1, 1);
        addHbm(generic, "scrap", 1, 3, 10);
        addHbm(generic, "dust", 2, 4, 9);
        addHbm(generic, "bottle_nuka", 1, 3, 4);
        addHbm(generic, "bottle_cherry", 1, 1, 2);
        addHbm(generic, "cap_nuka", 1, 15, 7);
        addHbm(generic, "gas_mask_filter", 1, 1, 3);
        addHbm(generic, "blueprint_folder", 1, 1, 1);
        addHbm(generic, "coin_token", 1, 1, 2);
        addStack(generic, dieselCanister(), 1, 2, 2);
        addStack(generic, biofuelCanister(), 1, 2, 3);

        ItemPool antenna = new ItemPool(POOL_ANTENNA);
        antenna.pool.addAll(List.of(
                ItemPool.entry(IngotNuggetItems.INGOT_STEEL.get(), 1, 2, 7),
                ItemPool.entry(IngotNuggetItems.INGOT_RED_COPPER.get(), 1, 1, 4),
                ItemPool.entry(IngotNuggetItems.INGOT_TITANIUM.get(), 1, 3, 5),
                ItemPool.entry(BilletPowderItems.POWDER_IODINE.get(), 1, 1, 1),
                ItemPool.entry(BilletPowderItems.POWDER_BROMINE.get(), 1, 1, 1),
                ItemPool.entry(GearItems.BOTTLE_OPENER.get(), 1, 1, 2),
                ItemPool.entry(SpecialItems.STEALTH_BOY.get(), 1, 1, 1)
        ));
        addHbm(antenna, "twinkie", 1, 3, 4);
        addHbm(antenna, "wire_fine_mingrade", 2, 3, 7);
        addHbm(antenna, "circuit_vacuum_tube", 1, 1, 4);
        addHbm(antenna, "circuit_capacitor", 1, 1, 2);
        addHbm(antenna, "battery_redstone", 1, 1, 1);
        addHbm(antenna, "steel_poles", 1, 4, 8);
        addHbm(antenna, "steel_scaffold", 1, 3, 8);
        addHbm(antenna, "pole_top", 1, 1, 4);
        addHbm(antenna, "pole_satellite_receiver", 1, 1, 7);
        addHbm(antenna, "scrap", 1, 3, 10);
        addHbm(antenna, "dust", 2, 4, 9);
        addHbm(antenna, "bottle_nuka", 1, 3, 4);
        addHbm(antenna, "bottle_cherry", 1, 1, 2);
        addHbm(antenna, "cap_nuka", 1, 15, 7);
        addHbm(antenna, "bomb_caller", 1, 1, 1);
        addHbm(antenna, "gas_mask_filter", 1, 1, 2);

        ItemPool expensive = new ItemPool(POOL_EXPENSIVE);
        expensive.pool.addAll(List.of(
                ItemPool.entry(ToolItems.CHLORINE_PINWHEEL.get(), 1, 1, 1),
                ItemPool.entry(ToolItems.GUN_KIT_1.get(), 1, 3, 6),
                ItemPool.entry(ToolItems.GUN_KIT_2.get(), 1, 2, 3),
                ItemPool.entry(GunHeavyItems.GUN_PANZERSCHRECK.get(), 1, 1, 4),
                ItemPool.entry(XFactoryRocket.ITEM_ROCKET_HE, 1, 4, 5),
                ItemPool.entry(XFactory40mm.ITEM_G26_FLARE_SUPPLY, 1, 1, 5),
                ItemPool.entry(XFactory40mm.ITEM_G26_FLARE_WEAPON, 1, 1, 3),
                ItemPool.entry(SpecialItems.STEALTH_BOY.get(), 1, 1, 2),
                ItemPool.entry(SpecialArmorItems.GAS_MASK_M65.get(), 1, 1, 5),
                ItemPool.entry(GunShotgunItems.GUN_DOUBLE_BARREL.get(), 1, 1, 1)
        ));
        addHbm(expensive, "circuit_vacuum_tube", 1, 1, 4);
        addHbm(expensive, "circuit_analog", 1, 1, 3);
        addHbm(expensive, "circuit_chip", 1, 1, 2);
        addHbm(expensive, "battery_lithium", 1, 1, 1);
        addHbm(expensive, "syringe_awesome", 1, 1, 1);
        addHbm(expensive, "fusion_core", 1, 1, 4);
        addHbm(expensive, "bottle_nuka", 1, 3, 6);
        addHbm(expensive, "bottle_quantum", 1, 1, 3);
        addHbm(expensive, "red_barrel", 1, 1, 6);
        addHbm(expensive, "bomb_caller", 1, 1, 2);
        addHbm(expensive, "gas_mask_filter", 1, 1, 4);
        addHbm(expensive, "journal_pip", 1, 1, 1);
        addHbm(expensive, "journal_bj", 1, 1, 1);
        addHbm(expensive, "blueprint_folder", 1, 1, 1);
        addStack(expensive, dieselCanister(), 1, 2, 2);
        addStack(expensive, biofuelCanister(), 1, 2, 3);
        // SKIPPED: ItemGrenadeUniversal.make(NUKE/FRAG) — no assembled grenade item in this port
        // SKIPPED: CE launch_code_piece (commented TODO in CE ItemPoolsLegacy.java:136)

        ItemPool nukeTrash = new ItemPool(POOL_NUKE_TRASH);
        nukeTrash.pool.addAll(List.of(
                ItemPool.entry(IngotNuggetItems.NUGGET_U238.get(), 3, 12, 5),
                ItemPool.entry(IngotNuggetItems.NUGGET_PU240.get(), 3, 8, 5),
                ItemPool.entry(IngotNuggetItems.NUGGET_NEPTUNIUM.get(), 1, 4, 3),
                ItemPool.entry(SpecialArmorItems.GAS_MASK_M65.get(), 1, 1, 5),
                ItemPool.entry(SpecialItems.HAZMAT_KIT.get(), 1, 1, 1)
        ));
        addHbm(nukeTrash, "rod_u238", 1, 1, 3);
        addHbm(nukeTrash, "rod_dual_u238", 1, 1, 3);
        addHbm(nukeTrash, "rod_quad_u238", 1, 1, 3);
        addHbm(nukeTrash, "bottle_quantum", 1, 1, 1);
        addHbm(nukeTrash, "gas_mask_filter", 1, 1, 5);
        addHbm(nukeTrash, "yellow_barrel", 1, 1, 2);

        ItemPool nukeMisc = new ItemPool(POOL_NUKE_MISC);
        nukeMisc.pool.addAll(List.of(
                ItemPool.entry(IngotNuggetItems.NUGGET_U235.get(), 3, 12, 5),
                ItemPool.entry(IngotNuggetItems.NUGGET_PU238.get(), 3, 12, 5),
                ItemPool.entry(IngotNuggetItems.NUGGET_RA226.get(), 3, 6, 5),
                ItemPool.entry(MachineItems.PELLET_RTG.get(), 1, 1, 3),
                ItemPool.entry(BilletPowderItems.POWDER_THORIUM.get(), 1, 1, 1),
                ItemPool.entry(BilletPowderItems.POWDER_NEPTUNIUM.get(), 1, 1, 1),
                ItemPool.entry(BilletPowderItems.POWDER_STRONTIUM.get(), 1, 1, 1),
                ItemPool.entry(BilletPowderItems.POWDER_COBALT.get(), 1, 1, 1),
                ItemPool.entry(SpecialArmorItems.GAS_MASK_M65.get(), 1, 1, 5),
                ItemPool.entry(SpecialItems.HAZMAT_KIT.get(), 1, 1, 2)
        ));
        addHbm(nukeMisc, "rod_u235", 1, 1, 3);
        addHbm(nukeMisc, "rod_dual_u235", 1, 1, 3);
        addHbm(nukeMisc, "rod_quad_u235", 1, 1, 3);
        addHbm(nukeMisc, "rod_zirnox_uranium_fuel", 1, 1, 4);
        addHbm(nukeMisc, "rod_zirnox_mox_fuel", 1, 1, 4);
        addHbm(nukeMisc, "rod_zirnox_lithium", 1, 1, 3);
        addHbm(nukeMisc, "rod_zirnox_thorium_fuel", 1, 1, 3);
        addHbm(nukeMisc, "rod_dual_thf", 1, 1, 3);
        addHbm(nukeMisc, "rod_zirnox_tritium", 1, 1, 1);
        addHbm(nukeMisc, "rod_zirnox_u233_fuel", 1, 1, 1);
        addHbm(nukeMisc, "rod_zirnox_u235_fuel", 1, 1, 1);
        addHbm(nukeMisc, "bottle_quantum", 1, 1, 1);
        addHbm(nukeMisc, "gas_mask_filter", 1, 1, 5);
        addHbm(nukeMisc, "yellow_barrel", 1, 1, 3);

        ItemPool vertibird = new ItemPool(POOL_VERTIBIRD);
        vertibird.pool.addAll(List.of(
                ItemPool.entry(PoweredArmorItems.T51_HELMET.get(), 1, 1, 15),
                ItemPool.entry(PoweredArmorItems.T51_PLATE.get(), 1, 1, 15),
                ItemPool.entry(PoweredArmorItems.T51_LEGS.get(), 1, 1, 15),
                ItemPool.entry(PoweredArmorItems.T51_BOOTS.get(), 1, 1, 15),
                ItemPool.entry(SpecialItems.T45_KIT.get(), 1, 1, 3),
                ItemPool.entry(GunPistolItems.GUN_LIGHT_REVOLVER.get(), 1, 1, 4),
                ItemPool.entry(ToolItems.GUN_KIT_1.get(), 2, 3, 4),
                ItemPool.entry(XFactory357.ITEM_M357_FMJ, 1, 24, 4),
                ItemPool.entry(XFactory40mm.ITEM_G26_FLARE_WEAPON, 1, 1, 5),
                ItemPool.entry(BilletPowderItems.BILLET_URANIUM_FUEL.get(), 1, 1, 2),
                ItemPool.entry(IngotNuggetItems.INGOT_URANIUM_FUEL.get(), 1, 1, 2),
                ItemPool.entry(SpecialItems.STEALTH_BOY.get(), 1, 1, 7),
                ItemPool.entry(SpecialArmorItems.GAS_MASK_M65.get(), 1, 1, 5)
        ));
        addHbm(vertibird, "fusion_core", 1, 1, 10);
        vertibird.pool.add(ItemPool.entry(XFactory40mm.ITEM_G40_HE, 1, 6, 3));
        addHbm(vertibird, "rod_u235", 1, 1, 2);
        addHbm(vertibird, "bottle_nuka", 1, 3, 6);
        addHbm(vertibird, "bottle_quantum", 1, 1, 3);
        addHbm(vertibird, "gas_mask_filter", 1, 1, 5);
        addHbm(vertibird, "bomb_caller", 1, 1, 1);
        // SKIPPED: ItemGrenadeUniversal.make(NUKE) — same as POOL_EXPENSIVE

        ItemPool spaceship = new ItemPool(POOL_SPACESHIP);
        spaceship.pool.addAll(List.of(
                ItemPool.entry(BilletPowderItems.POWDER_NEODYMIUM.get(), 1, 1, 1),
                ItemPool.entry(BilletPowderItems.POWDER_NIOBIUM.get(), 1, 1, 1),
                ItemPool.entry(PWRBlocks.PWR_FUELROD.get(), 1, 2, 5),
                ItemPool.entry(EnergyNetworkBlocks.RED_CABLE.get(), 8, 16, 5)
        ));
        addHbm(spaceship, "battery_lead", 1, 1, 2);
        addHbm(spaceship, "coil_copper", 2, 16, 5);
        addHbm(spaceship, "wire_fine_mingrade", 8, 32, 5);
        addHbm(spaceship, "wire_dense_mingrade", 2, 4, 5);
        addHbm(spaceship, "wire_dense_gold", 1, 3, 5);
        addHbm(spaceship, "block_tungsten", 3, 8, 5);
        addHbm(spaceship, "red_wire_coated", 4, 8, 5);
        addStack(spaceship, filledCell(Fluids.DEUTERIUM), 1, 8, 5);
        addStack(spaceship, filledCell(Fluids.TRITIUM), 1, 8, 5);
        addStack(spaceship, filledCell(Fluids.AMAT), 1, 1, 1);
    }

    private static void addHbm(ItemPool pool, String path, int min, int max, int weight) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path)).orElse(null);
        if (item == null || item == Items.AIR) {
            MainRegistry.logger.debug("ItemPoolsLegacy: skip missing hbm:{}", path);
            return;
        }
        pool.pool.add(ItemPool.entry(item, min, max, weight));
    }

    private static void addStack(ItemPool pool, ItemStack stack, int min, int max, int weight) {
        if (stack == null || stack.isEmpty()) return;
        pool.pool.add(ItemPool.entry(stack, min, max, weight));
    }

    private static ItemStack dieselCanister() {
        return filledCanister(Fluids.DIESEL);
    }

    private static ItemStack biofuelCanister() {
        return filledCanister(Fluids.BIOFUEL);
    }

    private static ItemStack filledCanister(com.hbm.inventory.fluid.FluidType fluid) {
        ItemStack stack = new ItemStack(ToolItems.CANISTER_FUEL.get());
        if (stack.getItem() instanceof ItemCanister canister) {
            canister.tryFill(fluid, 1000, stack);
        }
        return stack;
    }

    private static ItemStack filledCell(com.hbm.inventory.fluid.FluidType fluid) {
        return ItemCell.getFullCell(SpecialItems.CELL.get(), fluid);
    }
}
