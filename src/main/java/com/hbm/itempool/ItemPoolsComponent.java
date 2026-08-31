package com.hbm.itempool;

import net.minecraft.world.item.Items;

/**
 * Port of CE's {@code com.hbm.itempool.ItemPoolsComponent} (Warfactory-Official/Hbm-s-Nuclear-Tech-CE
 * {@code ItemPoolsComponent.java}, SHA {@code e5ed5ec}). Nine {@code POOL_*} names consumed by modern
 * {@code .nbt} wand-loot / bunker / silo / vault / meteor-safe chests. Names stay verbatim.
 * <p>
 * Meta-discriminated CE entries remapped to discrete port ids (circuits, battery packs, stamp_book
 * PRINTING1–8, deco_computer IBM_300PL, blueprint_folder base/discover). Missing ids skip.
 * Weights/min/max are CE's exact numbers. Must run after item {@code RegisterEvent}.
 */
public final class ItemPoolsComponent {

    public static final String POOL_MACHINE_PARTS = "POOL_MACHINE_PARTS";
    public static final String POOL_NUKE_FUEL = "POOL_NUKE_FUEL";
    public static final String POOL_SILO = "POOL_SILO";
    public static final String POOL_OFFICE_TRASH = "POOL_OFFICE_TRASH";
    public static final String POOL_FILING_CABINET = "POOL_FILING_CABINET";
    public static final String POOL_SOLID_FUEL = "POOL_SOLID_FUEL";
    public static final String POOL_VAULT_LAB = "POOL_VAULT_LAB";
    public static final String POOL_VAULT_LOCKERS = "POOL_VAULT_LOCKERS";
    public static final String POOL_METEOR_SAFE = "POOL_METEOR_SAFE";

    private ItemPoolsComponent() {
    }

