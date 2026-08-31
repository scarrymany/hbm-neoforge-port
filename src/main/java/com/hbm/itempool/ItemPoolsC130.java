package com.hbm.itempool;

import com.hbm.blocks.machine.PowerGenBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.tool.ItemCanister;
import com.hbm.items.tool.ToolItems;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunRifleItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import com.hbm.items.weapon.sedna.content.GunHeavyItems;
import com.hbm.items.weapon.sedna.content.XFactory12ga;
import com.hbm.items.weapon.sedna.content.XFactory357;
import com.hbm.items.weapon.sedna.content.XFactory44;
import com.hbm.items.weapon.sedna.content.XFactory762mm;
import com.hbm.items.weapon.sedna.content.XFactory9mm;
import com.hbm.items.weapon.sedna.content.XFactoryRocket;
import com.hbm.items.food.FoodItems;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.itempool.ItemPoolsC130} (59 lines, read in full) -
 * {@link com.hbm.entity.logic.EntityC130}'s two loot pools, per both
 * {@code docs/phase4/entities_vehicles_aircraft.md} and
 * {@code docs/phase4/entities_orbital_and_beam_payloads.md} (jointly authoritative on this file - the
 * second report's own re-keyed-item table is followed here). {@code POOL_AMMO}'s CE entries key off
 * the legacy metadata item {@code ammo_standard} + {@code GunFactory.EnumAmmo} ordinal; this port's
 * Sedna gun content already replaced that with one discrete real {@code Item} per round, so {@code
 * meta} is dropped entirely per {@link ItemPool}'s own class javadoc - no numeric discriminator is
 * needed, just the real item reference, confirmed present for every one of CE's 9 ammo entries by name.
 * <p>
 * <b>3 of CE's 8 {@code POOL_SUPPLIES} entries are skipped</b> (not silently - see the inline comments
 * below): {@code syringe_metal_stimpak}, {@code med_bag}, and {@code radaway} (the drinkable item, as
 * opposed to {@code HbmPotionEffects.RADAWAY}, the {@code MobEffect} it would apply) are not registered
 * anywhere in this port yet (confirmed by repo-wide grep - {@code com.hbm.items.special.
 * ItemConsumable}, the shared base class all three would extend, is never instantiated anywhere). This
 * is a genuine gap in the two research reports' own "all confirmed present" claim for this pool,
 * discovered while actually building this file - flagged explicitly rather than silently ported around
 * or invented. The other 5 supply entries, all 8 weapon entries, and all 9 ammo entries are ported with
 * CE's exact weights/counts.
 */
public final class ItemPoolsC130 {

    public static final String POOL_SUPPLIES = "c130_supplies";
    public static final String POOL_WEAPONS = "c130_weapons";
    public static final String POOL_AMMO = "c130_ammo";

    private ItemPoolsC130() {
    }

    public static void init() {

        ItemPool supplies = new ItemPool(POOL_SUPPLIES);
        supplies.pool.addAll(List.of(
                ItemPool.entry(FoodItems.DEFINITELYFOOD.get(), 3, 10, 25),
                // SKIPPED: CE weighted(ModItems.syringe_metal_stimpak, 0, 1, 3, 10) - not registered
                // anywhere in this port yet (ItemConsumable is never instantiated) - see class javadoc.
                ItemPool.entry(FoodItems.PILL_IODINE.get(), 1, 2, 2),
                ItemPool.entry(fullDieselCanister(), 1, 4, 5),
                ItemPool.entry(PowerGenBlocks.MACHINE_DIESEL.get(), 1, 1, 1),
                ItemPool.entry(ToolItems.GEIGER_COUNTER.get(), 1, 1, 2)
                // SKIPPED: CE weighted(ModItems.med_bag, 0, 1, 1, 3) - not registered, see class javadoc.
                // SKIPPED: CE weighted(ModItems.radaway, 0, 1, 5, 10) - not registered (only the
                // HbmPotionEffects.RADAWAY MobEffect exists, not the drinkable item), see class javadoc.
        ));

        ItemPool weapons = new ItemPool(POOL_WEAPONS);
        weapons.pool.addAll(List.of(
                ItemPool.entry(GunPistolItems.GUN_LIGHT_REVOLVER.get(), 1, 1, 10),
                ItemPool.entry(GunPistolItems.GUN_HENRY.get(), 1, 1, 10),
                ItemPool.entry(GunShotgunItems.GUN_MARESLEG.get(), 1, 1, 10),
                ItemPool.entry(GunPistolItems.GUN_GREASEGUN.get(), 1, 1, 10),
                ItemPool.entry(GunRifleItems.GUN_CARBINE.get(), 1, 1, 5),
                ItemPool.entry(GunPistolItems.GUN_HEAVY_REVOLVER.get(), 1, 1, 5),
                ItemPool.entry(GunHeavyItems.GUN_PANZERSCHRECK.get(), 1, 1, 2),
                ItemPool.entry(GunShotgunItems.GUN_DOUBLE_BARREL.get(), 1, 1, 1)
        ));

        ItemPool ammo = new ItemPool(POOL_AMMO);
        ammo.pool.addAll(List.of(
                ItemPool.entry(XFactory357.ITEM_M357_SP, 12, 12, 10),
                ItemPool.entry(XFactory357.ITEM_M357_FMJ, 6, 6, 10),
                ItemPool.entry(XFactory44.ITEM_M44_SP, 12, 12, 5),
                ItemPool.entry(XFactory44.ITEM_M44_FMJ, 6, 6, 5),
                ItemPool.entry(XFactory9mm.ITEM_P9_SP, 12, 12, 10),
                ItemPool.entry(XFactory9mm.ITEM_P9_FMJ, 6, 6, 10),
                ItemPool.entry(XFactory762mm.ITEM_R762_SP, 6, 6, 5),
                ItemPool.entry(XFactory12ga.ITEM_G12_BP, 6, 6, 10),
                ItemPool.entry(XFactoryRocket.ITEM_ROCKET_HE, 1, 1, 3)
        ));
    }

    /**
     * CE: {@code weighted(ModItems.canister_full, Fluids.DIESEL.getID(), 1, 4, 5)} - CE's
     * {@code canister_full} is a separate metadata-subtype item; this port's {@link ItemCanister}
     * collapses "empty" and "filled" into one item plus a fill-level/fluid-type data component (see
     * that class's own javadoc), so a filled stack is built directly via {@link
     * ItemCanister#tryFill}.
     */
    private static ItemStack fullDieselCanister() {
        ItemStack stack = new ItemStack(ToolItems.CANISTER_FUEL.get());
        ((ItemCanister) stack.getItem()).tryFill(Fluids.DIESEL, 1000, stack);
        return stack;
    }
}
