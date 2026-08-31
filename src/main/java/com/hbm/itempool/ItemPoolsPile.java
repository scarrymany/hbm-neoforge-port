package com.hbm.itempool;

import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import com.hbm.items.weapon.sedna.content.XFactory12ga;
import com.hbm.items.weapon.sedna.content.XFactory357;
import com.hbm.items.weapon.sedna.content.XFactory40mm;
import net.minecraft.world.item.Items;

/**
 * Port of CE's {@code com.hbm.itempool.ItemPoolsPile} (SHA {@code 4c61be97}). Glyphid-hive /
 * bone / cap / med / makeshift / nuke-storage / garbage / mechanical / gear piles.
 * Grenade-shell assembly skipped. Material-meta wire/bolt/pipe remapped to discrete
 * {@code wire_fine_*} ids where those exist.
 */
public final class ItemPoolsPile {

    public static final String POOL_PILE_HIVE = "POOL_PILE_HIVE";
    public static final String POOL_PILE_BONES = "POOL_PILE_BONES";
    public static final String POOL_PILE_CAPS = "POOL_PILE_CAPS";
    public static final String POOL_PILE_MED_SYRINGE = "POOL_PILE_MED_SYRINGE";
    public static final String POOL_PILE_MED_PILLS = "POOL_PILE_MED_PILLS";
    public static final String POOL_PILE_MAKESHIFT_GUN = "POOL_PILE_MAKESHIFT_GUN";
    public static final String POOL_PILE_MAKESHIFT_WRENCH = "POOL_PILE_MAKESHIFT_WRENCH";
    public static final String POOL_PILE_MAKESHIFT_PLATES = "POOL_PILE_MAKESHIFT_PLATES";
    public static final String POOL_PILE_MAKESHIFT_WIRE = "POOL_PILE_MAKESHIFT_WIRE";
    public static final String POOL_PILE_NUKE_STORAGE = "POOL_PILE_NUKE_STORAGE";
    public static final String POOL_PILE_OF_GARBAGE = "POOL_PILE_OF_GARBAGE";
    public static final String POOL_PILE_MECHANICAL = "POOL_PILE_MECHANICAL";
    public static final String POOL_PILE_GEAR = "POOL_PILE_GEAR";

    private ItemPoolsPile() {
    }