    public static void init() {
        ItemPool parts = new ItemPool(POOL_MACHINE_PARTS);
        ItemPoolLookups.add(parts, "plate_steel", 1, 5, 5);
        // SKIPPED: shell MAT_STEEL — material-meta shell not a discrete item
        ItemPoolLookups.add(parts, "plate_polymer", 1, 6, 5);
        // SKIPPED: bolt MAT_STEEL / MAT_TUNGSTEN — same meta-material split
        ItemPoolLookups.add(parts, "coil_tungsten", 1, 2, 5);
        ItemPoolLookups.add(parts, "motor", 1, 2, 4);
        ItemPoolLookups.add(parts, "coil_copper", 1, 3, 4);
        ItemPoolLookups.add(parts, "coil_copper_torus", 1, 2, 3);
        ItemPoolLookups.add(parts, "wire_fine_mingrade", 1, 8, 5);
        ItemPoolLookups.add(parts, "piston_selenium", 1, 1, 3);
        ItemPoolLookups.add(parts, "battery_lead_pack", 1, 1, 3);
        ItemPoolLookups.add(parts, "circuit_vacuum_tube", 1, 2, 4);
        ItemPoolLookups.add(parts, "circuit_pcb", 1, 3, 5);
        ItemPoolLookups.add(parts, "circuit_capacitor", 1, 1, 3);
        ItemPoolLookups.add(parts, "blade_titanium", 1, 8, 1);
        ItemPoolLookups.add(parts, "blueprint_folder_base", 1, 1, 1);

        ItemPool fuel = new ItemPool(POOL_NUKE_FUEL);
        ItemPoolLookups.add(fuel, "billet_uranium", 1, 4, 4);
        ItemPoolLookups.add(fuel, "billet_th232", 1, 3, 3);
        ItemPoolLookups.add(fuel, "billet_uranium_fuel", 1, 3, 5);
        ItemPoolLookups.add(fuel, "billet_mox_fuel", 1, 3, 5);
        ItemPoolLookups.add(fuel, "billet_thorium_fuel", 1, 3, 3);
        ItemPoolLookups.add(fuel, "billet_ra226be", 1, 2, 2);
        ItemPoolLookups.add(fuel, "billet_beryllium", 1, 1, 1);
        ItemPoolLookups.add(fuel, "nugget_u233", 1, 1, 1);
        ItemPoolLookups.add(fuel, "nugget_uranium_fuel", 1, 1, 1);
        ItemPoolLookups.add(fuel, "rod_zirnox_empty", 1, 3, 3);
        ItemPoolLookups.add(fuel, "ingot_graphite", 1, 4, 3);
        ItemPoolLookups.add(fuel, "pile_rod_uranium", 2, 5, 3);
        ItemPoolLookups.add(fuel, "pile_rod_source", 1, 2, 2);
        ItemPoolLookups.add(fuel, "reacher", 1, 1, 3);
        ItemPoolLookups.add(fuel, "screwdriver", 1, 1, 2);

        ItemPool silo = new ItemPool(POOL_SILO);
        ItemPoolLookups.add(silo, "missile_generic", 1, 1, 4);
        ItemPoolLookups.add(silo, "missile_incendiary", 1, 1, 4);
        ItemPoolLookups.add(silo, "gas_mask_m65", 1, 1, 5);
        ItemPoolLookups.add(silo, "battery_lead_pack", 1, 1, 3);
        ItemPoolLookups.add(silo, "designator", 1, 1, 5);
        ItemPoolLookups.add(silo, "thruster_small", 1, 1, 5);
        ItemPoolLookups.add(silo, "thruster_medium", 1, 1, 4);
        ItemPoolLookups.add(silo, "fuel_tank_small", 1, 1, 5);
        ItemPoolLookups.add(silo, "fuel_tank_medium", 1, 1, 4);
        ItemPoolLookups.add(silo, "bomb_caller", 1, 1, 1);
        // SKIPPED: bomb_caller meta 3 — no discrete variant
        ItemPoolLookups.add(silo, "bottle_nuka", 1, 3, 10);

        ItemPool office = new ItemPool(POOL_OFFICE_TRASH);
        office.pool.add(ItemPool.entry(Items.PAPER, 1, 12, 10));
        office.pool.add(ItemPool.entry(Items.BOOK, 1, 3, 4));
        ItemPoolLookups.add(office, "twinkie", 1, 2, 6);
        ItemPoolLookups.add(office, "coffee", 1, 1, 4);
        ItemPoolLookups.add(office, "deco_computer_ibm_300pl", 1, 1, 1);
        ItemPoolLookups.add(office, "flame_politics", 1, 1, 2);
        ItemPoolLookups.add(office, "ring_pull", 1, 1, 4);
        ItemPoolLookups.add(office, "can_empty", 1, 1, 2);
        ItemPoolLookups.add(office, "can_creature", 1, 2, 2);
        ItemPoolLookups.add(office, "can_smart", 1, 3, 2);
        ItemPoolLookups.add(office, "can_mrsugar", 1, 2, 2);
        ItemPoolLookups.add(office, "cap_nuka", 1, 16, 2);
        ItemPoolLookups.add(office, "book_guide_book", 1, 1, 1);
        ItemPoolLookups.add(office, "deco_computer_ibm_300pl", 1, 1, 1); // CE lists deco_computer twice
        ItemPoolLookups.add(office, "blueprint_folder_base", 1, 1, 1);
        ItemPoolLookups.add(office, "coin_token", 1, 1, 2);

        ItemPool cabinet = new ItemPool(POOL_FILING_CABINET);
        cabinet.pool.add(ItemPool.entry(Items.PAPER, 1, 12, 240));
        cabinet.pool.add(ItemPool.entry(Items.BOOK, 1, 3, 90));
        cabinet.pool.add(ItemPool.entry(Items.MAP, 1, 1, 50));
        cabinet.pool.add(ItemPool.entry(Items.WRITABLE_BOOK, 1, 1, 30));
        ItemPoolLookups.add(cabinet, "cigarette", 1, 16, 20);
        ItemPoolLookups.add(cabinet, "dust", 1, 1, 40);
        ItemPoolLookups.add(cabinet, "dust_tiny", 1, 3, 75);
        ItemPoolLookups.add(cabinet, "ink", 1, 1, 1);
        ItemPoolLookups.add(cabinet, "screwdriver", 1, 1, 1);
        ItemPoolLookups.add(cabinet, "blueprint_folder_base", 1, 1, 5);
        ItemPoolLookups.add(cabinet, "coin_token", 1, 1, 30);

        ItemPool solid = new ItemPool(POOL_SOLID_FUEL);
        ItemPoolLookups.add(solid, "solid_fuel", 1, 5, 1);
        ItemPoolLookups.add(solid, "solid_fuel_presto", 1, 2, 2);
        ItemPoolLookups.add(solid, "ball_dynamite", 1, 4, 2);
        ItemPoolLookups.add(solid, "coke_petroleum", 1, 3, 1);
        solid.pool.add(ItemPool.entry(Items.REDSTONE, 1, 3, 1));
        ItemPoolLookups.add(solid, "niter", 1, 3, 1);

        ItemPool lab = new ItemPool(POOL_VAULT_LAB);
        // SKIPPED: ItemBlowtorch.getEmptyTool — tool NBT assembly not ported
        ItemPoolLookups.add(lab, "chemistry_set", 1, 1, 15);
        ItemPoolLookups.add(lab, "screwdriver", 1, 1, 10);
        ItemPoolLookups.add(lab, "ingot_mercury", 1, 1, 3);
        ItemPoolLookups.add(lab, "morning_glory", 1, 1, 1);
        ItemPoolLookups.add(lab, "filter_coal", 1, 1, 5);
        ItemPoolLookups.add(lab, "dust", 1, 3, 25);
        lab.pool.add(ItemPool.entry(Items.PAPER, 1, 2, 15));
        ItemPoolLookups.add(lab, "cell", 1, 1, 5);
        lab.pool.add(ItemPool.entry(Items.GLASS_BOTTLE, 1, 1, 5));
        ItemPoolLookups.add(lab, "powder_iodine", 1, 1, 1);
        ItemPoolLookups.add(lab, "powder_bromine", 1, 1, 1);
        ItemPoolLookups.add(lab, "powder_cobalt", 1, 1, 1);
        ItemPoolLookups.add(lab, "powder_neodymium", 1, 1, 1);
        ItemPoolLookups.add(lab, "powder_boron", 1, 1, 1);

        ItemPool lockers = new ItemPool(POOL_VAULT_LOCKERS);
        ItemPoolLookups.add(lockers, "robes_helmet", 1, 1, 1);
        ItemPoolLookups.add(lockers, "robes_plate", 1, 1, 1);
        ItemPoolLookups.add(lockers, "robes_legs", 1, 1, 1);
        ItemPoolLookups.add(lockers, "robes_boots", 1, 1, 1);
        ItemPoolLookups.add(lockers, "jackt", 1, 1, 1);
        ItemPoolLookups.add(lockers, "jackt2", 1, 1, 1);
        ItemPoolLookups.add(lockers, "gas_mask_m65", 1, 1, 2);
        ItemPoolLookups.add(lockers, "gas_mask_mono", 1, 1, 2);
        ItemPoolLookups.add(lockers, "goggles", 1, 1, 2);
        ItemPoolLookups.add(lockers, "gas_mask_filter", 1, 1, 4);
        ItemPoolLookups.add(lockers, "flame_opinion", 1, 3, 5);
        ItemPoolLookups.add(lockers, "flame_conspiracy", 1, 3, 5);
        ItemPoolLookups.add(lockers, "flame_politics", 1, 3, 5);
        ItemPoolLookups.add(lockers, "definitelyfood", 2, 7, 5);
        ItemPoolLookups.add(lockers, "cigarette", 1, 8, 5);
        ItemPoolLookups.add(lockers, "armor_polish", 1, 1, 3);
        ItemPoolLookups.add(lockers, "gun_kit_1", 1, 1, 3);
        ItemPoolLookups.add(lockers, "rag", 1, 3, 5);
        lockers.pool.add(ItemPool.entry(Items.PAPER, 1, 6, 7));
        lockers.pool.add(ItemPool.entry(Items.CLOCK, 1, 1, 3));
        lockers.pool.add(ItemPool.entry(Items.BOOK, 1, 5, 10));
        lockers.pool.add(ItemPool.entry(Items.EXPERIENCE_BOTTLE, 1, 3, 1));
        ItemPoolLookups.add(lockers, "blueprint_folder_base", 1, 1, 1);
        ItemPoolLookups.add(lockers, "blueprint_folder_discover", 1, 1, 1);
        ItemPoolLookups.add(lockers, "ammo_container", 1, 1, 1);
        ItemPoolLookups.add(lockers, "coin_token", 1, 1, 5);

        ItemPool meteor = new ItemPool(POOL_METEOR_SAFE);
        ItemPoolLookups.add(meteor, "book_of_", 1, 1, 1);
        ItemPoolLookups.add(meteor, "stamp_book_printing1", 1, 1, 1);
        ItemPoolLookups.add(meteor, "stamp_book_printing2", 1, 1, 1);
        ItemPoolLookups.add(meteor, "stamp_book_printing3", 1, 1, 1);
        ItemPoolLookups.add(meteor, "stamp_book_printing4", 1, 1, 1);
        ItemPoolLookups.add(meteor, "stamp_book_printing5", 1, 1, 1);
        ItemPoolLookups.add(meteor, "stamp_book_printing6", 1, 1, 1);
        ItemPoolLookups.add(meteor, "stamp_book_printing7", 1, 1, 1);
        ItemPoolLookups.add(meteor, "stamp_book_printing8", 1, 1, 1);
    }
}
