package com.hbm.items.armor;

import com.hbm.handler.ArmorUtil;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MaterialRegistry;
import com.hbm.potion.HbmPotionEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registers the {@link ArmorFSBPowered}/{@link ArmorFSBFueled}/plain-{@link ArmorFSB} concrete
 * power-armor leaf sets this package's task brief names (grep-confirmed CE census against
 * {@code upstream/hbm-ce/src/main/java/com/hbm/items/armor}, cross-checked line-by-line against
 * each set's real {@code ModItems.java} construction site - every stat number, potion effect,
 * hazard class, and rad-resist value below is transcribed from that real source, not guessed):
 * {@code ArmorLiquidator} (4), {@code ArmorT51} (4), {@code ArmorDesh}/steamsuit (4),
 * {@code ArmorTrenchmaster} (4), {@code ArmorTaurun} (4), {@code ArmorBismuth} (4),
 * {@code ArmorEnvsuit} (4), {@code ArmorDiesel}/dieselsuit (4), {@code ArmorAJR} (4),
 * {@code ArmorAJRO} (4), {@code ArmorHEV} (4), {@code ArmorBJ}(+{@code ArmorBJJetpack}) (5),
 * {@code ArmorRPA} (4), {@code ArmorNCRPA} (4), {@code ArmorDigamma}/fau (4), {@code ArmorDNT}/dns
 * (4), {@code ArmorHat} (1) - 66 items total.
 *
 * <p>Per-piece builder chains are written once per set as a private static helper applied
 * identically to all 4 pieces, rather than a live {@code .cloneStats(helmet.get())} call against a
 * sibling {@link DeferredItem} - same reasoning as {@code com.hbm.items.gear.SpecialArmorItems}
 * ("to avoid relying on cross-item {@code DeferredItem#get()} succeeding before the registry event
 * has actually populated it"): functionally identical to CE's real {@code cloneStats}, since that
 * method only copies the same field values this helper sets directly.
 *
 * <p><b>Preserved CE quirks, not fixed</b> (this port's behavior-fidelity mandate - CE is the sole
 * source of truth for behavior, including its own inconsistencies):
 * <ul>
 *     <li>{@code rpa_plate}/{@code rpa_legs}/{@code rpa_boots} pass consumption {@code 10000}, while
 *     {@code rpa_helmet} passes {@code 1000} - four different literal constructor calls in CE's own
 *     {@code ModItems.java}, not a transcription error here.</li>
 *     <li>{@code ncrpa_plate}/{@code ncrpa_legs}/{@code ncrpa_boots} clone their full-set-bonus stat
 *     table (potion effect, hazard class, rad-resist, sounds) from {@code rpa_helmet}, <b>not</b>
 *     from {@code ncrpa_helmet}, in CE's own real source ({@code
 *     .cloneStats((ArmorFSB) rpa_helmet)}) - almost certainly a CE copy-paste bug, but a real one
 *     this port reproduces rather than silently "fixing".</li>
 * </ul>
 *
 * <p>Durability follows each base class's own CE constructor behavior, not a single blanket rule:
 * {@link ArmorFSBPowered}'s CE constructor unconditionally forces {@code setMaxDamage(1)}
 * regardless of material (see that class's own javadoc) - every powered set here uses a flat
 * {@code durability(1)}. Plain {@link ArmorFSB} and {@link ArmorFSBFueled} pieces use the normal
 * material-multiplier-derived durability ({@code com.hbm.main.MaterialRegistry}'s own javadoc-
 * prescribed formula), except {@code ArmorTaurun}/{@code ArmorTrenchmaster}, whose CE constructors
 * explicitly call {@code setMaxDamage(0)} (indestructible) - reproduced as {@code durability(0)}.
 *
 * <p>Creative-tab placement: <b>none</b> of these 62 items get a creative tab, except
 * {@code t51_plate}/{@code t51_legs}/{@code t51_boots} (vanilla {@link CreativeModeTabs#COMBAT}) -
 * confirmed by real per-item inspection of CE's {@code ModItems.java}, not assumed. Unlike modern
 * vanilla, CE's {@link ArmorFSB}/{@link ArmorFSBPowered}/{@link ArmorFSBFueled} constructors never
 * call {@code setCreativeTab} themselves (same finding already established by
 * {@code com.hbm.items.gear.SpecialArmorItems}' javadoc for {@code asbestos_*}/{@code
 * schrabidium_*}), and no declaration site in this batch overrides that <i>except</i> exactly those
 * 3 T51 pieces (note: not {@code t51_helmet} itself) - {@code
 * .cloneStats((ArmorFSB) t51_helmet).setCreativeTab(CreativeTabs.COMBAT)}. Every other item here is
 * real, obtainable CE content (via crafting/loot/commands) that simply never appears in the
 * creative-inventory search, matching CE's own real behavior exactly.
 */
public final class PoweredArmorItems {

    private PoweredArmorItems() {
    }

    private static final List<Supplier<? extends ItemLike>> COMBAT_TAB = new ArrayList<>();

    public static void registerAll(IEventBus modEventBus) {
        modEventBus.addListener(PoweredArmorItems::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            COMBAT_TAB.forEach(item -> event.accept(item.get()));
        }
    }

    private static Item.Properties props(Holder<ArmorMaterial> material, ArmorItem.Type type) {
        return new Item.Properties().stacksTo(1)
                .durability(type.getDurability(MaterialRegistry.getDurabilityMultiplier(material)));
    }

    /** {@link ArmorFSBPowered}'s CE constructor always forces durability 1, regardless of material. */
    private static Item.Properties poweredProps() {
        return new Item.Properties().stacksTo(1).durability(1);
    }

    private static Item.Properties indestructibleProps() {
        return new Item.Properties().stacksTo(1).durability(0);
    }

    /** No creative tab by default - see class javadoc's "Creative-tab placement" note. */
    private static DeferredItem<Item> register(String name, Supplier<? extends Item> factory) {
        return ModItems.ITEMS.register(name, factory);
    }

    /** {@code t51_plate}/{@code t51_legs}/{@code t51_boots} only - see class javadoc. */
    private static DeferredItem<Item> registerCombatTab(String name, Supplier<? extends Item> factory) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, factory);
        COMBAT_TAB.add(item);
        return item;
    }

    private static MobEffectInstance effect(Holder<MobEffect> effectHolder, int duration, int amplifier) {
        return new MobEffectInstance(effectHolder, duration, amplifier, false, true);
    }

    // ==================== Liquidator (4) - plain ArmorFSB + IGasMask ====================
    // CE ModItems.java:460-466.

    public static final DeferredItem<Item> LIQUIDATOR_HELMET = register("liquidator_helmet", () ->
            liquidatorEffects(new ArmorLiquidator(MaterialRegistry.aMatLiquidator, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatLiquidator, ArmorItem.Type.HELMET))));
    public static final DeferredItem<Item> LIQUIDATOR_PLATE = register("liquidator_plate", () ->
            liquidatorEffects(new ArmorLiquidator(MaterialRegistry.aMatLiquidator, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatLiquidator, ArmorItem.Type.CHESTPLATE))));
    public static final DeferredItem<Item> LIQUIDATOR_LEGS = register("liquidator_legs", () ->
            liquidatorEffects(new ArmorLiquidator(MaterialRegistry.aMatLiquidator, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatLiquidator, ArmorItem.Type.LEGGINGS))));
    public static final DeferredItem<Item> LIQUIDATOR_BOOTS = register("liquidator_boots", () ->
            liquidatorEffects(new ArmorLiquidator(MaterialRegistry.aMatLiquidator, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatLiquidator, ArmorItem.Type.BOOTS))));

    private static ArmorLiquidator liquidatorEffects(ArmorLiquidator armor) {
        armor.setStep(HBMSoundHandler.metalStep.get()).setJump(HBMSoundHandler.ironJump.get()).setFall(HBMSoundHandler.ironLand.get());
        return armor;
    }

    // ==================== T51 (4) - ArmorFSBPowered(1000000,10000,1000,5) ====================
    // CE ModItems.java:542-554.

    public static final DeferredItem<Item> T51_HELMET = register("t51_helmet", () ->
            t51Effects(new ArmorT51(MaterialRegistry.enumArmorMaterialT51, ArmorItem.Type.HELMET, poweredProps(), 1000000, 10000, 1000, 5)));
    public static final DeferredItem<Item> T51_PLATE = registerCombatTab("t51_plate", () ->
            t51Effects(new ArmorT51(MaterialRegistry.enumArmorMaterialT51, ArmorItem.Type.CHESTPLATE, poweredProps(), 1000000, 10000, 1000, 5)));
    public static final DeferredItem<Item> T51_LEGS = registerCombatTab("t51_legs", () ->
            t51Effects(new ArmorT51(MaterialRegistry.enumArmorMaterialT51, ArmorItem.Type.LEGGINGS, poweredProps(), 1000000, 10000, 1000, 5)));
    public static final DeferredItem<Item> T51_BOOTS = registerCombatTab("t51_boots", () ->
            t51Effects(new ArmorT51(MaterialRegistry.enumArmorMaterialT51, ArmorItem.Type.BOOTS, poweredProps(), 1000000, 10000, 1000, 5)));

    private static ArmorT51 t51Effects(ArmorT51 armor) {
        armor.enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true)
                .addEffect(effect(MobEffects.DAMAGE_BOOST, 20, 0))
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_NO_LIGHT).setRadResist(1D)
                .setStep(HBMSoundHandler.metalStep.get()).setJump(HBMSoundHandler.ironJump.get()).setFall(HBMSoundHandler.ironLand.get());
        return armor;
    }

    // ==================== Steamsuit / Desh (4) - ArmorFSBFueled(STEAM,64000,500,50,1) ====================
    // CE ModItems.java:555-565.

    public static final DeferredItem<Item> STEAMSUIT_HELMET = register("steamsuit_helmet", () ->
            deshEffects(new ArmorDesh(MaterialRegistry.aMatSteamsuit, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatSteamsuit, ArmorItem.Type.HELMET), Fluids.STEAM, 64000, 500, 50, 1)));
    public static final DeferredItem<Item> STEAMSUIT_PLATE = register("steamsuit_plate", () ->
            deshEffects(new ArmorDesh(MaterialRegistry.aMatSteamsuit, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatSteamsuit, ArmorItem.Type.CHESTPLATE), Fluids.STEAM, 64000, 500, 50, 1)));
    public static final DeferredItem<Item> STEAMSUIT_LEGS = register("steamsuit_legs", () ->
            deshEffects(new ArmorDesh(MaterialRegistry.aMatSteamsuit, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatSteamsuit, ArmorItem.Type.LEGGINGS), Fluids.STEAM, 64000, 500, 50, 1)));
    public static final DeferredItem<Item> STEAMSUIT_BOOTS = register("steamsuit_boots", () ->
            deshEffects(new ArmorDesh(MaterialRegistry.aMatSteamsuit, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatSteamsuit, ArmorItem.Type.BOOTS), Fluids.STEAM, 64000, 500, 50, 1)));

    private static ArmorDesh deshEffects(ArmorDesh armor) {
        armor.addEffect(effect(MobEffects.DIG_SPEED, 30, 0))
                .setHasHardLanding(true)
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(1.3D)
                .setStep(HBMSoundHandler.iron.get()).setJump(HBMSoundHandler.ironJump.get()).setFall(HBMSoundHandler.ironLand.get());
        return armor;
    }

    // ==================== Trenchmaster (4) - plain ArmorFSB, indestructible ====================
    // CE ModItems.java:566-576.

    public static final DeferredItem<Item> TRENCHMASTER_HELMET = register("trenchmaster_helmet", () ->
            trenchmasterEffects(new ArmorTrenchmaster(MaterialRegistry.aMatTrench, ArmorItem.Type.HELMET, indestructibleProps())));
    public static final DeferredItem<Item> TRENCHMASTER_PLATE = register("trenchmaster_plate", () ->
            trenchmasterEffects(new ArmorTrenchmaster(MaterialRegistry.aMatTrench, ArmorItem.Type.CHESTPLATE, indestructibleProps())));
    public static final DeferredItem<Item> TRENCHMASTER_LEGS = register("trenchmaster_legs", () ->
            trenchmasterEffects(new ArmorTrenchmaster(MaterialRegistry.aMatTrench, ArmorItem.Type.LEGGINGS, indestructibleProps())));
    public static final DeferredItem<Item> TRENCHMASTER_BOOTS = register("trenchmaster_boots", () ->
            trenchmasterEffects(new ArmorTrenchmaster(MaterialRegistry.aMatTrench, ArmorItem.Type.BOOTS, indestructibleProps())));

    private static ArmorTrenchmaster trenchmasterEffects(ArmorTrenchmaster armor) {
        armor.setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setRadResist(1D)
                .setHazardClass(ArmorUtil.FULL_PACKAGE)
                .addEffect(effect(MobEffects.DIG_SPEED, 20, 1))
                .addEffect(effect(MobEffects.DAMAGE_BOOST, 20, 2))
                .addEffect(effect(MobEffects.JUMP, 20, 1))
                .addEffect(effect(MobEffects.MOVEMENT_SPEED, 20, 0));
        return armor;
    }

    // ==================== Taurun (4) - plain ArmorFSB, indestructible ====================
    // CE ModItems.java:577-584.

    public static final DeferredItem<Item> TAURUN_HELMET = register("taurun_helmet", () ->
            taurunEffects(new ArmorTaurun(MaterialRegistry.aMatTaurun, ArmorItem.Type.HELMET, indestructibleProps())));
    public static final DeferredItem<Item> TAURUN_PLATE = register("taurun_plate", () ->
            taurunEffects(new ArmorTaurun(MaterialRegistry.aMatTaurun, ArmorItem.Type.CHESTPLATE, indestructibleProps())));
    public static final DeferredItem<Item> TAURUN_LEGS = register("taurun_legs", () ->
            taurunEffects(new ArmorTaurun(MaterialRegistry.aMatTaurun, ArmorItem.Type.LEGGINGS, indestructibleProps())));
    public static final DeferredItem<Item> TAURUN_BOOTS = register("taurun_boots", () ->
            taurunEffects(new ArmorTaurun(MaterialRegistry.aMatTaurun, ArmorItem.Type.BOOTS, indestructibleProps())));

    private static ArmorTaurun taurunEffects(ArmorTaurun armor) {
        armor.addEffect(effect(MobEffects.DAMAGE_BOOST, 20, 0))
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(0.125D);
        return armor;
    }

    // ==================== Bismuth (4) - plain ArmorFSB, dash suit ====================
    // CE ModItems.java:585-593.

    public static final DeferredItem<Item> BISMUTH_HELMET = register("bismuth_helmet", () ->
            bismuthEffects(new ArmorBismuth(MaterialRegistry.aMatBismuth, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatBismuth, ArmorItem.Type.HELMET))));
    public static final DeferredItem<Item> BISMUTH_PLATE = register("bismuth_plate", () ->
            bismuthEffects(new ArmorBismuth(MaterialRegistry.aMatBismuth, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatBismuth, ArmorItem.Type.CHESTPLATE))));
    public static final DeferredItem<Item> BISMUTH_LEGS = register("bismuth_legs", () ->
            bismuthEffects(new ArmorBismuth(MaterialRegistry.aMatBismuth, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatBismuth, ArmorItem.Type.LEGGINGS))));
    public static final DeferredItem<Item> BISMUTH_BOOTS = register("bismuth_boots", () ->
            bismuthEffects(new ArmorBismuth(MaterialRegistry.aMatBismuth, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatBismuth, ArmorItem.Type.BOOTS))));

    private static ArmorBismuth bismuthEffects(ArmorBismuth armor) {
        armor.addEffect(effect(MobEffects.JUMP, 20, 6))
                .addEffect(effect(MobEffects.MOVEMENT_SPEED, 20, 6))
                .addEffect(effect(MobEffects.REGENERATION, 20, 1))
                .addEffect(effect(MobEffects.NIGHT_VISION, 15 * 20, 0))
                .setDashCount(3);
        return armor;
    }

    // ==================== Envsuit (4) - ArmorFSBPowered(100000,1000,250,0) ====================
    // CE ModItems.java:594-601.

    public static final DeferredItem<Item> ENVSUIT_HELMET = register("envsuit_helmet", () ->
            envsuitEffects(new ArmorEnvsuit(MaterialRegistry.aMatEnvsuit, ArmorItem.Type.HELMET, poweredProps(), 100000, 1000, 250, 0)));
    public static final DeferredItem<Item> ENVSUIT_PLATE = register("envsuit_plate", () ->
            envsuitEffects(new ArmorEnvsuit(MaterialRegistry.aMatEnvsuit, ArmorItem.Type.CHESTPLATE, poweredProps(), 100000, 1000, 250, 0)));
    public static final DeferredItem<Item> ENVSUIT_LEGS = register("envsuit_legs", () ->
            envsuitEffects(new ArmorEnvsuit(MaterialRegistry.aMatEnvsuit, ArmorItem.Type.LEGGINGS, poweredProps(), 100000, 1000, 250, 0)));
    public static final DeferredItem<Item> ENVSUIT_BOOTS = register("envsuit_boots", () ->
            envsuitEffects(new ArmorEnvsuit(MaterialRegistry.aMatEnvsuit, ArmorItem.Type.BOOTS, poweredProps(), 100000, 1000, 250, 0)));

    private static ArmorEnvsuit envsuitEffects(ArmorEnvsuit armor) {
        armor.addEffect(effect(MobEffects.JUMP, 30, 0))
                .addEffect(effect(MobEffects.MOVEMENT_SPEED, 30, 1))
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(1.0D);
        return armor;
    }

    // ==================== Dieselsuit / Diesel (4) - ArmorFSBFueled(DIESEL,64000,500,50,1) ====================
    // CE ModItems.java:602-608.

    public static final DeferredItem<Item> DIESELSUIT_HELMET = register("dieselsuit_helmet", () ->
            dieselEffects(new ArmorDiesel(MaterialRegistry.aMatDieselsuit, ArmorItem.Type.HELMET, props(MaterialRegistry.aMatDieselsuit, ArmorItem.Type.HELMET), Fluids.DIESEL, 64000, 500, 50, 1)));
    public static final DeferredItem<Item> DIESELSUIT_PLATE = register("dieselsuit_plate", () ->
            dieselEffects(new ArmorDiesel(MaterialRegistry.aMatDieselsuit, ArmorItem.Type.CHESTPLATE, props(MaterialRegistry.aMatDieselsuit, ArmorItem.Type.CHESTPLATE), Fluids.DIESEL, 64000, 500, 50, 1)));
    public static final DeferredItem<Item> DIESELSUIT_LEGS = register("dieselsuit_legs", () ->
            dieselEffects(new ArmorDiesel(MaterialRegistry.aMatDieselsuit, ArmorItem.Type.LEGGINGS, props(MaterialRegistry.aMatDieselsuit, ArmorItem.Type.LEGGINGS), Fluids.DIESEL, 64000, 500, 50, 1)));
    public static final DeferredItem<Item> DIESELSUIT_BOOTS = register("dieselsuit_boots", () ->
            dieselEffects(new ArmorDiesel(MaterialRegistry.aMatDieselsuit, ArmorItem.Type.BOOTS, props(MaterialRegistry.aMatDieselsuit, ArmorItem.Type.BOOTS), Fluids.DIESEL, 64000, 500, 50, 1)));

    private static ArmorDiesel dieselEffects(ArmorDiesel armor) {
        armor.addEffect(effect(MobEffects.JUMP, 30, 2))
                .addEffect(effect(MobEffects.MOVEMENT_SPEED, 30, 2))
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT);
        return armor;
    }

    // ==================== AJR (4) - ArmorFSBPowered(2500000,10000,2000,25) ====================
    // CE ModItems.java:609-622.

    public static final DeferredItem<Item> AJR_HELMET = register("ajr_helmet", () ->
            ajrEffects(new ArmorAJR(MaterialRegistry.aMatAJR, ArmorItem.Type.HELMET, poweredProps(), 2500000, 10000, 2000, 25)));
    public static final DeferredItem<Item> AJR_PLATE = register("ajr_plate", () ->
            ajrEffects(new ArmorAJR(MaterialRegistry.aMatAJR, ArmorItem.Type.CHESTPLATE, poweredProps(), 2500000, 10000, 2000, 25)));
    public static final DeferredItem<Item> AJR_LEGS = register("ajr_legs", () ->
            ajrEffects(new ArmorAJR(MaterialRegistry.aMatAJR, ArmorItem.Type.LEGGINGS, poweredProps(), 2500000, 10000, 2000, 25)));
    public static final DeferredItem<Item> AJR_BOOTS = register("ajr_boots", () ->
            ajrEffects(new ArmorAJR(MaterialRegistry.aMatAJR, ArmorItem.Type.BOOTS, poweredProps(), 2500000, 10000, 2000, 25)));

    private static ArmorAJR ajrEffects(ArmorAJR armor) {
        armor.enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true)
                .addEffect(effect(MobEffects.JUMP, 30, 0))
                .addEffect(effect(MobEffects.DAMAGE_BOOST, 30, 0))
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(1.3D)
                .setStep(HBMSoundHandler.metalStep.get()).setJump(HBMSoundHandler.ironJump.get()).setFall(HBMSoundHandler.ironLand.get());
        return armor;
    }

    // ==================== AJRO (4) - ArmorFSBPowered(2500000,10000,2000,25), same chain as AJR ====================
    // CE ModItems.java:623-636.

    public static final DeferredItem<Item> AJRO_HELMET = register("ajro_helmet", () ->
            ajroEffects(new ArmorAJRO(MaterialRegistry.aMatAJR, ArmorItem.Type.HELMET, poweredProps(), 2500000, 10000, 2000, 25)));
    public static final DeferredItem<Item> AJRO_PLATE = register("ajro_plate", () ->
            ajroEffects(new ArmorAJRO(MaterialRegistry.aMatAJR, ArmorItem.Type.CHESTPLATE, poweredProps(), 2500000, 10000, 2000, 25)));
    public static final DeferredItem<Item> AJRO_LEGS = register("ajro_legs", () ->
            ajroEffects(new ArmorAJRO(MaterialRegistry.aMatAJR, ArmorItem.Type.LEGGINGS, poweredProps(), 2500000, 10000, 2000, 25)));
    public static final DeferredItem<Item> AJRO_BOOTS = register("ajro_boots", () ->
            ajroEffects(new ArmorAJRO(MaterialRegistry.aMatAJR, ArmorItem.Type.BOOTS, poweredProps(), 2500000, 10000, 2000, 25)));

    private static ArmorAJRO ajroEffects(ArmorAJRO armor) {
        armor.enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true)
                .addEffect(effect(MobEffects.JUMP, 30, 0))
                .addEffect(effect(MobEffects.DAMAGE_BOOST, 30, 0))
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(1.3D)
                .setStep(HBMSoundHandler.metalStep.get()).setJump(HBMSoundHandler.ironJump.get()).setFall(HBMSoundHandler.ironLand.get());
        return armor;
    }

    // ==================== HEV (4) - ArmorFSBPowered(1000000,10000,2500,0) ====================
    // CE ModItems.java:637-646.

    public static final DeferredItem<Item> HEV_HELMET = register("hev_helmet", () ->
            hevEffects(new ArmorHEV(MaterialRegistry.aMatHEV, ArmorItem.Type.HELMET, poweredProps(), 1000000, 10000, 2500, 0)));
    public static final DeferredItem<Item> HEV_PLATE = register("hev_plate", () ->
            hevEffects(new ArmorHEV(MaterialRegistry.aMatHEV, ArmorItem.Type.CHESTPLATE, poweredProps(), 1000000, 10000, 2500, 0)));
    public static final DeferredItem<Item> HEV_LEGS = register("hev_legs", () ->
            hevEffects(new ArmorHEV(MaterialRegistry.aMatHEV, ArmorItem.Type.LEGGINGS, poweredProps(), 1000000, 10000, 2500, 0)));
    public static final DeferredItem<Item> HEV_BOOTS = register("hev_boots", () ->
            hevEffects(new ArmorHEV(MaterialRegistry.aMatHEV, ArmorItem.Type.BOOTS, poweredProps(), 1000000, 10000, 2500, 0)));

    private static ArmorHEV hevEffects(ArmorHEV armor) {
        armor.addEffect(effect(MobEffects.JUMP, 30, 0))
                .addEffect(effect(MobEffects.MOVEMENT_SPEED, 30, 1))
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(2.3D)
                .setHasGeigerSound(true).setHasCustomGeiger(true);
        return armor;
    }

    // ==================== BJ (4) + BJJetpack (1) - ArmorFSBPowered(10000000,10000,1000,100) ====================
    // CE ModItems.java:647-663.

    public static final DeferredItem<Item> BJ_HELMET = register("bj_helmet", () ->
            bjEffects(new ArmorBJ(MaterialRegistry.aMatBJ, ArmorItem.Type.HELMET, poweredProps(), 10000000, 10000, 1000, 100)));
    public static final DeferredItem<Item> BJ_PLATE = register("bj_plate", () ->
            bjEffects(new ArmorBJ(MaterialRegistry.aMatBJ, ArmorItem.Type.CHESTPLATE, poweredProps(), 10000000, 10000, 1000, 100)));
    public static final DeferredItem<Item> BJ_PLATE_JETPACK = register("bj_plate_jetpack", () ->
            bjEffects(new ArmorBJJetpack(MaterialRegistry.aMatBJ, ArmorItem.Type.CHESTPLATE, poweredProps(), 10000000, 10000, 1000, 100)));
    public static final DeferredItem<Item> BJ_LEGS = register("bj_legs", () ->
            bjEffects(new ArmorBJ(MaterialRegistry.aMatBJ, ArmorItem.Type.LEGGINGS, poweredProps(), 10000000, 10000, 1000, 100)));
    public static final DeferredItem<Item> BJ_BOOTS = register("bj_boots", () ->
            bjEffects(new ArmorBJ(MaterialRegistry.aMatBJ, ArmorItem.Type.BOOTS, poweredProps(), 10000000, 10000, 1000, 100)));

    private static <T extends ArmorBJ> T bjEffects(T armor) {
        armor.enableVATS(true).enableThermalSight(true).setHasHardLanding(true).setHasGeigerSound(true)
                .addEffect(effect(MobEffects.MOVEMENT_SPEED, 30, 1))
                .addEffect(effect(MobEffects.JUMP, 30, 0))
                .addEffect(effect(MobEffects.SATURATION, 30, 0))
                .addEffect(effect(HbmPotionEffects.RADX, 30, 0))
                .setStep(HBMSoundHandler.metalStep.get()).setJump(HBMSoundHandler.ironJump.get()).setFall(HBMSoundHandler.ironLand.get())
                .setRadResist(1D);
        return armor;
    }

    // ==================== RPA (4) - ArmorFSBPowered(2500000,10000,{1000|10000},25) ====================
    // CE ModItems.java:664-676. Consumption literal differs per piece - see class javadoc.

    public static final DeferredItem<Item> RPA_HELMET = register("rpa_helmet", () ->
            rpaHelmetEffects(new ArmorRPA(MaterialRegistry.aMatRPA, ArmorItem.Type.HELMET, poweredProps(), 2500000, 10000, 1000, 25)));
    public static final DeferredItem<Item> RPA_PLATE = register("rpa_plate", () ->
            rpaHelmetEffects(new ArmorRPA(MaterialRegistry.aMatRPA, ArmorItem.Type.CHESTPLATE, poweredProps(), 2500000, 10000, 10000, 25)));
    public static final DeferredItem<Item> RPA_LEGS = register("rpa_legs", () ->
            rpaHelmetEffects(new ArmorRPA(MaterialRegistry.aMatRPA, ArmorItem.Type.LEGGINGS, poweredProps(), 2500000, 10000, 10000, 25)));
    public static final DeferredItem<Item> RPA_BOOTS = register("rpa_boots", () ->
            rpaHelmetEffects(new ArmorRPA(MaterialRegistry.aMatRPA, ArmorItem.Type.BOOTS, poweredProps(), 2500000, 10000, 10000, 25)));

    private static <T extends ArmorFSB> T rpaHelmetEffects(T armor) {
        armor.enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true)
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(2D)
                .setStep(HBMSoundHandler.poweredStep.get()).setJump(HBMSoundHandler.poweredStep.get()).setFall(HBMSoundHandler.poweredStep.get())
                .addEffect(effect(MobEffects.DAMAGE_BOOST, 20, 3));
        return armor;
    }

    // ==================== NCRPA (4) - ArmorFSBPowered(2500000,10000,2000,25) ====================
    // CE ModItems.java:677-689. plate/legs/boots clone RPA_HELMET's stats, not NCRPA's own helmet -
    // see class javadoc's "Preserved CE quirks" note.

    public static final DeferredItem<Item> NCRPA_HELMET = register("ncrpa_helmet", () ->
            ncrpaHelmetEffects(new ArmorNCRPA(MaterialRegistry.aMatAJR, ArmorItem.Type.HELMET, poweredProps(), 2500000, 10000, 2000, 25)));
    public static final DeferredItem<Item> NCRPA_PLATE = register("ncrpa_plate", () ->
            rpaHelmetEffects(new ArmorNCRPA(MaterialRegistry.aMatAJR, ArmorItem.Type.CHESTPLATE, poweredProps(), 2500000, 10000, 2000, 25)));
    public static final DeferredItem<Item> NCRPA_LEGS = register("ncrpa_legs", () ->
            rpaHelmetEffects(new ArmorNCRPA(MaterialRegistry.aMatAJR, ArmorItem.Type.LEGGINGS, poweredProps(), 2500000, 10000, 2000, 25)));
    public static final DeferredItem<Item> NCRPA_BOOTS = register("ncrpa_boots", () ->
            rpaHelmetEffects(new ArmorNCRPA(MaterialRegistry.aMatAJR, ArmorItem.Type.BOOTS, poweredProps(), 2500000, 10000, 2000, 25)));

    private static <T extends ArmorFSB> T ncrpaHelmetEffects(T armor) {
        armor.enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true)
                .addEffect(effect(MobEffects.DAMAGE_BOOST, 20, 3))
                .setStep(HBMSoundHandler.poweredStep.get()).setJump(HBMSoundHandler.poweredStep.get()).setFall(HBMSoundHandler.poweredStep.get())
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(1.7D);
        return armor;
    }

    // ==================== Digamma / Fau (4) - ArmorFSBPowered(10000000,100000,25000,1000) ====================
    // CE ModItems.java:690-702.

    public static final DeferredItem<Item> FAU_HELMET = register("fau_helmet", () ->
            digammaEffects(new ArmorDigamma(MaterialRegistry.aMatFau, ArmorItem.Type.HELMET, poweredProps(), 10000000, 100000, 25000, 1000)));
    public static final DeferredItem<Item> FAU_PLATE = register("fau_plate", () ->
            digammaEffects(new ArmorDigamma(MaterialRegistry.aMatFau, ArmorItem.Type.CHESTPLATE, poweredProps(), 10000000, 100000, 25000, 1000)).setFullSetForHide());
    public static final DeferredItem<Item> FAU_LEGS = register("fau_legs", () ->
            digammaEffects(new ArmorDigamma(MaterialRegistry.aMatFau, ArmorItem.Type.LEGGINGS, poweredProps(), 10000000, 100000, 25000, 1000))
                    .setHides(IArmorDisableModel.EnumPlayerPart.LEFT_LEG, IArmorDisableModel.EnumPlayerPart.RIGHT_LEG).setFullSetForHide());
    public static final DeferredItem<Item> FAU_BOOTS = register("fau_boots", () ->
            digammaEffects(new ArmorDigamma(MaterialRegistry.aMatFau, ArmorItem.Type.BOOTS, poweredProps(), 10000000, 100000, 25000, 1000)));

    private static ArmorFSB digammaEffects(ArmorDigamma armor) {
        return armor.addEffect(effect(MobEffects.JUMP, 30, 1))
                .setHasGeigerSound(true).enableThermalSight(true).setHasHardLanding(true)
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(4D)
                .setStep(HBMSoundHandler.metalStep.get()).setJump(HBMSoundHandler.ironJump.get()).setFall(HBMSoundHandler.ironLand.get());
    }

    // ==================== DNS / DNT (4) - ArmorFSBPowered(1000000000,1000000,100000,115) ====================
    // CE ModItems.java:703-718.

    public static final DeferredItem<Item> DNS_HELMET = register("dns_helmet", () ->
            dnsEffects(new ArmorDNT(MaterialRegistry.aMatDNS, ArmorItem.Type.HELMET, poweredProps(), 1000000000L, 1000000, 100000, 115)));
    public static final DeferredItem<Item> DNS_PLATE = register("dns_plate", () ->
            dnsEffects(new ArmorDNT(MaterialRegistry.aMatDNS, ArmorItem.Type.CHESTPLATE, poweredProps(), 1000000000L, 1000000, 100000, 115)));
    public static final DeferredItem<Item> DNS_LEGS = register("dns_legs", () ->
            dnsEffects(new ArmorDNT(MaterialRegistry.aMatDNS, ArmorItem.Type.LEGGINGS, poweredProps(), 1000000000L, 1000000, 100000, 115)));
    public static final DeferredItem<Item> DNS_BOOTS = register("dns_boots", () ->
            dnsEffects(new ArmorDNT(MaterialRegistry.aMatDNS, ArmorItem.Type.BOOTS, poweredProps(), 1000000000L, 1000000, 100000, 115)));

    private static ArmorDNT dnsEffects(ArmorDNT armor) {
        armor.addEffect(effect(MobEffects.DAMAGE_BOOST, 20, 9))
                .addEffect(effect(MobEffects.DIG_SPEED, 20, 7))
                .addEffect(effect(MobEffects.JUMP, 20, 2))
                .setHasGeigerSound(true).enableVATS(true).enableThermalSight(true).setHasHardLanding(true)
                .setHides(IArmorDisableModel.EnumPlayerPart.HAT)
                .setHazardClass(ArmorUtil.FULL_PACKAGE).setRadResist(5D)
                .setStep(HBMSoundHandler.metalStep.get()).setJump(HBMSoundHandler.ironJump.get()).setFall(HBMSoundHandler.ironLand.get());
        return armor;
    }

    // ==================== Hat (1) - cosmetic, ArmorModel-based, ArmorMaterials.IRON ====================
    // CE ModItems.java:728. Java field name "hat", real registry id "nossy_hat" (the ctor's own name
    // param) - matched here, not the misleading field name.

    public static final DeferredItem<Item> NOSSY_HAT = ModItems.ITEMS.register("nossy_hat", () ->
            new ArmorHat(ArmorMaterials.IRON, ArmorItem.Type.HELMET, props(ArmorMaterials.IRON, ArmorItem.Type.HELMET)));
}
