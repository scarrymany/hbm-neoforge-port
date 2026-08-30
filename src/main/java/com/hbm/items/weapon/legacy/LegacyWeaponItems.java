package com.hbm.items.weapon.legacy;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Registered items for CE's legacy (pre-Sedna) gun roster - the two working charge weapons
 * ({@code gun_b92}, {@code gun_b93}) plus their shared ammo cell ({@code gun_b92_ammo}), and the two
 * confirmed-non-functional decorative shells ({@code gun_supershotgun}, {@code gun_vortex}). See
 * {@code docs/phase3/guns_and_ammo.md}'s "Legacy system" section and {@link LegacyChargeWeapons}/
 * {@link ItemGunSupershotgun}'s class javadocs for the full per-item rationale. Mirrors
 * {@code com.hbm.items.weapon.sedna.content.GunPistolItems}'s exact per-batch aggregator shape.
 */
public final class LegacyWeaponItems {

    private LegacyWeaponItems() {
    }

    public static final DeferredItem<ItemGunB92> GUN_B92 = ModItems.ITEMS.register("gun_b92", () -> new ItemGunB92(LegacyChargeWeapons.LEGENDARY_PROPS));
    public static final DeferredItem<ItemGunB93> GUN_B93 = ModItems.ITEMS.register("gun_b93", () -> new ItemGunB93(LegacyChargeWeapons.LEGENDARY_PROPS));
    public static final DeferredItem<ItemGunB92Cell> GUN_B92_AMMO = ModItems.ITEMS.register("gun_b92_ammo", () -> new ItemGunB92Cell(new Item.Properties()));

    public static final DeferredItem<ItemGunSupershotgun> GUN_SUPERSHOTGUN = ModItems.ITEMS.register("gun_supershotgun", () -> new ItemGunSupershotgun(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ItemGunVortex> GUN_VORTEX = ModItems.ITEMS.register("gun_vortex", () -> new ItemGunVortex(new Item.Properties().stacksTo(1)));

    /** No-op beyond forcing this class to load before {@code ModItems.ITEMS.register(modEventBus)}. */
    public static void registerAll() {
        CreativeTabContents.add(ModCreativeTabs.WEAPON, GUN_B92);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, GUN_B93);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, GUN_B92_AMMO);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, GUN_SUPERSHOTGUN);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, GUN_VORTEX);
    }
}
