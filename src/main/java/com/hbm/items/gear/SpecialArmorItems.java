package com.hbm.items.gear;

import com.hbm.items.ModItems;
import com.hbm.main.MaterialRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registers the 33 items covered by this package's task brief
 * ({@code docs/phase3/armor_special_sets.md}): {@code ArmorEuphemium} (4), {@code ArmorGasMask} (4),
 * {@code ArmorHazmat} (12), {@code ArmorHazmatMask} (4), {@code MaskOfInfamy} (1), plus the 8
 * asbestos/schrabidium items built directly on the already-ported {@code ArmorFSB} rather than the
 * two dead CE leaf classes of the same conceptual name (see {@code docs/phase3/armor_special_sets.md}
 * Headline finding #1 - confirmed by reading the real, current CE {@code ModItems.java} declaration
 * sites for all 8 items: {@code asbestos_helmet/_plate/_legs/_boots} get no builder calls beyond a
 * (not-reproduced, Phase 5) helmet overlay, and {@code schrabidium_helmet/_plate/_legs/_boots} get
 * only 4 {@code .addEffect(...)} full-suit potion calls - neither set's real live behavior includes
 * any damage-absorption/self-damage mechanic, resolving this package's open research question with
 * direct evidence rather than assumption).
 *
 * <p>Creative-tab placement follows CE's actual final state at each item's real {@code ModItems.java}
 * declaration site (not just each class's own constructor default) - confirmed per-item:
 * <ul>
 *     <li>{@code gas_mask*} (4) and the hazmat family (16) - vanilla {@code CreativeModeTabs.COMBAT}
 *     (CE: {@code ArmorGasMask}/{@code ArmorHazmat} constructors call
 *     {@code setCreativeTab(CreativeTabs.COMBAT)}, never overridden at the declaration site).</li>
 *     <li>{@code asbestos_*}/{@code schrabidium_*} (8) - no tab at all: CE's {@code ArmorFSB}
 *     constructor never calls {@code setCreativeTab}, and neither declaration site overrides it.</li>
 *     <li>{@code euphemium_*} (4) - no tab: CE's {@code ArmorEuphemium} constructor explicitly calls
 *     {@code setCreativeTab(null)}.</li>
 *     <li>{@code mask_of_infamy} (1) - no tab: no {@code setCreativeTab} call anywhere for it in CE.</li>
 * </ul>
 */
public final class SpecialArmorItems {

    private SpecialArmorItems() {
    }

    private static final List<Supplier<? extends ItemLike>> COMBAT_TAB = new ArrayList<>();

    public static void registerAll(IEventBus modEventBus) {
        modEventBus.addListener(SpecialArmorItems::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            COMBAT_TAB.forEach(item -> event.accept(item.get()));
        }
    }

    /**
     * Per {@code com.hbm.main.MaterialRegistry}'s own javadoc: 1.21 bakes durability into
     * {@code Item.Properties} at construction time via {@code ArmorItem.Type#getDurability(int)},
     * fed by the material's CE-derived durability multiplier.
     */
    private static Item.Properties props(Holder<ArmorMaterial> material, ArmorItem.Type type) {
        return new Item.Properties().stacksTo(1)
                .durability(type.getDurability(MaterialRegistry.getDurabilityMultiplier(material)));
    }

    // ==================== Euphemium (4) - ArmorEuphemium, full-set damage immunity ====================
    // CE ModItems.java:482-485. EPIC rarity (CE: getRarity override); no durability component at all
    // (see ArmorEuphemium's javadoc for why that - not Item.Properties#durability(Integer.MAX_VALUE) -
    // is the faithful translation of CE's indestructibility overrides); no creative tab (CE ctor:
    // setCreativeTab(null), no override at the declaration site).

    public static final DeferredItem<Item> EUPHEMIUM_HELMET = ModItems.ITEMS.register("euphemium_helmet",
            () -> new ArmorEuphemium(MaterialRegistry.aMatEuph, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> EUPHEMIUM_PLATE = ModItems.ITEMS.register("euphemium_plate",
            () -> new ArmorEuphemium(MaterialRegistry.aMatEuph, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> EUPHEMIUM_LEGS = ModItems.ITEMS.register("euphemium_legs",
            () -> new ArmorEuphemium(MaterialRegistry.aMatEuph, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> EUPHEMIUM_BOOTS = ModItems.ITEMS.register("euphemium_boots",
            () -> new ArmorEuphemium(MaterialRegistry.aMatEuph, ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ==================== Gas masks (4) - ArmorGasMask, HEAD-only, IGasMask ====================
    // CE ModItems.java:724-727, ArmorMaterial.IRON, vanilla COMBAT tab.

    public static final DeferredItem<Item> GAS_MASK = gasMask("gas_mask",
            List.of(HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT));
    public static final DeferredItem<Item> GAS_MASK_M65 = gasMask("gas_mask_m65",
            List.of(HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT));
    public static final DeferredItem<Item> GAS_MASK_MONO = gasMask("gas_mask_mono",
            List.of(HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT, HazardClass.BACTERIA));
    public static final DeferredItem<Item> GAS_MASK_OLDE = gasMask("gas_mask_olde",
            List.of(HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT));

    private static DeferredItem<Item> gasMask(String name, List<HazardClass> blacklist) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () ->
                new ArmorGasMask(ArmorMaterials.IRON, ArmorItem.Type.HELMET, props(ArmorMaterials.IRON, ArmorItem.Type.HELMET), blacklist));
        COMBAT_TAB.add(item);
        return item;
    }

    // ==================== Hazmat suits (12) - ArmorHazmat, no special behavior ====================
    // CE ModItems.java:448-470, vanilla COMBAT tab.

    public static final DeferredItem<Item> HAZMAT_PLATE = hazmat("hazmat_plate", MaterialRegistry.aMatHaz, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> HAZMAT_LEGS = hazmat("hazmat_legs", MaterialRegistry.aMatHaz, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> HAZMAT_BOOTS = hazmat("hazmat_boots", MaterialRegistry.aMatHaz, ArmorItem.Type.BOOTS);
    public static final DeferredItem<Item> HAZMAT_PLATE_RED = hazmat("hazmat_plate_red", MaterialRegistry.aMatHaz2, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> HAZMAT_LEGS_RED = hazmat("hazmat_legs_red", MaterialRegistry.aMatHaz2, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> HAZMAT_BOOTS_RED = hazmat("hazmat_boots_red", MaterialRegistry.aMatHaz2, ArmorItem.Type.BOOTS);
    public static final DeferredItem<Item> HAZMAT_PLATE_GREY = hazmat("hazmat_plate_grey", MaterialRegistry.aMatHaz3, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> HAZMAT_LEGS_GREY = hazmat("hazmat_legs_grey", MaterialRegistry.aMatHaz3, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> HAZMAT_BOOTS_GREY = hazmat("hazmat_boots_grey", MaterialRegistry.aMatHaz3, ArmorItem.Type.BOOTS);
    public static final DeferredItem<Item> HAZMAT_PAA_PLATE = hazmat("hazmat_paa_plate", MaterialRegistry.aMatPaa, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> HAZMAT_PAA_LEGS = hazmat("hazmat_paa_legs", MaterialRegistry.aMatPaa, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> HAZMAT_PAA_BOOTS = hazmat("hazmat_paa_boots", MaterialRegistry.aMatPaa, ArmorItem.Type.BOOTS);

    private static DeferredItem<Item> hazmat(String name, Holder<ArmorMaterial> material, ArmorItem.Type type) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ArmorHazmat(material, type, props(material, type)));
        COMBAT_TAB.add(item);
        return item;
    }

    // ==================== Hazmat helmets (4) - ArmorHazmatMask extends ArmorHazmat, + IGasMask ====================
    // CE ModItems.java:448-467. Only _red/_grey get the shared custom model in CE.

    public static final DeferredItem<Item> HAZMAT_HELMET = hazmatMask("hazmat_helmet", MaterialRegistry.aMatHaz, false);
    public static final DeferredItem<Item> HAZMAT_HELMET_RED = hazmatMask("hazmat_helmet_red", MaterialRegistry.aMatHaz2, true);
    public static final DeferredItem<Item> HAZMAT_HELMET_GREY = hazmatMask("hazmat_helmet_grey", MaterialRegistry.aMatHaz3, true);
    public static final DeferredItem<Item> HAZMAT_PAA_HELMET = hazmatMask("hazmat_paa_helmet", MaterialRegistry.aMatPaa, false);

    private static DeferredItem<Item> hazmatMask(String name, Holder<ArmorMaterial> material, boolean hasCustomModel) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () ->
                new ArmorHazmatMask(material, ArmorItem.Type.HELMET, props(material, ArmorItem.Type.HELMET), hasCustomModel));
        COMBAT_TAB.add(item);
        return item;
    }

    // ==================== Asbestos (4) - real live CE behavior is plain ArmorFSB ====================
    // CE ModItems.java:478-481: helmet gets .setOverlay(...) (Phase 5 rendering, not reproduced - the
    // ported ArmorFSB has no overlay mechanism, see class javadoc above); plate/legs/boots get no
    // builder calls at all. No fire immunity, no potion effects, no hazard-class registration of
    // their own beyond what ArmorUtil.register() wires for the helmet. No creative tab.

    public static final DeferredItem<Item> ASBESTOS_HELMET = ModItems.ITEMS.register("asbestos_helmet",
            () -> new ArmorFSB(MaterialRegistry.aMatAsbestos, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatAsbestos, ArmorItem.Type.HELMET)));
    public static final DeferredItem<Item> ASBESTOS_PLATE = ModItems.ITEMS.register("asbestos_plate",
            () -> new ArmorFSB(MaterialRegistry.aMatAsbestos, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatAsbestos, ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<Item> ASBESTOS_LEGS = ModItems.ITEMS.register("asbestos_legs",
            () -> new ArmorFSB(MaterialRegistry.aMatAsbestos, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatAsbestos, ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<Item> ASBESTOS_BOOTS = ModItems.ITEMS.register("asbestos_boots",
            () -> new ArmorFSB(MaterialRegistry.aMatAsbestos, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatAsbestos, ArmorItem.Type.BOOTS)));

    // ==================== Schrabidium (4) - real live CE behavior is ArmorFSB + 4 full-suit effects ====================
    // CE ModItems.java:534-541: helmet gets 4 .addEffect(...) calls (Haste II, Strength II, Jump
    // Boost I, Speed II, each a 20-tick refresh via ArmorFSB.handleTick - not yet wired to any tick
    // event by the ArmorFSB package itself, see that class's own javadoc); plate/legs/boots
    // .cloneStats(helmet) in CE. Reproduced here as identical per-piece .addEffect calls (via
    // schrabidiumEffects) rather than a live .cloneStats(...) call against the helmet's
    // DeferredItem, to avoid relying on cross-item DeferredItem#get() succeeding before the
    // registry event has actually populated it - functionally identical either way (ArmorFSB.effects
    // ends up holding the same 4-entry list on all 4 pieces). No capped-absorption/self-damage
    // mechanic - confirmed NOT present in ArmorFSB's real builder-call chain for these 4 items (only
    // .addEffect is called; that mechanic lived exclusively in the dead ArmorSchrabidium.java).

    public static final DeferredItem<Item> SCHRABIDIUM_HELMET = ModItems.ITEMS.register("schrabidium_helmet",
            () -> schrabidiumEffects(new ArmorFSB(MaterialRegistry.aMatSchrab, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatSchrab, ArmorItem.Type.HELMET))));
    public static final DeferredItem<Item> SCHRABIDIUM_PLATE = ModItems.ITEMS.register("schrabidium_plate",
            () -> schrabidiumEffects(new ArmorFSB(MaterialRegistry.aMatSchrab, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatSchrab, ArmorItem.Type.CHESTPLATE))));
    public static final DeferredItem<Item> SCHRABIDIUM_LEGS = ModItems.ITEMS.register("schrabidium_legs",
            () -> schrabidiumEffects(new ArmorFSB(MaterialRegistry.aMatSchrab, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatSchrab, ArmorItem.Type.LEGGINGS))));
    public static final DeferredItem<Item> SCHRABIDIUM_BOOTS = ModItems.ITEMS.register("schrabidium_boots",
            () -> schrabidiumEffects(new ArmorFSB(MaterialRegistry.aMatSchrab, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatSchrab, ArmorItem.Type.BOOTS))));

    private static ArmorFSB schrabidiumEffects(ArmorFSB armor) {
        // MobEffects.DIG_SPEED/DAMAGE_BOOST/JUMP/MOVEMENT_SPEED are Mojang's real internal names for
        // Haste/Strength/Jump Boost/Speed - already-established convention in this port (see
        // ArmorFSB.addEffect's own javadoc for MobEffects.JUMP specifically).
        armor.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20, 2, false, true));
        armor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 2, false, true));
        armor.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 1, false, true));
        armor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 2, false, true));
        return armor;
    }

    // ==================== Mask of Infamy (1) - trivial cosmetic ====================
    // CE ModItems.java:1710, ArmorMaterial.IRON. No creative tab, no recipe (see MaskOfInfamy's javadoc).

    public static final DeferredItem<Item> MASK_OF_INFAMY = ModItems.ITEMS.register("mask_of_infamy",
            () -> new MaskOfInfamy(ArmorMaterials.IRON, ArmorItem.Type.HELMET, props(ArmorMaterials.IRON, ArmorItem.Type.HELMET)));
}
