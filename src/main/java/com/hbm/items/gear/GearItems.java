package com.hbm.items.gear;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ToolTiers;
import com.hbm.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registers the 13 Phase-1-safe {@code items/gear} tool/weapon files
 * (docs/phase1/items_food_gear.md, "Tools/weapons - Phase-1-safe" table).
 * <p>
 * <b>Deviation from the research report, confirmed by direct source inspection:</b>
 * {@code ModAxe}, {@code ModPickaxe}, {@code ModSpade}, {@code AxeSchrabidium},
 * {@code PickaxeSchrabidium}, {@code SpadeSchrabidium} and {@code SwordSchrabidium} are never
 * instantiated anywhere in {@code upstream/hbm-ce} (confirmed by grepping the entire tree for
 * {@code new <Class>(} - zero matches for any of the seven). They are dead source files with no
 * corresponding CE registry entry. Per the port's own ground rule ("do not invent items CE does not
 * have"), none of the seven are ported here - there is no real item to port.
 * <p>
 * Tab placement follows CE's actual final {@code .setCreativeTab(...)} call at each field's
 * declaration site in {@code ModItems.java} (not the dead default baked into each class's own
 * constructor, which every declaration site overrides): every hoe/sword here lands in vanilla's
 * {@code minecraft:tools_and_utilities} or {@code minecraft:combat} tabs, not one of this mod's own
 * ten tabs. {@link com.hbm.creativetabs.CreativeTabContents} only feeds this mod's own tabs (see its
 * javadoc), so placing items into a vanilla tab needs the standard NeoForge mechanism instead - a
 * {@link BuildCreativeModeTabContentsEvent} listener, registered from {@link #registerAll} onto the
 * mod event bus, gated on {@link BuildCreativeModeTabContentsEvent#getTabKey()}.
 */
public final class GearItems {

    private static final List<Supplier<? extends ItemLike>> TOOLS_TAB = new ArrayList<>();
    private static final List<Supplier<? extends ItemLike>> COMBAT_TAB = new ArrayList<>();

    private GearItems() {
    }

    public static void registerAll(IEventBus modEventBus) {
        modEventBus.addListener(GearItems::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            TOOLS_TAB.forEach(item -> event.accept(item.get()));
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            COMBAT_TAB.forEach(item -> event.accept(item.get()));
        }
    }

    /**
     * CE's {@code ModHoe} universally hardcodes 0 attack damage / -2.8 attack speed regardless of
     * material (upstream hbm-ce {@code ModHoe.getItemAttributeModifiers}) - unlike modern vanilla
     * hoes, which vary attack damage per tier. Every CE hoe gets that same constant pair here,
     * matching CE exactly rather than inventing a per-material damage curve CE never had.
     */
    private static DeferredItem<Item> hoeWithAttack(String name, Tier tier) {
        return ModItems.ITEMS.register(name, () ->
                new HoeItem(tier, new Item.Properties().attributes(HoeItem.createAttributes(tier, 0.0F, -2.8F))));
    }

    private static DeferredItem<Item> sword(String name, Tier tier) {
        return ModItems.ITEMS.register(name, () ->
                new ModSword(tier, new Item.Properties().attributes(SwordItem.createAttributes(tier, 3, -2.4F))));
    }

    private static ItemAttributeModifiers weaponAttributesWithSlow(Tier tier, double movementSpeedPenalty) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 3.0 + tier.getAttackDamageBonus(), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "weapon_modifier"), movementSpeedPenalty, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // ==================== hoes (CE ModItems.java ~1444-1663) - vanilla TOOLS_AND_UTILITIES tab ====================

    public static final DeferredItem<Item> TITANIUM_HOE = tabTools(hoeWithAttack("titanium_hoe", ToolTiers.TITANIUM));
    public static final DeferredItem<Item> STEEL_HOE = tabTools(hoeWithAttack("steel_hoe", ToolTiers.STEEL));
    /** CE marks this field {@code @Deprecated} but still instantiates it - kept for parity. */
    public static final DeferredItem<Item> ALLOY_HOE = tabTools(hoeWithAttack("alloy_hoe", ToolTiers.ALLOY));
    public static final DeferredItem<Item> DESH_HOE = tabTools(hoeWithAttack("desh_hoe", ToolTiers.DESH));
    public static final DeferredItem<Item> COBALT_HOE = tabTools(hoeWithAttack("cobalt_hoe", ToolTiers.COBALT));
    public static final DeferredItem<Item> COBALT_DECORATED_HOE = tabTools(hoeWithAttack("cobalt_decorated_hoe", ToolTiers.COBALT_DECORATED));
    public static final DeferredItem<Item> STARMETAL_HOE = tabTools(hoeWithAttack("starmetal_hoe", ToolTiers.STARMETAL));
    public static final DeferredItem<Item> CMB_HOE = tabTools(hoeWithAttack("cmb_hoe", ToolTiers.CMB));
    /** CE's {@code HoeSchrabidium} extends vanilla {@code ItemHoe} directly (not {@code ModHoe}) - no attack-attribute override, just RARE rarity. */
    public static final DeferredItem<Item> SCHRABIDIUM_HOE = tabTools(ModItems.ITEMS.register("schrabidium_hoe", () ->
            new HoeItem(GearTiers.SCHRABIDIUM, new Item.Properties().rarity(Rarity.RARE))));

    // ==================== swords (CE ModItems.java ~1699-1706) ====================

    public static final DeferredItem<Item> CROWBAR = tabTools(sword("crowbar", GearTiers.STEEL));
    public static final DeferredItem<Item> WEAPON_SAW = tabTools(sword("weapon_saw", GearTiers.SAW));
    public static final DeferredItem<Item> WEAPON_BAT = tabCombat(sword("weapon_bat", GearTiers.BAT));
    public static final DeferredItem<Item> WEAPON_BAT_NAIL = tabCombat(sword("weapon_bat_nail", GearTiers.BAT_NAIL));
    public static final DeferredItem<Item> WEAPON_GOLF_CLUB = tabCombat(sword("weapon_golf_club", GearTiers.GOLF_CLUB));
    public static final DeferredItem<Item> WEAPON_PIPE_RUSTY = tabCombat(sword("weapon_pipe_rusty", GearTiers.PIPE_RUSTY));
    public static final DeferredItem<Item> WEAPON_PIPE_LEAD = tabCombat(sword("weapon_pipe_lead", GearTiers.PIPE_LEAD));
    public static final DeferredItem<Item> REER_GRAAR = tabCombat(sword("reer_graar", GearTiers.TITANIUM));

    // ==================== big_sword / redstone_sword (CE ModItems.java:76-77) - vanilla COMBAT ====

    public static final DeferredItem<Item> BIG_SWORD = tabCombat(ModItems.ITEMS.register("big_sword", () ->
            new BigSword(Tiers.DIAMOND, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4F)))));
    public static final DeferredItem<Item> REDSTONE_SWORD = tabCombat(ModItems.ITEMS.register("redstone_sword", () ->
            new RedstoneSword(Tiers.STONE, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.STONE, 3, -2.4F)))));

    // ==================== WeaponSpecial (CE ModItems.java 741-753, 1364, 1707-1709) ====================
    // schrabidium_hammer/shimmer_sledge/shimmer_axe/ullapool_caber/wrench_flipped/memespoon/
    // wood_gavel/lead_gavel/diamond_gavel -> this mod's own WEAPON tab; bottle_opener -> CONSUMABLE;
    // stopsign/sopsign/chernobylsign -> vanilla COMBAT (no MainRegistry.weaponTab call in CE for
    // those three, confirmed at ModItems.java:1707-1709).

    public static final DeferredItem<Item> SCHRABIDIUM_HAMMER = weapon("schrabidium_hammer", GearTiers.HAMMER,
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).attributes(weaponAttributesWithSlow(GearTiers.HAMMER, -0.5)));
    public static final DeferredItem<Item> SHIMMER_SLEDGE = weapon("shimmer_sledge", GearTiers.SLEDGE,
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).attributes(SwordItem.createAttributes(GearTiers.SLEDGE, 3, -2.4F)));
    public static final DeferredItem<Item> SHIMMER_AXE = weapon("shimmer_axe", GearTiers.SLEDGE,
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).attributes(SwordItem.createAttributes(GearTiers.SLEDGE, 3, -2.4F)));
    public static final DeferredItem<Item> ULLAPOOL_CABER = weapon("ullapool_caber", GearTiers.STEEL,
            new Item.Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes(GearTiers.STEEL, 3, -2.4F)));
    public static final DeferredItem<Item> WRENCH_FLIPPED = weapon("wrench_flipped", GearTiers.ELEC,
            new Item.Properties().stacksTo(1).attributes(weaponAttributesWithSlow(GearTiers.ELEC, -0.1)));
    public static final DeferredItem<Item> MEMESPOON = weapon("memespoon", GearTiers.STEEL,
            new Item.Properties().stacksTo(1).attributes(SwordItem.createAttributes(GearTiers.STEEL, 3, -2.4F)));
    public static final DeferredItem<Item> WOOD_GAVEL = weapon("wood_gavel", Tiers.WOOD,
            new Item.Properties().stacksTo(1).attributes(SwordItem.createAttributes(Tiers.WOOD, 3, -2.4F)));
    public static final DeferredItem<Item> LEAD_GAVEL = weapon("lead_gavel", GearTiers.STEEL,
            new Item.Properties().stacksTo(1).attributes(SwordItem.createAttributes(GearTiers.STEEL, 3, -2.4F)));
    public static final DeferredItem<Item> DIAMOND_GAVEL = weapon("diamond_gavel", Tiers.DIAMOND,
            new Item.Properties().stacksTo(1).attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4F)));

    public static final DeferredItem<Item> BOTTLE_OPENER = tabConsumable(ModItems.ITEMS.register("bottle_opener", () ->
            new WeaponSpecial(GearTiers.BOTTLE_OPENER, new Item.Properties().stacksTo(1).attributes(SwordItem.createAttributes(GearTiers.BOTTLE_OPENER, 3, -2.4F)))));

    public static final DeferredItem<Item> STOPSIGN = tabCombat(ModItems.ITEMS.register("stopsign", () ->
            new WeaponSpecial(GearTiers.ALLOY, new Item.Properties().attributes(SwordItem.createAttributes(GearTiers.ALLOY, 3, -2.4F)))));
    public static final DeferredItem<Item> SOPSIGN = tabCombat(ModItems.ITEMS.register("sopsign", () ->
            new WeaponSpecial(GearTiers.ALLOY, new Item.Properties().attributes(SwordItem.createAttributes(GearTiers.ALLOY, 3, -2.4F)))));
    public static final DeferredItem<Item> CHERNOBYLSIGN = tabCombat(ModItems.ITEMS.register("chernobylsign", () ->
            new WeaponSpecial(GearTiers.ALLOY, new Item.Properties().attributes(SwordItem.createAttributes(GearTiers.ALLOY, 3, -2.4F)))));

    private static DeferredItem<Item> weapon(String name, Tier tier, Item.Properties properties) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new WeaponSpecial(tier, properties));
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    private static DeferredItem<Item> tabTools(DeferredItem<Item> item) {
        TOOLS_TAB.add(item);
        return item;
    }

    private static DeferredItem<Item> tabCombat(DeferredItem<Item> item) {
        COMBAT_TAB.add(item);
        return item;
    }

    private static DeferredItem<Item> tabConsumable(DeferredItem<Item> item) {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, item);
        return item;
    }
}
