package com.hbm.items.weapon;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemBoltgun;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Registers the 2 concrete, dependency-free {@code items/weapon} melee items this package's task
 * ports: {@code crucible} ({@link ItemCrucible}) and {@code boltgun} ({@link ItemBoltgun}, which
 * despite living in {@code items/tool} in CE - and in this port, matching CE's package - is a
 * melee-scope item per this package's task brief).
 * <p>
 * <b>Not registered here</b> (see this package's structured-output wiring notes for the full
 * rationale): the 23 {@code ItemSwordAbility}/{@code ItemSwordAbilityPower}/
 * {@code ItemSwordMeteorite} sword instances (need per-material {@link net.minecraft.world.item.Tier}
 * definitions and, for {@code elec_sword}/the meteorite tiers, subclasses this package's task did not
 * ask for) and the 8 {@link com.hbm.items.tool.ItemMultitoolPassive} rungs (need
 * {@code ToolItems}-owned {@code DeferredItem} references for their upgrade-chain
 * {@code nextRung} suppliers, and that file is concurrently owned by this wave's mining-tool-ability
 * area) - both classes are fully built and ready for whoever picks up that registration pass.
 */
public final class WeaponMeleeItems {

    private WeaponMeleeItems() {
    }

    public static final DeferredItem<Item> CRUCIBLE = ModItems.ITEMS.register("crucible", () ->
            new ItemCrucible(500F, 1.0, WeaponTiers.CRUCIBLE, new Item.Properties()));

    public static final DeferredItem<Item> BOLTGUN = ModItems.ITEMS.register("boltgun", () ->
            new ItemBoltgun(new Item.Properties().stacksTo(1)));

    public static void registerAll() {
        CreativeTabContents.add(ModCreativeTabs.WEAPON, CRUCIBLE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, BOLTGUN);
    }
}
