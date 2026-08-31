package com.hbm.itempool;

import com.hbm.items.weapon.sedna.content.GunPistolItems;
import net.minecraft.world.item.Items;

/**
 * Port of CE's {@code com.hbm.itempool.ItemPoolsSingle} (SHA {@code a23316ca}). Vault / powder-box /
 * meteorite-treasure / blueprint folder pools. Grenade-shell assembly skipped (same as Legacy).
 * Ammo meta remapped to discrete Sedna items where those exist.
 */
public final class ItemPoolsSingle {

    public static final String POOL_POWDER = "POOL_POWDER";
    public static final String POOL_VAULT_RUSTY = "POOL_VAULT_RUSTY";
    public static final String POOL_VAULT_STANDARD = "POOL_VAULT_STANDARD";
    public static final String POOL_VAULT_REINFORCED = "POOL_VAULT_REINFORCED";
    public static final String POOL_VAULT_UNBREAKABLE = "POOL_VAULT_UNBREAKABLE";
    public static final String POOL_METEORITE_TREASURE = "POOL_METEORITE_TREASURE";
    public static final String POOL_BLUEPRINTS = "POOL_BLUEPRINTS";

    private ItemPoolsSingle() {
    }

    public static void init() {
        ItemPool powder = new ItemPool(POOL_POWDER);
        ItemPoolLookups.add(powder, "powder_neptunium", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_iodine", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_thorium", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_astatine", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_neodymium", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_caesium", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_strontium", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_cobalt", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_bromine", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_niobium", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_tennessine", 1, 32, 1);
        ItemPoolLookups.add(powder, "powder_cerium", 1, 32, 1);

        ItemPool rusty = new ItemPool(POOL_VAULT_RUSTY);
        rusty.pool.add(ItemPool.entry(Items.GOLD_INGOT, 3, 14, 1));
        rusty.pool.add(ItemPool.entry(GunPistolItems.GUN_HEAVY_REVOLVER.get(), 1, 1, 2));
        ItemPoolLookups.add(rusty, "pin", 8, 8, 1);
        ItemPoolLookups.add(rusty, "gun_am180", 1, 1, 1);
        ItemPoolLookups.add(rusty, "bottle_quantum", 1, 3, 1);
        ItemPoolLookups.add(rusty, "ingot_cobalt", 4, 12, 1);
        ItemPoolLookups.add(rusty, "bmg50_fmj", 24, 48, 1);
        ItemPoolLookups.add(rusty, "p9_jhp", 48, 64, 2);
        ItemPoolLookups.add(rusty, "circuit_chip", 3, 6, 1);
        ItemPoolLookups.add(rusty, "gas_mask_m65", 1, 1, 1);
        // SKIPPED: ItemGrenadeUniversal FRAG/HE + FRAG/INC
        rusty.pool.add(ItemPool.entry(Items.DIAMOND, 1, 2, 1));

        ItemPool standard = new ItemPool(POOL_VAULT_STANDARD);
        ItemPoolLookups.add(standard, "ingot_desh", 2, 6, 1);
        ItemPoolLookups.add(standard, "powder_desh_mix", 1, 5, 1);
        standard.pool.add(ItemPool.entry(Items.DIAMOND, 3, 6, 1));
        ItemPoolLookups.add(standard, "ammo_nuke", 1, 1, 1);
        ItemPoolLookups.add(standard, "ammo_container", 1, 1, 1);
        // SKIPPED: grenade NUKE / EMP
        ItemPoolLookups.add(standard, "powder_yellowcake", 16, 24, 1);
        ItemPoolLookups.add(standard, "gun_uzi", 1, 1, 1);
        ItemPoolLookups.add(standard, "circuit_vacuum_tube", 12, 16, 1);
        ItemPoolLookups.add(standard, "circuit_chip", 2, 6, 1);

        ItemPool reinforced = new ItemPool(POOL_VAULT_REINFORCED);
        ItemPoolLookups.add(reinforced, "ingot_desh", 6, 16, 1);
        ItemPoolLookups.add(reinforced, "powder_power", 1, 5, 1);
        ItemPoolLookups.add(reinforced, "sat_chip", 1, 1, 1);
        reinforced.pool.add(ItemPool.entry(Items.DIAMOND, 5, 9, 1));
        ItemPoolLookups.add(reinforced, "ammo_nuke", 1, 3, 1);
        ItemPoolLookups.add(reinforced, "ammo_container", 1, 4, 1);
        ItemPoolLookups.add(reinforced, "powder_yellowcake", 26, 42, 1);
        reinforced.pool.add(ItemPool.entry(GunPistolItems.GUN_HEAVY_REVOLVER.get(), 1, 1, 1));
        ItemPoolLookups.add(reinforced, "circuit_chip", 18, 32, 1);
        ItemPoolLookups.add(reinforced, "circuit_analog", 6, 12, 1);

        ItemPool unbreakable = new ItemPool(POOL_VAULT_UNBREAKABLE);
        ItemPoolLookups.add(unbreakable, "ammo_container", 3, 6, 1);
        ItemPoolLookups.add(unbreakable, "ammo_nuke_demo", 2, 3, 1);
        ItemPoolLookups.add(unbreakable, "gun_carbine", 1, 1, 1);
        ItemPoolLookups.add(unbreakable, "ammo_r762_du", 16, 32, 1);
        ItemPoolLookups.add(unbreakable, "gun_congolake", 1, 1, 1);
        ItemPoolLookups.add(unbreakable, "circuit_advanced", 6, 12, 1);

        ItemPool treasure = new ItemPool(POOL_METEORITE_TREASURE);
        ItemPoolLookups.add(treasure, "cobalt_pickaxe", 1, 1, 10);
        ItemPoolLookups.add(treasure, "ingot_zirconium", 1, 16, 10);
        ItemPoolLookups.add(treasure, "ingot_niobium", 1, 16, 10);
        ItemPoolLookups.add(treasure, "ingot_cobalt", 1, 16, 10);
        ItemPoolLookups.add(treasure, "ingot_boron", 1, 16, 10);
        ItemPoolLookups.add(treasure, "ingot_starmetal", 1, 1, 5);
        ItemPoolLookups.add(treasure, "crystal_gold", 1, 4, 10);
        ItemPoolLookups.add(treasure, "circuit_vacuum_tube", 4, 8, 10);
        ItemPoolLookups.add(treasure, "circuit_chip", 2, 4, 10);
        ItemPoolLookups.add(treasure, "definitelyfood", 16, 32, 25);
        ItemPoolLookups.add(treasure, "crate_can", 1, 3, 10);
        ItemPoolLookups.add(treasure, "pill_herbal", 1, 2, 10);
        ItemPoolLookups.add(treasure, "serum", 1, 1, 5);
        ItemPoolLookups.add(treasure, "heart_piece", 1, 1, 5);
        ItemPoolLookups.add(treasure, "scrumpy", 1, 1, 5);
        ItemPoolLookups.add(treasure, "launch_code_piece", 1, 1, 5);
        ItemPoolLookups.add(treasure, "egg_glyphid", 1, 1, 5);
        ItemPoolLookups.add(treasure, "gem_alexandrite", 1, 1, 1);

        ItemPool blueprints = new ItemPool(POOL_BLUEPRINTS);
        ItemPoolLookups.add(blueprints, "blueprint_folder_base", 1, 1, 10);
        ItemPoolLookups.add(blueprints, "blueprint_folder_discover", 1, 1, 5);
        ItemPoolLookups.add(blueprints, "blueprint_folder_secret", 1, 1, 1);
    }
}
