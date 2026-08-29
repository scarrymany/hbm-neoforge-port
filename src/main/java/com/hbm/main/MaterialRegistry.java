package com.hbm.main;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ports CE's armor/tool material catalog (built on Forge's {@code EnumHelper}) onto NeoForge 21.1, where
 * {@link ArmorMaterial} is a registry entry (backed by {@link DeferredRegister}) and tool "materials" are plain
 * {@link Tier} objects rather than a registry.
 *
 * <p>Two structural differences from CE, both forced by the 1.21 API:
 *
 * <p>1. CE registers materials with no repair item and back-fills the real ones in a second pass
 * ({@code initFixMaterials}) once {@code ModItems} exists. In 1.21 the repair ingredient is baked into the
 * {@link ArmorMaterial}/{@link Tier} at construction time, before any items exist. This is solved by pointing
 * every repair ingredient at a {@code hbm:repair/<name>} item tag instead of a concrete item; the items area
 * populates those tags once its items are registered, removing the registration-order dependency entirely.
 *
 * <p>2. {@link ArmorMaterial} no longer carries CE's durability multiplier (the first {@code EnumHelper}
 * argument) - 1.21 applies durability per {@link ArmorItem.Type} at item-construction time via
 * {@code Item.Properties.durability(type.getDurability(multiplier))}. The multiplier is preserved here via
 * {@link #getDurabilityMultiplier(Holder)} so the items area can look it up when building the actual armor items.
 */
public final class MaterialRegistry {

    private MaterialRegistry() {}

    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MainRegistry.MODID);

    private static final Map<Holder<ArmorMaterial>, Integer> DURABILITY_MULTIPLIERS = new HashMap<>();

    private static final Supplier<Ingredient> NO_REPAIR = () -> Ingredient.of();

    // Repair item tags. Empty until the items area adds its items to them - see the class javadoc.
    private static final Supplier<Ingredient> REPAIR_INGOT_SCHRABIDIUM = repairTag("ingot_schrabidium");
    private static final Supplier<Ingredient> REPAIR_HAZMAT_CLOTH = repairTag("hazmat_cloth");
    private static final Supplier<Ingredient> REPAIR_HAZMAT_CLOTH_RED = repairTag("hazmat_cloth_red");
    private static final Supplier<Ingredient> REPAIR_HAZMAT_CLOTH_GREY = repairTag("hazmat_cloth_grey");
    private static final Supplier<Ingredient> REPAIR_PLATE_ARMOR_LUNAR = repairTag("plate_armor_lunar");
    private static final Supplier<Ingredient> REPAIR_PLATE_ARMOR_AJR = repairTag("plate_armor_ajr");
    private static final Supplier<Ingredient> REPAIR_PLATE_ARMOR_HEV = repairTag("plate_armor_hev");
    private static final Supplier<Ingredient> REPAIR_INGOT_TITANIUM = repairTag("ingot_titanium");
    private static final Supplier<Ingredient> REPAIR_INGOT_STEEL = repairTag("ingot_steel");
    private static final Supplier<Ingredient> REPAIR_PLATE_PAA = repairTag("plate_paa");
    private static final Supplier<Ingredient> REPAIR_INGOT_COMBINE_STEEL = repairTag("ingot_combine_steel");
    private static final Supplier<Ingredient> REPAIR_INGOT_AUSTRALIUM = repairTag("ingot_australium");
    private static final Supplier<Ingredient> REPAIR_PLATE_KEVLAR = repairTag("plate_kevlar");
    private static final Supplier<Ingredient> REPAIR_ASBESTOS_CLOTH = repairTag("asbestos_cloth");
    private static final Supplier<Ingredient> REPAIR_PLATE_LEAD = repairTag("plate_lead");
    private static final Supplier<Ingredient> REPAIR_PLATE_ARMOR_FAU = repairTag("plate_armor_fau");
    private static final Supplier<Ingredient> REPAIR_PLATE_ARMOR_DNT = repairTag("plate_armor_dnt");
    private static final Supplier<Ingredient> REPAIR_BLOCK_SCHRABIDIUM = repairTag("block_schrabidium");
    private static final Supplier<Ingredient> REPAIR_PLATE_STEEL = repairTag("plate_steel");
    private static final Supplier<Ingredient> REPAIR_INGOT_DESH = repairTag("ingot_desh");

    // ---- Armor materials ----
    // int[] defense order matches CE exactly: {boots, leggings, chestplate, helmet}.

    public static final Holder<ArmorMaterial> enumArmorMaterialT51 = armor("t51", "t51", 150, new int[]{3, 8, 6, 3}, 0, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatBJ = armor("blackjack", "hbm_blackjack", 150, new int[]{3, 6, 8, 3}, 0, 2.0F, REPAIR_PLATE_ARMOR_LUNAR);
    public static final Holder<ArmorMaterial> aMatAJR = armor("t45ajr", "t45ajr", 150, new int[]{3, 6, 8, 3}, 0, 2.0F, REPAIR_PLATE_ARMOR_AJR);
    public static final Holder<ArmorMaterial> aMatSteamsuit = armor("steamsuit", "steamsuit", 150, new int[]{3, 6, 8, 3}, 100, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatDieselsuit = armor("dieselsuit", "dieselsuit", 150, new int[]{3, 6, 8, 3}, 100, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatTrench = armor("trenchmaster", "trenchmaster", 150, new int[]{3, 6, 8, 3}, 0, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatTaurun = armor("taurun", "taurun", 150, new int[]{3, 6, 8, 3}, 10, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatBismuth = armor("bismuth", "bismuth", 150, new int[]{3, 6, 8, 3}, 100, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatZirconium = armor("zirconium", "zirconium", 150, new int[]{3, 6, 8, 3}, 1000, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatDNT = armor("dnt", "dnt", 3, new int[]{1, 1, 1, 1}, 0, 0.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatEnvsuit = armor("envsuit", "envsuit", 150, new int[]{3, 6, 8, 3}, 10, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatRPA = armor("rpa", "rpa", 150, new int[]{3, 6, 8, 3}, 100, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatHEV = armor("hev", "hev", 150, new int[]{3, 6, 8, 3}, 0, 2.0F, REPAIR_PLATE_ARMOR_HEV);
    public static final Holder<ArmorMaterial> aMatHaz = armor("hazmat", "hazmat", 60, new int[]{1, 4, 5, 2}, 5, 0.0F, REPAIR_HAZMAT_CLOTH);
    public static final Holder<ArmorMaterial> aMatHaz2 = armor("hazmat2", "hazmat2", 60, new int[]{1, 4, 5, 2}, 5, 0.0F, REPAIR_HAZMAT_CLOTH_RED);
    public static final Holder<ArmorMaterial> aMatHaz3 = armor("hazmat3", "hazmat3", 60, new int[]{1, 4, 5, 2}, 5, 0.0F, REPAIR_HAZMAT_CLOTH_GREY);
    public static final Holder<ArmorMaterial> aMatPaa = armor("paa", "paa", 75, new int[]{3, 6, 8, 3}, 25, 2.0F, REPAIR_PLATE_PAA);
    public static final Holder<ArmorMaterial> aMatSchrab = armor("schrabidium", "schrabidium", 100, new int[]{3, 6, 8, 3}, 50, 2.0F, REPAIR_INGOT_SCHRABIDIUM);
    public static final Holder<ArmorMaterial> aMatEuph = armor("euphemium", "euphemium", 15000000, new int[]{3, 6, 8, 3}, 100, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatSteel = armor("steel", "steel", 30, new int[]{3, 6, 8, 3}, 5, 0.0F, REPAIR_INGOT_STEEL);
    // deprecated, alloy armor is uncraftable (preserved verbatim from CE)
    public static final Holder<ArmorMaterial> aMatAlloy = armor("alloy", "alloy", 40, new int[]{3, 6, 8, 3}, 12, 0.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatAus3 = armor("ausiii", "ausiii", 375, new int[]{2, 5, 6, 2}, 0, 0.0F, REPAIR_INGOT_AUSTRALIUM);
    public static final Holder<ArmorMaterial> aMatTitan = armor("titanium", "titanium", 25, new int[]{3, 6, 8, 3}, 9, 2.0F, REPAIR_INGOT_TITANIUM);
    public static final Holder<ArmorMaterial> aMatCMB = armor("cmb", "cmb", 60, new int[]{3, 6, 8, 3}, 50, 2.0F, REPAIR_INGOT_COMBINE_STEEL);
    public static final Holder<ArmorMaterial> aMatSecurity = armor("security", "security", 100, new int[]{3, 6, 8, 3}, 15, 2.0F, REPAIR_PLATE_KEVLAR);
    public static final Holder<ArmorMaterial> aMatAsbestos = armor("asbestos", "asbestos", 20, new int[]{1, 3, 4, 1}, 5, 0.0F, REPAIR_ASBESTOS_CLOTH);
    public static final Holder<ArmorMaterial> aMatCobalt = armor("cobalt", "cobalt", 70, new int[]{3, 6, 8, 3}, 25, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatStarmetal = armor("starmetal", "starmetal", 150, new int[]{3, 6, 8, 3}, 100, 2.0F, NO_REPAIR);
    public static final Holder<ArmorMaterial> aMatLiquidator = armor("liquidator", "liquidator", 750, new int[]{3, 6, 8, 3}, 10, 2.0F, REPAIR_PLATE_LEAD);
    public static final Holder<ArmorMaterial> aMatFau = armor("digamma", "digamma", 150, new int[]{3, 8, 6, 3}, 0, 2.0F, REPAIR_PLATE_ARMOR_FAU);
    public static final Holder<ArmorMaterial> aMatDNS = armor("dnt_nano", "dnt_nano", 150, new int[]{3, 8, 6, 3}, 0, 2.0F, REPAIR_PLATE_ARMOR_DNT);

    // ---- Tool tiers ----
    // Parameter order matches CE's EnumHelper.addToolMaterial exactly: harvestLevel, maxUses, efficiency,
    // attackDamageBonus, enchantability. Tiers are not registry entries in NeoForge, so no id/name survives.

    public static final Tier enumToolMaterialSchrabidium = tier(4, 10000, 50.0F, 100.0F, 200, REPAIR_INGOT_SCHRABIDIUM);
    public static final Tier enumToolMaterialHammer = tier(3, 0, 50.0F, 999999996F, 200, REPAIR_BLOCK_SCHRABIDIUM);
    public static final Tier enumToolMaterialChainsaw = tier(3, 1500, 50.0F, 22.0F, 0, REPAIR_INGOT_STEEL);
    public static final Tier enumToolMaterialSteel = tier(3, 750, 8.0F, 2.0F, 10, REPAIR_INGOT_STEEL);
    public static final Tier enumToolMaterialTitanium = tier(3, 1000, 9.0F, 2.5F, 15, REPAIR_INGOT_TITANIUM);
    public static final Tier enumToolMaterialAlloy = tier(3, 2000, 15.0F, 5.0F, 5, NO_REPAIR);
    public static final Tier enumToolMaterialCmb = tier(4, 8500, 40.0F, 55F, 100, REPAIR_INGOT_COMBINE_STEEL);
    public static final Tier enumToolMaterialElec = tier(2, 0, 30.0F, 12.0F, 2, NO_REPAIR);
    public static final Tier enumToolMaterialDesh = tier(2, 0, 7.5F, 2.0F, 10, REPAIR_INGOT_DESH);
    public static final Tier enumToolMaterialCobalt = tier(4, 750, 9.0F, 2.5F, 15, NO_REPAIR);
    public static final Tier enumToolMaterialSaw = tier(2, 750, 2.0F, 3.5F, 25, NO_REPAIR);
    public static final Tier enumToolMaterialBat = tier(0, 500, 1.5F, 3F, 25, NO_REPAIR);
    public static final Tier enumToolMaterialBatNail = tier(0, 450, 1.0F, 4F, 25, NO_REPAIR);
    public static final Tier enumToolMaterialGolfClub = tier(1, 1000, 2.0F, 5F, 25, NO_REPAIR);
    public static final Tier enumToolMaterialPipeRusty = tier(1, 350, 1.5F, 4.5F, 25, NO_REPAIR);
    public static final Tier enumToolMaterialPipeLead = tier(1, 250, 1.5F, 5.5F, 25, NO_REPAIR);
    public static final Tier enumToolMaterialBottleOpener = tier(1, 250, 1.5F, 0.5F, 200, REPAIR_PLATE_STEEL);
    public static final Tier enumToolMaterialSledge = tier(1, 0, 25.0F, 26F, 200, NO_REPAIR);
    public static final Tier enumToolMaterialMultitool = tier(3, 5000, 25F, 5.5F, 25, NO_REPAIR);
    public static final Tier matMeteorite = tier(4, 0, 50F, 0.0F, 200, REPAIR_PLATE_PAA);
    public static final Tier matCrucible = tier(3, 10000, 50.0F, 100.0F, 200, NO_REPAIR);
    // CE defines matHS and matHF as exact duplicates of matCrucible (same "CRUCIBLE" EnumHelper name and values);
    // preserved verbatim rather than silently deduplicated, since that is what CE's actual behavior is.
    public static final Tier matHS = tier(3, 10000, 50.0F, 100.0F, 200, NO_REPAIR);
    public static final Tier matHF = tier(3, 10000, 50.0F, 100.0F, 200, NO_REPAIR);

    /**
     * Registers every {@link ArmorMaterial} with the mod event bus. Tool {@link Tier}s need no registration -
     * they are plain objects in NeoForge, not registry entries.
     */
    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
    }

    /**
     * CE's durability multiplier (its EnumHelper's first numeric argument) no longer lives on
     * {@link ArmorMaterial} itself in 1.21 - the items area needs this value to compute
     * {@code ArmorItem.Type.getDurability(multiplier)} when constructing the actual armor items.
     */
    public static int getDurabilityMultiplier(Holder<ArmorMaterial> material) {
        return DURABILITY_MULTIPLIERS.getOrDefault(material, 15);
    }

    private static Holder<ArmorMaterial> armor(String id, String texture, int durabilityMultiplier, int[] defense,
                                                int enchantmentValue, float toughness, Supplier<Ingredient> repairIngredient) {
        Map<ArmorItem.Type, Integer> defenseByType = new EnumMap<>(ArmorItem.Type.class);
        defenseByType.put(ArmorItem.Type.BOOTS, defense[0]);
        defenseByType.put(ArmorItem.Type.LEGGINGS, defense[1]);
        defenseByType.put(ArmorItem.Type.CHESTPLATE, defense[2]);
        defenseByType.put(ArmorItem.Type.HELMET, defense[3]);

        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, texture)));

        Holder<ArmorMaterial> holder = ARMOR_MATERIALS.register(id, () ->
                new ArmorMaterial(defenseByType, enchantmentValue, SoundEvents.ARMOR_EQUIP_GENERIC, repairIngredient, layers, toughness, 0.0F));

        DURABILITY_MULTIPLIERS.put(holder, durabilityMultiplier);
        return holder;
    }

    private static Tier tier(int harvestLevel, int maxUses, float efficiency, float attackDamageBonus,
                              int enchantmentValue, Supplier<Ingredient> repairIngredient) {
        return new SimpleTier(incorrectBlocksFor(harvestLevel), maxUses, efficiency, attackDamageBonus, enchantmentValue, repairIngredient);
    }

    private static TagKey<Block> incorrectBlocksFor(int harvestLevel) {
        return switch (harvestLevel) {
            case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }

    private static Supplier<Ingredient> repairTag(String name) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "repair/" + name));
        return () -> Ingredient.of(tag);
    }
}
