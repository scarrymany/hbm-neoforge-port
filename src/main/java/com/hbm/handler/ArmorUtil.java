package com.hbm.handler;

import com.hbm.api.item.IGasMask;
import com.hbm.items.HbmDataComponents;
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
 * <p><b>Forward references (expected, per this area's research report), stubbed to compile
 * today:</b> every {@code checkFor*} method that CE keys off a concrete armor set (hazmat, fau/dns
 * "digamma", asbestos, euphemium, liquidator, rpa, hazmat_paa, jackt/jackt2) references {@code
 * ModItems} fields that are not registered anywhere in this port yet - those armor pieces belong
 * to a separate, not-yet-scheduled Phase 3 "armor items" work package. Referencing a nonexistent
 * static field is a hard compile error, not a soft forward reference the way an unresolved runtime
 * lookup would be, so each such method is stubbed to CE's own "no match" return value ({@code
 * false}) with an inline TODO naming exactly which {@code ModItems} fields it needs, rather than
 * left referencing fields that don't exist. Every other method in this class (the gas-mask-filter
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
        for (Tuple.Pair<Item, HazardClass[]> pair : external) {
            ArmorRegistry.registerHazard(pair.getKey(), pair.getValue());
        }

        // TODO: CE also hard-codes ~20 ArmorRegistry.registerHazard(...) calls here for concrete
        // gas-mask-filter/hazmat/schrabidium/euphemium items (e.g. ModItems.gas_mask_filter,
        // ModItems.hazmat_helmet, ModItems.schrabidium_helmet) plus a "registerIfExists" GregTech
        // compat hook for 3 cross-mod armor pieces. None of those items exist in this port yet
        // (see class javadoc's "Forward references" note) and GregTech compat is out of this
        // NeoForge port's scope entirely - port the concrete-item block once the "armor items"
        // work package lands.
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

        // TODO(HazmatRegistry): CE also checks HazmatRegistry.getCladding(item) > 0 here (the
        // per-armor-piece cladding coefficient table). That class doesn't exist in this port yet
        // (see this area's research report's Deferred scope) - stub as 0, matching CE's own
        // `f != null ? f : 0` null-safety idiom (a true no-behavior-change stub, not a guess).
        int cladding = 0;
        return cladding > 0;
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

    // TODO(hazmat_paa/liquidator/euphemium/rpa/fau/dns armor sets not yet ported - see class
    // javadoc's "Forward references" note and docs/phase3/armor_equippable_framework.md's
    // Phase-3-safe-scope table for the owning "armor items" work package). Stubbed to false
    // (CE's own default when none of these sets are worn) rather than referencing nonexistent
    // ModItems fields, which is a hard compile error.
    public static boolean checkForHaz2(LivingEntity entity) {
        return false;
    }

    // TODO(hazmat/hazmat_red/hazmat_grey/hazmat_paa/liquidator armor sets not yet ported - same
    // blocker as checkForHaz2 above).
    public static boolean checkForHazmatOnly(LivingEntity entity) {
        return false;
    }

    // TODO(hazmat/hazmat_red/hazmat_grey/schrabidium armor sets not yet ported - same blocker as
    // checkForHaz2 above; checkForHaz2 itself is likewise stubbed).
    @Deprecated
    public static boolean checkForHazmat(LivingEntity entity) {
        // TODO(HbmPotion): CE also returns true for player.isPotionActive(HbmPotion.mutation)
        // here; HbmPotion doesn't exist in this port yet (see Deferred scope).
        return false;
    }

    // TODO(asbestos_helmet/plate/legs/boots not yet ported - same blocker as checkForHaz2 above).
    public static boolean checkForAsbestos(LivingEntity entity) {
        return false;
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