    public static void init() {
        ItemPool hive = new ItemPool(POOL_PILE_HIVE);
        hive.pool.add(ItemPool.entry(Items.IRON_INGOT, 1, 3, 10));
        ItemPoolLookups.add(hive, "ingot_steel", 1, 2, 10);
        ItemPoolLookups.add(hive, "ingot_aluminium", 1, 2, 10);
        ItemPoolLookups.add(hive, "scrap", 3, 6, 10);
        ItemPoolLookups.add(hive, "gas_mask_m65", 1, 1, 10);
        ItemPoolLookups.add(hive, "steel_plate", 1, 1, 5);
        ItemPoolLookups.add(hive, "steel_legs", 1, 1, 5);
        ItemPoolLookups.add(hive, "steel_pickaxe", 1, 1, 5);
        ItemPoolLookups.add(hive, "steel_shovel", 1, 1, 5);
        hive.pool.add(ItemPool.entry(GunShotgunItems.GUN_MARESLEG.get(), 1, 1, 5));
        hive.pool.add(ItemPool.entry(GunPistolItems.GUN_LIGHT_REVOLVER.get(), 1, 1, 1));
        hive.pool.add(ItemPool.entry(XFactory12ga.ITEM_G12_BP, 4, 4, 10));
        hive.pool.add(ItemPool.entry(XFactory357.ITEM_M357_SP, 6, 12, 10));
        hive.pool.add(ItemPool.entry(XFactory40mm.ITEM_G40_HE, 1, 1, 2));
        ItemPoolLookups.add(hive, "bottle_nuka", 1, 2, 20);
        ItemPoolLookups.add(hive, "bottle_quantum", 1, 2, 1);
        ItemPoolLookups.add(hive, "definitelyfood", 5, 12, 20);
        ItemPoolLookups.add(hive, "egg_glyphid", 1, 3, 30);
        ItemPoolLookups.add(hive, "syringe_metal_stimpak", 1, 1, 5);
        ItemPoolLookups.add(hive, "iv_blood", 1, 1, 10);
        hive.pool.add(ItemPool.entry(Items.EXPERIENCE_BOTTLE, 1, 3, 5));

        ItemPool bones = new ItemPool(POOL_PILE_BONES);
        bones.pool.add(ItemPool.entry(Items.BONE, 1, 1, 10));
        bones.pool.add(ItemPool.entry(Items.ROTTEN_FLESH, 1, 1, 5));
        ItemPoolLookups.add(bones, "biomass", 1, 1, 2);

        ItemPool caps = new ItemPool(POOL_PILE_CAPS);
        ItemPoolLookups.add(caps, "cap_nuka", 4, 4, 20);
        ItemPoolLookups.add(caps, "cap_quantum", 4, 4, 3);
        ItemPoolLookups.add(caps, "cap_sparkle", 4, 4, 1);

        ItemPool syringe = new ItemPool(POOL_PILE_MED_SYRINGE);
        ItemPoolLookups.add(syringe, "syringe_metal_stimpak", 1, 1, 10);
        ItemPoolLookups.add(syringe, "syringe_metal_medx", 1, 1, 5);
        ItemPoolLookups.add(syringe, "syringe_metal_psycho", 1, 1, 5);

        ItemPool pills = new ItemPool(POOL_PILE_MED_PILLS);
        ItemPoolLookups.add(pills, "radaway", 1, 1, 10);
        ItemPoolLookups.add(pills, "radx", 1, 1, 10);
        ItemPoolLookups.add(pills, "iv_blood", 1, 1, 15);
        ItemPoolLookups.add(pills, "siox", 1, 1, 5);

        ItemPool makeshiftGun = new ItemPool(POOL_PILE_MAKESHIFT_GUN);
        makeshiftGun.pool.add(ItemPool.entry(GunShotgunItems.GUN_MARESLEG.get(), 1, 1, 10));

        ItemPool wrench = new ItemPool(POOL_PILE_MAKESHIFT_WRENCH);
        ItemPoolLookups.add(wrench, "wrench", 1, 1, 10);

        ItemPool plates = new ItemPool(POOL_PILE_MAKESHIFT_PLATES);
        ItemPoolLookups.add(plates, "plate_steel", 1, 1, 10);

        ItemPool wire = new ItemPool(POOL_PILE_MAKESHIFT_WIRE);
        ItemPoolLookups.add(wire, "wire_fine_aluminium", 1, 1, 10);

        ItemPool nuke = new ItemPool(POOL_PILE_NUKE_STORAGE);
        ItemPoolLookups.add(nuke, "ammo_nuke", 1, 1, 50);
        ItemPoolLookups.add(nuke, "ammo_nuke_high", 1, 1, 10);
        ItemPoolLookups.add(nuke, "ammo_nuke_tots", 1, 1, 10);

        ItemPool garbage = new ItemPool(POOL_PILE_OF_GARBAGE);
        ItemPoolLookups.add(garbage, "pipe_steel", 0, 2, 20);
        ItemPoolLookups.add(garbage, "scrap", 1, 5, 20);
        ItemPoolLookups.add(garbage, "wire_fine_copper", 1, 2, 20);
        ItemPoolLookups.add(garbage, "dust", 1, 3, 40);
        ItemPoolLookups.add(garbage, "dust_tiny", 1, 7, 40);
        ItemPoolLookups.add(garbage, "powder_cement", 1, 6, 40);
        ItemPoolLookups.add(garbage, "nugget_lead", 0, 3, 20);
        ItemPoolLookups.add(garbage, "wire_fine_aluminium", 0, 3, 20);
        ItemPoolLookups.add(garbage, "powder_ash", 0, 1, 15);
        ItemPoolLookups.add(garbage, "plate_lead", 0, 1, 15);
        garbage.pool.add(ItemPool.entry(Items.STRING, 0, 1, 15));
        ItemPoolLookups.add(garbage, "bolt_steel", 0, 2, 15);
        ItemPoolLookups.add(garbage, "pin", 0, 2, 15);
        ItemPoolLookups.add(garbage, "cap_nuka", 0, 8, 15);
        ItemPoolLookups.add(garbage, "plate_iron", 0, 2, 15);
        ItemPoolLookups.add(garbage, "fallout", 0, 2, 15);
        ItemPoolLookups.add(garbage, "coil_tungsten", 0, 2, 15);
        ItemPoolLookups.add(garbage, "can_empty", 0, 1, 15);
        ItemPoolLookups.add(garbage, "ingot_asbestos", 0, 1, 15);
        ItemPoolLookups.add(garbage, "syringe_metal_empty", 0, 1, 15);
        ItemPoolLookups.add(garbage, "syringe_empty", 0, 1, 15);
        ItemPoolLookups.add(garbage, "pipe_lead", 0, 1, 5);
        ItemPoolLookups.add(garbage, "motor", 0, 1, 5);
        ItemPoolLookups.add(garbage, "canned_conserve_beef", 0, 1, 5);

        ItemPool mechanical = new ItemPool(POOL_PILE_MECHANICAL);
        ItemPoolLookups.add(mechanical, "defuser", 1, 1, 30);
        ItemPoolLookups.add(mechanical, "screwdriver", 1, 1, 30);
        ItemPoolLookups.add(mechanical, "wire_fine_copper", 8, 12, 120);
        ItemPoolLookups.add(mechanical, "plate_steel", 3, 8, 40);
        ItemPoolLookups.add(mechanical, "plate_copper", 2, 5, 40);
        ItemPoolLookups.add(mechanical, "coil_copper", 2, 5, 40);
        ItemPoolLookups.add(mechanical, "coil_tungsten", 2, 5, 40);

        ItemPool gear = new ItemPool(POOL_PILE_GEAR);
        ItemPoolLookups.add(gear, "defuser", 1, 1, 40);
        ItemPoolLookups.add(gear, "screwdriver", 1, 1, 30);
        ItemPoolLookups.add(gear, "canteen_vodka", 1, 1, 40);
        ItemPoolLookups.add(gear, "casing_small_steel", 1, 4, 30);
        ItemPoolLookups.add(gear, "casing_small", 3, 8, 40);
        ItemPoolLookups.add(gear, "casing_shotshell", 3, 8, 40);
        ItemPoolLookups.add(gear, "canned_conserve_beef", 2, 5, 40);
        ItemPoolLookups.add(gear, "taurun_helmet", 1, 1, 20);
        ItemPoolLookups.add(gear, "taurun_plate", 1, 1, 20);
        ItemPoolLookups.add(gear, "taurun_legs", 1, 1, 20);
        ItemPoolLookups.add(gear, "taurun_boots", 1, 1, 20);
    }
}
