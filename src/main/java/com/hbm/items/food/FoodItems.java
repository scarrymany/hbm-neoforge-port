package com.hbm.items.food;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemChemicalDye;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Registers every Phase-1-safe {@code com.hbm.items.food} item (see
 * docs/phase1/items_food_gear.md). Mirrors the shape of {@code MachineItems}/{@code GearItems}: a
 * plain registration class whose {@link #registerAll} is called from {@code ModItems#register},
 * exposing the one {@code DeferredItem} field ({@link #CAN_KEY}) another class in this same package
 * ({@link ItemConserve}) needs at runtime.
 * <p>
 * The existing, previously-orphaned {@link ItemLemon} is this class's biggest single contributor: its
 * javadoc already documents that its ~19 food-catalog fields (everything except
 * {@code ingot_semtex}/{@code powder_cement}/{@code bio_wafer}, which land in the PARTS tab via other
 * Phase 1 areas) are registered here, entirely through {@code FoodProperties.Builder#effect}/
 * {@code #usingConvertsTo} - no Java-side {@code onFoodEaten} override needed for any of them.
 * <p>
 * Four CE metadata-multi classes are flattened into distinct registry entries per the research
 * report's table: {@link ItemConserve} (27 {@code canned_*} items), crayons (16
 * {@code crayon_<color>} items, keyed off {@link ItemChemicalDye.EnumChemDye} - see
 * {@link #registerCrayon}), and {@link ItemAppleSchrabidium}/{@link ItemTemFlakes} (6 and 3 items,
 * respectively, using a {@code _low}/{@code _mid}/{@code _high} tier-name suffix convention CE itself
 * never had - see those classes' javadocs).
 * <p>
 * {@link ItemEnergy}, {@link ItemPill}, and {@link ItemCanteen} are registered fully working per the
 * research report's sequencing recommendation. {@link ItemPill}'s {@code HbmPotion}/
 * {@code VersatileConfig.applyPotionSickness} branches are now wired against
 * {@code com.hbm.potion.HbmPotionEffects} (see that class's own javadoc); every remaining branch
 * that would call into the still-unported {@code HbmLivingProps}/{@code ContaminationUtil} facade is
 * left as an explicit TODO on that item class rather than silently dropped or stubbed to a no-op
 * without comment.
 */
public final class FoodItems {

    private FoodItems() {
    }

    // ==================== fields other classes reference ====================

    /** {@link ItemConserve#finishUsingItem} gives one of these on every can eaten, matching CE. */
    public static DeferredItem<Item> CAN_KEY;

    // Plain container items ItemEnergy's can/bottle-swap-on-drink logic needs to exist as real
    // registry entries - see registerContainers()'s javadoc below for why these are registered here
    // even though none of them come from one of this area's 16 CE class files.
    private static DeferredItem<Item> CAN_EMPTY;
    private static DeferredItem<Item> RING_PULL;
    private static DeferredItem<Item> BOTTLE_EMPTY;
    private static DeferredItem<Item> BOTTLE2_EMPTY;
    private static DeferredItem<Item> CAP_NUKA;
    private static DeferredItem<Item> CAP_QUANTUM;
    private static DeferredItem<Item> CAP_SPARKLE;
    private static DeferredItem<Item> CAP_RAD;
    private static DeferredItem<Item> CAP_KORL;

    /**
     * @param modEventBus unused by this area today (its one custom tab, CONSUMABLE, is populated via
     *                     the shared {@link CreativeTabContents} accumulator, not a per-caller
     *                     {@code BuildCreativeModeTabContentsEvent} listener like {@code GearItems}
     *                     needs) - accepted anyway to match the {@code FoodItems.registerAll(modEventBus)}
     *                     call site wired into {@code ModItems#register}.
     */
    public static void registerAll(IEventBus modEventBus) {
        registerContainers();
        registerFoodSoup();
        registerMuchoMango();
        registerNugget();
        registerAppleEuphemium();
        registerBDCL();
        registerFlask();
        registerLemonCatalog();
        registerConserve();
        registerCrayon();
        registerAppleSchrabidium();
        registerTemFlakes();
        registerEnergy();
        registerPill();
        registerCanteen();
    }

    // ==================== plain consumable containers (see fields above) ====================

    /**
     * CE declares {@code can_empty}/{@code ring_pull}/{@code bottle_empty}/{@code bottle2_empty}/
     * {@code cap_nuka}/{@code cap_quantum}/{@code cap_sparkle}/{@code cap_rad}/{@code cap_korl}/
     * {@code can_key} as bare {@code ItemBase} fields directly inline in its monolithic
     * {@code ModItems.java} - none of them are declared by one of this area's 16 CE class files, so
     * strictly speaking they belong to whichever area ends up owning "generic consumable containers."
     * They are registered here anyway, rather than left out, because {@link ItemEnergy#makeCan}/
     * {@link ItemEnergy#makeBottle} and {@link ItemConserve}'s can-key giveaway are meaningless without
     * a real item to hand back - registering the food items but leaving their container dependency
     * unregistered would be a worse outcome than this small, self-contained scope extension.
     * <p>
     * {@code cap_fritz} (CE also declares it) is deliberately skipped: no CE {@code ItemEnergy}
     * instance actually constructs with it ({@code bottle2_fritz} uses {@code cap_korl} instead per
     * CE's own {@code ModItems.java}), so nothing in this area's scope needs it.
     */
    private static void registerContainers() {
        CAN_EMPTY = tab(reg("can_empty", () -> new ItemBase(props())));
        RING_PULL = tab(reg("ring_pull", () -> new ItemBase(props())));
        BOTTLE_EMPTY = tab(reg("bottle_empty", () -> new ItemBase(props())));
        BOTTLE2_EMPTY = tab(reg("bottle2_empty", () -> new ItemBase(props())));
        CAP_NUKA = tab(reg("cap_nuka", () -> new ItemBase(props())));
        CAP_QUANTUM = tab(reg("cap_quantum", () -> new ItemBase(props())));
        CAP_SPARKLE = tab(reg("cap_sparkle", () -> new ItemBase(props())));
        CAP_RAD = tab(reg("cap_rad", () -> new ItemBase(props())));
        CAP_KORL = tab(reg("cap_korl", () -> new ItemBase(props())));
        CAN_KEY = tab(reg("can_key", () -> new ItemBase(props())));
    }

    // ==================== ItemFoodSoup (3 instances) ====================
    // Thin vanilla-ItemSoup-equivalent wrapper (mushroom-stew-style, returns an empty bowl); no
    // onFoodEaten override in CE, so a plain Item + FoodProperties covers it with no dedicated class.

    private static void registerFoodSoup() {
        registerSoup("glowing_stew");
        registerSoup("balefire_scrambled");
        registerSoup("balefire_and_ham");
    }

    private static void registerSoup(String name) {
        tab(reg(name, () -> new Item(new Item.Properties().stacksTo(1)
                .food(foodBuilder(6, 0.6F).usingConvertsTo(Items.BOWL).build()))));
    }

    // ==================== ItemMuchoMango ====================

    private static void registerMuchoMango() {
        tab(reg("mucho_mango", () -> new ItemMuchoMango(new Item.Properties()
                .food(foodBuilder(10, 0.6F).alwaysEdible()
                        .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0), 1.0F)
                        .build()))));
    }

    // ==================== ItemNugget ====================
    // CE has only one real instance (gun_moist_nugget); the tooltip joke is baked directly into the
    // class since there is nothing to switch on.

    private static void registerNugget() {
        tab(reg("gun_moist_nugget", () -> new ItemNugget(food(3, 0.6F))));
    }

    // ==================== ItemAppleEuphemium ====================

    private static void registerAppleEuphemium() {
        tab(reg("apple_euphemium", () -> new ItemAppleEuphemium(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .food(foodBuilder(20, 100F).alwaysEdible()
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 127), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.SATURATION, Integer.MAX_VALUE, 127), 1.0F)
                        .build()))));
    }

    // ==================== ItemBDCL ====================

    private static void registerBDCL() {
        tab(reg("bdcl", () -> new ItemBDCL(props())));
    }

    // ==================== ItemFlask (de-generified to one plain item) ====================

    private static void registerFlask() {
        tab(reg("flask_infusion", () -> new ItemFlask(props())));
    }

    // ==================== ItemLemon food-catalog fields ====================
    // See ItemLemon's own javadoc: ingot_semtex/powder_cement/bio_wafer are registered by other Phase
    // 1 areas (PARTS tab material resources) and are NOT repeated here.

    private static void registerLemonCatalog() {
        tab(reg("lemon", () -> new ItemLemon(food(3, 5F))));
        tab(reg("definitelyfood", () -> new ItemLemon(food(3, 0.5F))));
        tab(reg("med_ipecac", () -> new ItemLemon(new Item.Properties().food(foodBuilder(0, 0F)
                .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 50, 49), 1.0F).build()))));
        tab(reg("med_ptsd", () -> new ItemLemon(new Item.Properties().food(foodBuilder(0, 0F)
                .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 50, 49), 1.0F).build()))));
        // CE hides this from creative (setCreativeTab(null)); still a real registered item.
        reg("med_schizophrenia", () -> new ItemLemon(food(0, 0F)));
        tab(reg("loops", () -> new ItemLemon(food(4, 5F))));
        tab(reg("loop_stew", () -> new ItemLemon(new Item.Properties().stacksTo(1).food(foodBuilder(10, 10F)
                .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1), 1.0F)
                .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 2), 1.0F)
                .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 1), 1.0F)
                .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 20, 2), 1.0F)
                .usingConvertsTo(Items.BOWL)
                .build()))));
        // CE hides this from creative (setCreativeTab(null)); still a real registered item.
        reg("fooditem", () -> new ItemLemon(food(2, 5F)));
        tab(reg("twinkie", () -> new ItemLemon(food(3, 5F))));
        tab(reg("static_sandwich", () -> new ItemLemon(food(6, 5F))));
        tab(reg("nugget", () -> new ItemLemon(food(200, 200F))));
        tab(reg("marshmallow", () -> new ItemLemon(new Item.Properties().stacksTo(1).food(foodBuilder(2, 2F).build()))));
        tab(reg("cheese", () -> new ItemLemon(food(5, 0.75F))));
        tab(reg("cheese_quesadilla", () -> new ItemLemon(food(8, 1F))));
        tab(reg("marshmallow_roasted", () -> new ItemLemon(new Item.Properties().stacksTo(1).food(foodBuilder(6, 6F).build()))));
        // CE's isWolfsFavoriteMeat flag has no direct FoodProperties equivalent in 1.21 (wolf taming
        // is a minecraft:wolf_food item tag now, a datagen concern outside this class's scope).
        tab(reg("glyphid_meat", () -> new ItemLemon(food(3, 0.5F))));
        tab(reg("glyphid_meat_grilled", () -> new ItemLemon(new Item.Properties().food(foodBuilder(8, 0.75F)
                .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 180, 1), 1.0F).build()))));
        tab(reg("spongebob_macaroni", () -> new ItemLemon(food(5, 5F))));
        tab(reg("pudding", () -> new ItemLemon(food(6, 15F))));
    }

    // ==================== ItemConserve (27 flattened variants) ====================

    private static void registerConserve() {
        for (ItemConserve.FoodType type : ItemConserve.FoodType.values()) {
            tab(reg("canned_" + lower(type.name()), () -> new ItemConserve(type,
                    new Item.Properties().food(foodBuilder(type.foodLevel, type.saturation).build()))));
        }
    }

    // ==================== ItemCrayon (16 flattened variants) ====================
    // CE: single "crayon" field, alwaysEdible, 16 ItemChemicalDye.EnumChemDye color variants. No
    // onFoodEaten override in CE at all, so nothing beyond the color field (kept for lore/tooltip
    // lookups, mirroring ItemChemicalDye's own EnumChemDye field) is needed.

    private static void registerCrayon() {
        for (ItemChemicalDye.EnumChemDye color : ItemChemicalDye.EnumChemDye.VALUES) {
            tab(reg("crayon_" + lower(color.name()), () -> new ItemCrayon(color,
                    new Item.Properties().food(foodBuilder(3, 0.6F).alwaysEdible().build()))));
        }
    }

    // ==================== ItemAppleSchrabidium (2 bases x 3 tiers, flattened to 6) ====================

    private static void registerAppleSchrabidium() {
        // apple_schrabidium: plain vanilla MobEffect bundles per tier - fully Phase-1-safe.
        tab(reg("apple_schrabidium_low", () -> new ItemAppleSchrabidium(
                new Item.Properties().rarity(Rarity.UNCOMMON).food(foodBuilder(20, 100F).alwaysEdible()
                        .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0F)
                        .build()),
                false, false)));
        tab(reg("apple_schrabidium_mid", () -> new ItemAppleSchrabidium(
                new Item.Properties().rarity(Rarity.RARE).food(foodBuilder(20, 100F).alwaysEdible()
                        .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 1200, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 1200, 2), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 2), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.JUMP, 1200, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 1200, 9), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 1200, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.SATURATION, 1200, 9), 1.0F)
                        .build()),
                false, false)));
        tab(reg("apple_schrabidium_high", () -> new ItemAppleSchrabidium(
                new Item.Properties().rarity(Rarity.EPIC).food(foodBuilder(20, 100F).alwaysEdible()
                        .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 9), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, Integer.MAX_VALUE, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 3), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.JUMP, Integer.MAX_VALUE, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, Integer.MAX_VALUE, 24), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, Integer.MAX_VALUE, 14), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.SATURATION, Integer.MAX_VALUE, 99), 1.0F)
                        .build()),
                true, false)));

        // apple_lead: low/mid tiers grant com.hbm.potion.HbmPotionEffects.LEAD (CE's real numbers,
        // upstream hbm-ce ItemAppleSchrabidium#onFoodEaten: 15*20 ticks amp 2 / 60*20 ticks amp 4).
        // High tier is lethal instead (500 damage via the already-ported ModDamageTypes.LEAD),
        // implemented directly in ItemAppleSchrabidium#finishUsingItem.
        tab(reg("apple_lead_low", () -> new ItemAppleSchrabidium(
                new Item.Properties().rarity(Rarity.UNCOMMON).food(foodBuilder(5, 0F).alwaysEdible().build()),
                false, false, 15 * 20, 2)));
        tab(reg("apple_lead_mid", () -> new ItemAppleSchrabidium(
                new Item.Properties().rarity(Rarity.RARE).food(foodBuilder(5, 0F).alwaysEdible().build()),
                false, false, 60 * 20, 4)));
        tab(reg("apple_lead_high", () -> new ItemAppleSchrabidium(
                new Item.Properties().rarity(Rarity.EPIC).food(foodBuilder(5, 0F).alwaysEdible().build()),
                true, true)));
    }

    // ==================== ItemTemFlakes (1 base x 3 tiers, flattened to 3) ====================

    private static void registerTemFlakes() {
        tab(reg("tem_flakes_low", () -> new ItemTemFlakes(new Item.Properties()
                .food(foodBuilder(0, 0F).alwaysEdible().build()))));
        tab(reg("tem_flakes_mid", () -> new ItemTemFlakes(new Item.Properties()
                .food(foodBuilder(0, 0F).alwaysEdible().build()))));
        tab(reg("tem_flakes_high", () -> new ItemTemFlakes(new Item.Properties()
                .food(foodBuilder(0, 0F).alwaysEdible().build()))));
    }

    // ==================== ItemEnergy (~24 instances) ====================

    private static void registerEnergy() {
        registerCan("can_smart");
        registerCan("can_creature");
        registerCan("can_redbomb");
        registerCan("can_mrsugar");
        registerCan("can_overcharge");
        registerCan("can_luna");
        registerCan("can_bepis");
        registerCan("can_breen");
        registerCan("can_mug");

        registerBottle("bottle_nuka", BOTTLE_EMPTY, CAP_NUKA);
        registerBottle("bottle_cherry", BOTTLE_EMPTY, CAP_NUKA);
        registerBottle("bottle_quantum", BOTTLE_EMPTY, CAP_QUANTUM);
        registerBottle("bottle_sparkle", BOTTLE_EMPTY, CAP_SPARKLE);
        registerBottle("bottle_rad", BOTTLE_EMPTY, CAP_RAD);
        registerBottle("bottle2_korl", BOTTLE2_EMPTY, CAP_KORL);
        registerBottle("bottle2_fritz", BOTTLE2_EMPTY, CAP_KORL);

        registerPlainEnergy("bottle2_korl_special");
        registerPlainEnergy("bottle2_fritz_special");
        registerPlainEnergy("bottle2_sunset");
        registerPlainEnergy("chocolate_milk");
        registerPlainEnergy("coffee");
        registerPlainEnergy("coffee_radium");
    }

    private static void registerCan(String name) {
        tab(reg(name, () -> new ItemEnergy(props()).makeCan(CAN_EMPTY, RING_PULL)));
    }

    private static void registerBottle(String name, Supplier<? extends Item> container, Supplier<? extends Item> cap) {
        tab(reg(name, () -> new ItemEnergy(props()).makeBottle(container, cap)));
    }

    private static void registerPlainEnergy(String name) {
        tab(reg(name, () -> new ItemEnergy(props())));
    }

    // ==================== ItemPill (10 instances; report's "~13" was an over-estimate - confirmed
    // by direct source inspection: exactly 10 "new ItemPill(...)" call sites exist in CE's
    // ModItems.java) ====================

    private static void registerPill() {
        tab(reg("radx", () -> new ItemPill(pillProps())));
        tab(reg("siox", () -> new ItemPill(pillProps())));
        tab(reg("pill_herbal", () -> new ItemPill(pillProps())));
        tab(reg("xanax", () -> new ItemPill(pillProps())));
        tab(reg("fmn", () -> new ItemPill(pillProps())));
        tab(reg("five_htp", () -> new ItemPill(pillProps())));
        tab(reg("pill_iodine", () -> new ItemPill(pillProps())));
        tab(reg("plan_c", () -> new ItemPill(pillProps())));
        tab(reg("pill_red", () -> new ItemPill(pillProps())));
        tab(reg("chocolate", () -> new ItemPill(pillProps())));
    }

    private static Item.Properties pillProps() {
        return new Item.Properties().food(foodBuilder(0, 0.6F).alwaysEdible().build());
    }

    // ==================== ItemCanteen (3 instances) ====================
    // Constructor argument is CE's cooldown in seconds (matches CE's own new ItemCanteen(int, String)
    // call sites verbatim), not ticks - see ItemCanteen's javadoc.

    private static void registerCanteen() {
        tab(reg("canteen_13", () -> new ItemCanteen(60, props())));
        tab(reg("canteen_vodka", () -> new ItemCanteen(3 * 60, props())));
        tab(reg("canteen_fab", () -> new ItemCanteen(2 * 60, props())));
    }

    // ==================== shared helpers ====================

    private static Item.Properties props() {
        return new Item.Properties();
    }

    private static FoodProperties.Builder foodBuilder(int nutrition, float saturationModifier) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturationModifier);
    }

    private static Item.Properties food(int nutrition, float saturationModifier) {
        return new Item.Properties().food(foodBuilder(nutrition, saturationModifier).build());
    }

    private static DeferredItem<Item> reg(String name, Supplier<? extends Item> factory) {
        return ModItems.ITEMS.register(name, factory);
    }

    private static <T extends Item> DeferredItem<T> tab(DeferredItem<T> item) {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, item);
        return item;
    }

    private static String lower(String enumName) {
        return enumName.toLowerCase(Locale.ROOT);
    }
}
