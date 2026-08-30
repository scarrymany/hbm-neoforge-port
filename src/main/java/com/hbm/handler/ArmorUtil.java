package com.hbm.handler;

import com.hbm.api.item.IGasMask;
import com.hbm.items.HbmDataComponents;
import com.hbm.items.gear.SpecialArmorItems;
import com.hbm.lib.Library;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.Tuple;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Direct port of CE's {@code com.hbm.handler.ArmorUtil}, against {@code util.ArmorRegistry}
 * (already ported), {@link ArmorModHandler} (this same area), and
 * {@code capability.HbmLivingProps} (this same area). Confirmed CE's only {@code ArmorUtil};
 * {@code com.hbm.util.ArmorUtil} does not exist, no namespace duplication.
 *
 * <p>Already a live, uncommented dependency of {@code util.ArmorRegistry} ({@link #checkArmorNull},
 * 3 call sites), {@code hazard.type.HazardTypeAsbestos}/{@code HazardTypeCoal}/
 * {@code inventory.fluid.trait.FT_Toxin} ({@link #damageGasMaskFilter}), and
 * {@code hazard.type.HazardTypeCold}/{@code HazardTypeToxic}/{@code FT_Toxin}
 * ({@link #checkForHazmat}).
 *
 * <p><b>Forward references, partially resolved by {@code docs/phase3/armor_special_sets.md}'s
 * {@code com.hbm.items.gear.SpecialArmorItems}:</b> {@link #checkForAsbestos} is now fully wired
 * (all 4 {@code asbestos_*} items exist); {@link #checkForHazmatOnly}/{@link #checkForHaz2}/
 * {@link #checkForHazmat} are wired for the hazmat/hazmat_paa/euphemium/schrabidium sets that
 * package registers, with a per-branch TODO naming the still-missing {@code liquidator}/
 * {@code rpa}/{@code fau}/{@code dns} sets. {@link #checkForFiend}/{@link #checkForFiend2}
 * ({@code jackt}/{@code jackt2}) and {@link #checkForDigamma} ({@code fau}/{@code dns}) remain
 * fully stubbed to CE's own "no match" return value ({@code false}) - those armor pieces belong to
 * a separate, not-yet-scheduled Phase 3 "armor items" work package, and referencing a nonexistent
 * static field would be a hard compile error. Every other method in this class (the gas-mask-filter
 * helpers, {@link #checkForFaraday}, {@link #checkArmorNull}, {@link #damageSuit}, {@link
 * #resetFlightTime}, {@link #checkArmor}, {@link #checkArmorPiece}) has no such dependency and is
 * ported in full.
 */
public final class ArmorUtil {

    private ArmorUtil() {
    }

    public static final List<Tuple.Pair<Item, HazardClass[]>> external = new ArrayList<>();

    public static final HazardClass[] FULL_NO_LIGHT = new HazardClass[]{
            HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG,
            HazardClass.BACTERIA, HazardClass.GAS_BLISTERING, HazardClass.GAS_MONOXIDE, HazardClass.SAND};
    public static final HazardClass[] FULL_PACKAGE = new HazardClass[]{
            HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG,
            HazardClass.BACTERIA, HazardClass.GAS_BLISTERING, HazardClass.GAS_MONOXIDE,
            HazardClass.LIGHT, HazardClass.SAND};

    /**
     * CE's real body is a large {@code ArmorRegistry.registerHazard(...)} data-wiring block naming
     * ~20 concrete gas-mask/hazmat/schrabidium/euphemium armor items (none of which exist in this
     * port yet - the same "armor items" work package named in this class's javadoc) plus a
     * cross-mod GregTech compat hook (not applicable to this NeoForge port). Both of those pieces
     * are DEFERRED (TODO below); what is NOT deferred is CE's generic {@code external} flush loop -
     * it depends on no concrete item, only on whatever has already called
     * {@link com.hbm.items.gear.ArmorFSB#setHazardClass}, so it is restored here in full. This must
     * be called once, after every item is constructed (mirroring CE's own
     * {@code FMLPreInitializationEvent}-time call site) - wired from
     * {@code com.hbm.main.CommonEvents#commonSetup(FMLCommonSetupEvent)}, confirmed as the exact
     * real call-site timing Neo Edition's own {@code CommonEvents.commonSetup} uses.
     */
    public static void register() {
        // CE's real concrete-item block (docs/phase3/armor_special_sets.md-scoped items only - see
        // that package's structured report for the full ~20-call table). The gas_mask_filter*/
        // mask_rag/mask_piss/goggles/ashglasses/attachment_mask/liquidator_helmet entries and the
        // GregTech "registerIfExists" compat hook are still forward references: none of those items
        // exist in this port yet (a wider "armor items"/attachments scope than this package), and
        // GregTech compat is out of this NeoForge port's scope entirely.
        ArmorRegistry.registerHazard(SpecialArmorItems.GAS_MASK.get(), HazardClass.SAND, HazardClass.LIGHT);
        ArmorRegistry.registerHazard(SpecialArmorItems.GAS_MASK_M65.get(), HazardClass.SAND);

        ArmorRegistry.registerHazard(SpecialArmorItems.ASBESTOS_HELMET.get(), HazardClass.SAND, HazardClass.LIGHT);
        ArmorRegistry.registerHazard(SpecialArmorItems.HAZMAT_HELMET.get(), HazardClass.SAND);
        ArmorRegistry.registerHazard(SpecialArmorItems.HAZMAT_HELMET_RED.get(), HazardClass.SAND);
        ArmorRegistry.registerHazard(SpecialArmorItems.HAZMAT_HELMET_GREY.get(), HazardClass.SAND);
        ArmorRegistry.registerHazard(SpecialArmorItems.HAZMAT_PAA_HELMET.get(), HazardClass.LIGHT, HazardClass.SAND);
        // TODO(liquidator armor not yet ported): CE also registers liquidator_helmet here
        // (HazardClass.LIGHT, HazardClass.SAND) - a wider "armor items" scope than this package.
        ArmorRegistry.registerHazard(SpecialArmorItems.SCHRABIDIUM_HELMET.get(), FULL_PACKAGE);
        ArmorRegistry.registerHazard(SpecialArmorItems.EUPHEMIUM_HELMET.get(), FULL_PACKAGE);

        for (Tuple.Pair<Item, HazardClass[]> pair : external) {
            ArmorRegistry.registerHazard(pair.getKey(), pair.getValue());
        }

        // TODO(wider "armor items"/attachments scope): CE also registers gas_mask_filter/
        // gas_mask_filter_mono/gas_mask_filter_combo/gas_mask_filter_rag/gas_mask_filter_piss
        // (ItemFilter items), mask_rag/mask_piss (ArmorModel-based items), goggles/ashglasses
        // (ArmorModel-based), attachment_mask, and a GregTech "registerIfExists" compat hook (3
        // cross-mod hazmat helmets) here - none of those items exist in this port yet, and GregTech
        // compat is out of this NeoForge port's scope entirely.
    }

    public static boolean checkForFaraday(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);

        if (boots.isEmpty() || legs.isEmpty() || chest.isEmpty() || head.isEmpty()) return false;

        return isFaradayArmor(boots) && isFaradayArmor(legs) && isFaradayArmor(chest) && isFaradayArmor(head);
    }

    public static final String[] metals = new String[]{
            "chainmail", "iron", "silver", "gold", "platinum", "tin", "lead", "liquidator",
            "schrabidium", "euphemium", "steel", "cmb", "titanium", "alloy", "copper", "bronze",
            "electrum", "t45", "bj", "starmetal", "hazmat", "rubber", "hev", "ajr", "rpa", "spacesuit"
    };

    public static boolean isFaradayArmor(ItemStack item) {
        String name = item.getDescriptionId().toLowerCase(Locale.US);

        for (String metal : metals) {
            if (name.contains(metal)) return true;
        }

        // HazmatRegistry (com.hbm.handler.HazmatRegistry) now exists - getCladding itself is still a
        // documented stub pending ItemModCladding (see that method's own javadoc), so this remains
        // behavior-identical to the previous inline stub, just centralized into the real engine.
        return HazmatRegistry.getCladding(item) > 0;
    }

    public static boolean checkArmorNull(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot).isEmpty();
    }

    public static void damageSuit(LivingEntity entity, EquipmentSlot slot, int amount) {
        ItemStack piece = entity.getItemBySlot(slot);
        if (piece.isEmpty()) return;

        piece.hurtAndBreak(amount, entity, slot);
    }

    /**
     * TODO(unconfirmed 1.21.1 Mojang mapping): CE resets {@code mp.connection.floatingTickCount =
     * 0} here (the server connection's anti-cheat "hovering too long without falling" tick
     * counter), called whenever a player is legitimately airborne under their own power (e.g.
     * jetpacks) to prevent a bogus flight-kick. This port could not confirm the exact 1.21.1
     * Mojang-mapped field name on {@code ServerGamePacketListenerImpl} against a real compiled
     * class or a second source (no NeoForge/vanilla decompiled jar was reachable in this sandbox,
     * and the Neo Edition reference port has no equivalent call to cross-check against - see this
     * area's research report's Open questions section). Left as a documented no-op rather than
     * guessing at a field name that could reference the wrong counter or fail to compile; confirm
     * the real field before wiring this into real flight code.
     */
    public static void resetFlightTime(Player player) {
        if (!(player instanceof ServerPlayer)) return;
    }

    // TODO(jackt/jackt2 "fiend" cloak chestplates not yet ported - unlike shimmer_axe/
    // shimmer_sledge, which already exist as GearItems.SHIMMER_AXE/GearItems.SHIMMER_SLEDGE, no
    // ModItems/GearItems field for jackt or jackt2 exists anywhere in this port yet; these two
    // armor pieces belong to a later Phase 3 "armor items" work package per
    // docs/phase3/armor_equippable_framework.md's Phase-3-safe-scope table). Referencing those
    // fields here before they exist is a hard compile error, not a soft forward reference, so both
    // checks are stubbed to false (CE's own default when the jackt piece isn't worn) until that
    // package lands.
    public static boolean checkForFiend2(Player player) {
        return false;
    }

    public static boolean checkForFiend(Player player) {
        return false;
    }

    /**
     * {@code docs/phase3/armor_special_sets.md}-scoped sets ({@code hazmat_paa}, {@code euphemium})
     * are wired for real; {@code liquidator}/{@code rpa}/{@code fau}/{@code dns} are a wider "armor
     * items" scope than this package and remain a documented forward reference.
     */
    public static boolean checkForHaz2(LivingEntity entity) {
        if (checkArmor(entity, SpecialArmorItems.HAZMAT_PAA_HELMET.get(), SpecialArmorItems.HAZMAT_PAA_PLATE.get(),
                SpecialArmorItems.HAZMAT_PAA_LEGS.get(), SpecialArmorItems.HAZMAT_PAA_BOOTS.get())) return true;
        if (checkArmor(entity, SpecialArmorItems.EUPHEMIUM_HELMET.get(), SpecialArmorItems.EUPHEMIUM_PLATE.get(),
                SpecialArmorItems.EUPHEMIUM_LEGS.get(), SpecialArmorItems.EUPHEMIUM_BOOTS.get())) return true;

        // TODO(liquidator/rpa/fau/dns armor sets not yet ported - a wider "armor items" scope than
        // this package).
        return false;
    }

    /**
     * {@code docs/phase3/armor_special_sets.md}-scoped hazmat sets (base/red/grey/paa) are wired
     * for real; {@code liquidator} is a wider "armor items" scope than this package.
     */
    public static boolean checkForHazmatOnly(LivingEntity entity) {
        if (checkArmor(entity, SpecialArmorItems.HAZMAT_HELMET.get(), SpecialArmorItems.HAZMAT_PLATE.get(),
                SpecialArmorItems.HAZMAT_LEGS.get(), SpecialArmorItems.HAZMAT_BOOTS.get())) return true;
        if (checkArmor(entity, SpecialArmorItems.HAZMAT_HELMET_RED.get(), SpecialArmorItems.HAZMAT_PLATE_RED.get(),
                SpecialArmorItems.HAZMAT_LEGS_RED.get(), SpecialArmorItems.HAZMAT_BOOTS_RED.get())) return true;
        if (checkArmor(entity, SpecialArmorItems.HAZMAT_HELMET_GREY.get(), SpecialArmorItems.HAZMAT_PLATE_GREY.get(),
                SpecialArmorItems.HAZMAT_LEGS_GREY.get(), SpecialArmorItems.HAZMAT_BOOTS_GREY.get())) return true;
        if (checkArmor(entity, SpecialArmorItems.HAZMAT_PAA_HELMET.get(), SpecialArmorItems.HAZMAT_PAA_PLATE.get(),
                SpecialArmorItems.HAZMAT_PAA_LEGS.get(), SpecialArmorItems.HAZMAT_PAA_BOOTS.get())) return true;

        // TODO(liquidator armor not yet ported - a wider "armor items" scope than this package).
        return false;
    }

    /**
     * {@code docs/phase3/armor_special_sets.md}-scoped hazmat/schrabidium sets are wired for real;
     * {@link #checkForHaz2} covers its own scope. {@code HbmPotion.mutation} is a separate,
     * not-yet-ported {@code MobEffect}-registration area (see {@code ContaminationUtil}'s own
     * identical TODO for the same effect).
     */
    @Deprecated
    public static boolean checkForHazmat(LivingEntity entity) {
        if (checkArmor(entity, SpecialArmorItems.HAZMAT_HELMET.get(), SpecialArmorItems.HAZMAT_PLATE.get(),
                SpecialArmorItems.HAZMAT_LEGS.get(), SpecialArmorItems.HAZMAT_BOOTS.get())
                || checkArmor(entity, SpecialArmorItems.HAZMAT_HELMET_RED.get(), SpecialArmorItems.HAZMAT_PLATE_RED.get(),
                        SpecialArmorItems.HAZMAT_LEGS_RED.get(), SpecialArmorItems.HAZMAT_BOOTS_RED.get())
                || checkArmor(entity, SpecialArmorItems.HAZMAT_HELMET_GREY.get(), SpecialArmorItems.HAZMAT_PLATE_GREY.get(),
                        SpecialArmorItems.HAZMAT_LEGS_GREY.get(), SpecialArmorItems.HAZMAT_BOOTS_GREY.get())
                || checkArmor(entity, SpecialArmorItems.SCHRABIDIUM_HELMET.get(), SpecialArmorItems.SCHRABIDIUM_PLATE.get(),
                        SpecialArmorItems.SCHRABIDIUM_LEGS.get(), SpecialArmorItems.SCHRABIDIUM_BOOTS.get())
                || checkForHaz2(entity)) {
            return true;
        }

        // TODO(HbmPotion): CE also returns true for player.isPotionActive(HbmPotion.mutation)
        // here; HbmPotion doesn't exist in this port yet (see Deferred scope).
        return false;
    }

    /** Fully wireable: all 4 {@code asbestos_*} items are registered by this package. */
    public static boolean checkForAsbestos(LivingEntity entity) {
        return checkArmor(entity, SpecialArmorItems.ASBESTOS_HELMET.get(), SpecialArmorItems.ASBESTOS_PLATE.get(),
                SpecialArmorItems.ASBESTOS_LEGS.get(), SpecialArmorItems.ASBESTOS_BOOTS.get());
    }

    public static boolean checkArmor(LivingEntity entity, Item helm, Item chest, Item leg, Item shoe) {
        return entity.getItemBySlot(EquipmentSlot.FEET).getItem() == shoe
                && entity.getItemBySlot(EquipmentSlot.LEGS).getItem() == leg
                && entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == chest
                && entity.getItemBySlot(EquipmentSlot.HEAD).getItem() == helm;
    }

    public static boolean checkArmorPiece(LivingEntity entity, Item armor, EquipmentSlot slot) {
        return !entity.getItemBySlot(slot).isEmpty() && entity.getItemBySlot(slot).getItem() == armor;
    }

    /*
     * Default implementations for IGasMask items
     */

    public static void damageGasMaskFilter(LivingEntity entity, int damage) {
        ItemStack mask = entity.getItemBySlot(EquipmentSlot.HEAD);

        if (mask.isEmpty()) return;

        if (!(mask.getItem() instanceof IGasMask)) {
            if (ArmorModHandler.hasMods(mask)) {
                ItemStack[] mods = ArmorModHandler.pryMods(mask);

                if (!mods[ArmorModHandler.helmet_only].isEmpty() && mods[ArmorModHandler.helmet_only].getItem() instanceof IGasMask) {
                    mask = mods[ArmorModHandler.helmet_only];
                }
            }
        }

        damageGasMaskFilter(mask, damage);
    }

    public static void damageGasMaskFilter(ItemStack mask, int damage) {
        ItemStack filter = getGasMaskFilter(mask);

        if (filter.isEmpty() && ArmorModHandler.hasMods(mask)) {
            ItemStack[] mods = ArmorModHandler.pryMods(mask);

            if (!mods[ArmorModHandler.helmet_only].isEmpty() && mods[ArmorModHandler.helmet_only].getItem() instanceof IGasMask) {
                filter = getGasMaskFilter(mods[ArmorModHandler.helmet_only]);
            }
        }

        if (filter.isEmpty() || filter.getMaxDamage() == 0) return;

        filter.setDamageValue(filter.getDamageValue() + damage);

        if (filter.getDamageValue() > filter.getMaxDamage()) {
            removeFilter(mask);
        } else {
            installGasMaskFilter(mask, filter);
        }
    }

    public static void installGasMaskFilter(ItemStack mask, ItemStack filter) {
        if (mask.isEmpty() || filter.isEmpty()) return;

        ItemStack copy = filter.copy();
        copy.setCount(1);
        mask.set(HbmDataComponents.GAS_MASK_FILTER.get(), copy);
    }

    public static void removeFilter(ItemStack mask) {
        if (mask.isEmpty()) return;
        mask.remove(HbmDataComponents.GAS_MASK_FILTER.get());
    }

    public static ItemStack getGasMaskFilter(ItemStack mask) {
        if (mask.isEmpty()) return ItemStack.EMPTY;
        ItemStack filter = mask.get(HbmDataComponents.GAS_MASK_FILTER.get());
        return filter == null ? ItemStack.EMPTY : filter;
    }

    // TODO(fau/dns "digamma"/"deep null suit" armor sets not yet ported - same blocker as
    // checkForHaz2 above).
    public static boolean checkForDigamma(Player player) {
        // TODO(HbmPotion): CE also returns true for player.isPotionActive(HbmPotion.stability)
        // here; HbmPotion doesn't exist in this port yet (see Deferred scope).
        return false;
    }

    /**
     * Grabs the installed filter or the filter of the attachment, used for attachment rendering.
     */
    public static ItemStack getGasMaskFilterRecursively(ItemStack mask) {
        ItemStack filter = getGasMaskFilter(mask);

        if (filter.isEmpty() && ArmorModHandler.hasMods(mask)) {
            ItemStack[] mods = ArmorModHandler.pryMods(mask);

            if (!mods[ArmorModHandler.helmet_only].isEmpty() && mods[ArmorModHandler.helmet_only].getItem() instanceof IGasMask gasMask) {
                filter = gasMask.getFilter(mods[ArmorModHandler.helmet_only]);
            }
        }

        return filter;
    }

    /**
     * Simplified relative to CE: CE also recurses into the filter stack's own tooltip lines here
     * ({@code Item#addInformation} plus {@code ForgeEventFactory.onItemTooltip}, so cross-mod
     * tooltip additions on the filter itself show up nested under the mask's tooltip). This
     * static helper has no confirmed-safe way to build a detached {@code Item.TooltipContext} to
     * reproduce that without a real caller to verify the shape against - no {@link IGasMask}
     * implementor exists in this port yet (that's part of the not-yet-ported "armor items" work
     * package), so this is simplified to the filter's name and remaining durability; restore the
     * full recursive tooltip once a real caller exists.
     */
    public static void addGasMaskTooltip(ItemStack mask, List<Component> list) {
        if (mask.isEmpty() || !(mask.getItem() instanceof IGasMask gasMask)) return;

        ItemStack filter = gasMask.getFilter(mask);

        if (filter.isEmpty()) {
            list.add(Component.literal(I18nUtil.resolveKey("desc.nofilter")));
            return;
        }

        list.add(Component.literal(I18nUtil.resolveKey("desc.infilter")));

        int damage = filter.getDamageValue();
        int max = filter.getMaxDamage();

        String append = "";
        if (max > 0) {
            append = " (" + Library.roundFloat((max - damage) * 100F / max, 2) + "%) " + (max - damage) + "/" + max;
        }

        list.add(Component.literal("  ").append(filter.getHoverName()).append(append));
    }
}
