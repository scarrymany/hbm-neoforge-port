package com.hbm.items.weapon;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.handler.ability.IWeaponAbility;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemBoltgun;
import com.hbm.items.tool.ItemSwordAbility;
import com.hbm.items.tool.ToolTiers;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Registers the {@code items/weapon} melee items this package's task ports: {@code crucible}
 * ({@link ItemCrucible}), {@code boltgun} ({@link ItemBoltgun}, which despite living in
 * {@code items/tool} in CE - and in this port, matching CE's package - is a melee-scope item per
 * this package's task brief), and 10 of the 11 direct {@link ItemSwordAbility} instances (CE:
 * {@code titanium_sword} through {@code dnt_sword}, per {@code docs/phase3/melee_weapons.md} section
 * A) - this fixes the gap that report's own follow-up wiring note (and this class's own earlier
 * javadoc) flagged: {@link ItemSwordAbility} was fully built and correct but never actually
 * registered anywhere.
 * <p>
 * <b>Still not registered here</b> - genuinely blocked, not an oversight:
 * <ul>
 *     <li>{@code elec_sword} - needs {@code ItemSwordAbilityPower} (battery-charge sword variant,
 *     mirroring {@code ItemToolAbilityPower}), which does not exist yet anywhere in this port.</li>
 *     <li>The 11 {@code meteorite_sword}* tiers - need {@code ItemSwordMeteorite} (unbreakable,
 *     {@code this ==}-dispatched flavor-text tooltip variant of the sword family) plus a
 *     {@code matMeteorite}-equivalent {@link Tier}, neither of which exists yet.</li>
 * </ul>
 * Both are real, narrow, named gaps - see this area's structured-output report - left unbuilt rather
 * than guessed at, since inventing either class's exact shape without the real 1.21.1 battery/tooltip
 * API already proven elsewhere in this port would be exactly the kind of unverified new class this
 * review pass should not add.
 * <p>
 * CE parity note (confirmed by direct read of CE's real {@code ModItems.java}): of these 10 swords,
 * only {@code mese_gavel} is creative-tab-visible in CE (its constructor chain ends in
 * {@code .setCreativeTab(MainRegistry.weaponTab)}) - the other 9 have no {@code setCreativeTab} call
 * at all in CE and are reached only via crafting/give, not the creative menu. Preserved exactly:
 * do not add the other 9 to any {@link CreativeTabContents} tab, that would be a real deviation from
 * CE, not a fix.
 */
public final class WeaponMeleeItems {

    private WeaponMeleeItems() {
    }

    public static final DeferredItem<Item> CRUCIBLE = ModItems.ITEMS.register("crucible", () ->
            new ItemCrucible(500F, 1.0, WeaponTiers.CRUCIBLE, new Item.Properties()));

    public static final DeferredItem<Item> BOLTGUN = ModItems.ITEMS.register("boltgun", () ->
            new ItemBoltgun(new Item.Properties().stacksTo(1)));

    // ==================== ItemSwordAbility direct instances (10 of 11 - see class javadoc) ====================

    public static final DeferredItem<Item> TITANIUM_SWORD = ModItems.ITEMS.register("titanium_sword", () ->
            new ItemSwordAbility(6.5F, 0, ToolTiers.TITANIUM, new Item.Properties()));

    public static final DeferredItem<Item> STEEL_SWORD = ModItems.ITEMS.register("steel_sword", () ->
            new ItemSwordAbility(6F, 0, ToolTiers.STEEL, new Item.Properties())
                    .addAbility(IWeaponAbility.STUN, 0));

    /** CE: {@code @Deprecated}, still instantiated. */
    @Deprecated
    public static final DeferredItem<Item> ALLOY_SWORD = ModItems.ITEMS.register("alloy_sword", () ->
            new ItemSwordAbility(8F, 0, ToolTiers.ALLOY, new Item.Properties())
                    .addAbility(IWeaponAbility.STUN, 0));

    public static final DeferredItem<Item> DESH_SWORD = ModItems.ITEMS.register("desh_sword", () ->
            new ItemSwordAbility(15F, 0, ToolTiers.DESH, new Item.Properties())
                    .addAbility(IWeaponAbility.STUN, 0));

    public static final DeferredItem<Item> COBALT_SWORD = ModItems.ITEMS.register("cobalt_sword", () ->
            new ItemSwordAbility(12F, 0, ToolTiers.COBALT, new Item.Properties()));

    /** CE also adds {@code IWeaponAbility.BOBBLE, 0} here - not ported yet, see class javadoc/{@link IWeaponAbility}. */
    public static final DeferredItem<Item> COBALT_DECORATED_SWORD = ModItems.ITEMS.register("cobalt_decorated_sword", () ->
            new ItemSwordAbility(15F, 0, ToolTiers.COBALT_DECORATED, new Item.Properties()));

    /** CE also adds {@code IWeaponAbility.BOBBLE, 0} here - not ported yet, see class javadoc/{@link IWeaponAbility}. */
    public static final DeferredItem<Item> STARMETAL_SWORD = ModItems.ITEMS.register("starmetal_sword", () ->
            new ItemSwordAbility(25F, 0, ToolTiers.STARMETAL, new Item.Properties())
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .addAbility(IWeaponAbility.STUN, 1));

    public static final DeferredItem<Item> CMB_SWORD = ModItems.ITEMS.register("cmb_sword", () ->
            new ItemSwordAbility(35F, 0, ToolTiers.CMB, new Item.Properties())
                    .addAbility(IWeaponAbility.STUN, 0)
                    .addAbility(IWeaponAbility.VAMPIRE, 0));

    public static final DeferredItem<Item> SCHRABIDIUM_SWORD = ModItems.ITEMS.register("schrabidium_sword", () ->
            new ItemSwordAbility(75F, 0, ToolTiers.SCHRABIDIUM, new Item.Properties().rarity(Rarity.RARE))
                    .addAbility(IWeaponAbility.RADIATION, 1)
                    .addAbility(IWeaponAbility.VAMPIRE, 0));

    /**
     * CE's {@code matMeseGavel} ({@code EnumHelper.addToolMaterial("HBM_MESEGAVEL", 4, 0, 50F, 0.0F,
     * 200)}, upstream {@code ModItems.java:1767}) - kept package-local here rather than added to the
     * shared {@code ToolTiers} (concurrently owned by the mining-tool-ability area) since no other
     * item needs it. Matches {@code ToolTiers.MESE}'s own numbers exactly (same 0-durability/50-speed/
     * 200-enchantability triple, same "no matching material tag yet" {@link Ingredient#EMPTY} repair).
     */
    private static final Tier MESE_GAVEL_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0, 50.0F, 0.0F, 200, () -> Ingredient.EMPTY);

    /** CE also adds {@code IWeaponAbility.PHOSPHORUS, 0} here - not ported yet (needs the unbuilt {@code HbmPotion} registry, see {@link IWeaponAbility}'s own javadoc). */
    public static final DeferredItem<Item> MESE_GAVEL = ModItems.ITEMS.register("mese_gavel", () ->
            new ItemSwordAbility(250F, 1.5, MESE_GAVEL_TIER, new Item.Properties())
                    .addAbility(IWeaponAbility.RADIATION, 2)
                    .addAbility(IWeaponAbility.STUN, 3)
                    .addAbility(IWeaponAbility.VAMPIRE, 4)
                    .addAbility(IWeaponAbility.BEHEADER, 0));

    public static final DeferredItem<Item> DNT_SWORD = ModItems.ITEMS.register("dnt_sword", () ->
            new ItemSwordAbility(12F, 0, ToolTiers.MESE, new Item.Properties()));

    public static void registerAll() {
        CreativeTabContents.add(ModCreativeTabs.WEAPON, CRUCIBLE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, BOLTGUN);
        // CE parity: mese_gavel is the only one of the 10 swords above with a real creative tab
        // (MainRegistry.weaponTab) - the other 9 are crafting/give-only in CE too, see class javadoc.
        CreativeTabContents.add(ModCreativeTabs.WEAPON, MESE_GAVEL);
    }
}
