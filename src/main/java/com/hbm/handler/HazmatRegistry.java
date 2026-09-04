package com.hbm.handler;

import com.hbm.items.armor.ItemModCladding;
import com.hbm.items.armor.PoweredArmorItems;
import com.hbm.items.gear.BasicArmorItems;
import com.hbm.items.gear.SpecialArmorItems;
import com.hbm.potion.HbmPotionEffects;
import com.hbm.util.ShadyUtil;
import com.hbm.util.Tuple;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.handler.HazmatRegistry} - the per-item radiation-resistance
 * coefficient table consumed by {@code ArmorFSB#setRadResist} and
 * {@code ArmorUtil#isFaradayArmor}.
 *
 * <p>{@link #initDefault()} is Exact CE {@code HazmatRegistry.java:41-160} for every named item
 * that is already registered. Skipped (do not invent): {@code alloy_*} armor,
 * {@code jackt}/{@code jackt2}, {@code Compat.registerCompatHazmat()}, Gson
 * {@code hbmRadResist.json} persistence.
 */
public final class HazmatRegistry {

    private HazmatRegistry() {
    }

    public static final List<Tuple.Pair<Item, Double>> external = new ArrayList<>();

    public static double helmet = 0.2D;
    public static double chest = 0.4D;
    public static double legs = 0.3D;
    public static double boots = 0.1D;

    private static final Map<Item, Double> entries = new HashMap<>();

    /**
     * Exact CE {@code HazmatRegistry.java:41-160}. Flush {@link #external} first (ArmorFSB
     * {@code setRadResist} queue), then the hardcoded per-item table. {@code alloy_*},
     * {@code jackt}/{@code jackt2}, {@code Compat.registerCompatHazmat()} stay skipped —
     * those items/hooks are not registered here.
     */
    public static void initDefault() {
        for (Tuple.Pair<Item, Double> pair : external) {
            registerHazmat(pair.getKey(), pair.getValue());
        }

        // assuming coefficient of 10
        // real coefficient turned out to be 5
        // oops

        double iron = 0.0225D; // 5%
        double gold = 0.0225D; // 5%
        double steel = 0.045D; // 10%
        double titanium = 0.045D; // 10%
        double cobalt = 0.125D; // 25%

        double hazYellow = 0.6D; // 50%
        double hazRed = 1.0D; // 90%
        double hazGray = 2D; // 99%
        double paa = 1.7D; // 97%
        double liquidator = 2.4D; // 99.6%

        double security = 0.825D; // 85%
        double star = 1D; // 90%
        double cmb = 1.3D; // 95%
        double schrab = 3D; // 99.9%
        double euph = 10D; // <100%

        registerHazmat(SpecialArmorItems.HAZMAT_HELMET.get(), hazYellow * helmet);
        registerHazmat(SpecialArmorItems.HAZMAT_PLATE.get(), hazYellow * chest);
        registerHazmat(SpecialArmorItems.HAZMAT_LEGS.get(), hazYellow * legs);
        registerHazmat(SpecialArmorItems.HAZMAT_BOOTS.get(), hazYellow * boots);

        registerHazmat(SpecialArmorItems.HAZMAT_HELMET_RED.get(), hazRed * helmet);
        registerHazmat(SpecialArmorItems.HAZMAT_PLATE_RED.get(), hazRed * chest);
        registerHazmat(SpecialArmorItems.HAZMAT_LEGS_RED.get(), hazRed * legs);
        registerHazmat(SpecialArmorItems.HAZMAT_BOOTS_RED.get(), hazRed * boots);

        registerHazmat(SpecialArmorItems.HAZMAT_HELMET_GREY.get(), hazGray * helmet);
        registerHazmat(SpecialArmorItems.HAZMAT_PLATE_GREY.get(), hazGray * chest);
        registerHazmat(SpecialArmorItems.HAZMAT_LEGS_GREY.get(), hazGray * legs);
        registerHazmat(SpecialArmorItems.HAZMAT_BOOTS_GREY.get(), hazGray * boots);

        registerHazmat(PoweredArmorItems.LIQUIDATOR_HELMET.get(), liquidator * helmet);
        registerHazmat(PoweredArmorItems.LIQUIDATOR_PLATE.get(), liquidator * chest);
        registerHazmat(PoweredArmorItems.LIQUIDATOR_LEGS.get(), liquidator * legs);
        registerHazmat(PoweredArmorItems.LIQUIDATOR_BOOTS.get(), liquidator * boots);

        registerHazmat(SpecialArmorItems.PAA_PLATE.get(), paa * chest);
        registerHazmat(SpecialArmorItems.PAA_LEGS.get(), paa * legs);
        registerHazmat(SpecialArmorItems.PAA_BOOTS.get(), paa * boots);

        registerHazmat(SpecialArmorItems.HAZMAT_PAA_HELMET.get(), paa * helmet);
        registerHazmat(SpecialArmorItems.HAZMAT_PAA_PLATE.get(), paa * chest);
        registerHazmat(SpecialArmorItems.HAZMAT_PAA_LEGS.get(), paa * legs);
        registerHazmat(SpecialArmorItems.HAZMAT_PAA_BOOTS.get(), paa * boots);

        registerHazmat(BasicArmorItems.SECURITY_HELMET.get(), security * helmet);
        registerHazmat(BasicArmorItems.SECURITY_PLATE.get(), security * chest);
        registerHazmat(BasicArmorItems.SECURITY_LEGS.get(), security * legs);
        registerHazmat(BasicArmorItems.SECURITY_BOOTS.get(), security * boots);

        registerHazmat(BasicArmorItems.STARMETAL_HELMET.get(), star * helmet);
        registerHazmat(BasicArmorItems.STARMETAL_PLATE.get(), star * chest);
        registerHazmat(BasicArmorItems.STARMETAL_LEGS.get(), star * legs);
        registerHazmat(BasicArmorItems.STARMETAL_BOOTS.get(), star * boots);

        // TODO(CE:HazmatRegistry.java:109-110): jackt/jackt2 — unregistered, skip invent.

        registerHazmat(SpecialArmorItems.GAS_MASK.get(), 0.07);
        registerHazmat(SpecialArmorItems.GAS_MASK_M65.get(), 0.095);

        registerHazmat(BasicArmorItems.STEEL_HELMET.get(), steel * helmet);
        registerHazmat(BasicArmorItems.STEEL_PLATE.get(), steel * chest);
        registerHazmat(BasicArmorItems.STEEL_LEGS.get(), steel * legs);
        registerHazmat(BasicArmorItems.STEEL_BOOTS.get(), steel * boots);

        registerHazmat(BasicArmorItems.TITANIUM_HELMET.get(), titanium * helmet);
        registerHazmat(BasicArmorItems.TITANIUM_PLATE.get(), titanium * chest);
        registerHazmat(BasicArmorItems.TITANIUM_LEGS.get(), titanium * legs);
        registerHazmat(BasicArmorItems.TITANIUM_BOOTS.get(), titanium * boots);

        registerHazmat(BasicArmorItems.COBALT_HELMET.get(), cobalt * helmet);
        registerHazmat(BasicArmorItems.COBALT_PLATE.get(), cobalt * chest);
        registerHazmat(BasicArmorItems.COBALT_LEGS.get(), cobalt * legs);
        registerHazmat(BasicArmorItems.COBALT_BOOTS.get(), cobalt * boots);

        registerHazmat(Items.IRON_HELMET, iron * helmet);
        registerHazmat(Items.IRON_CHESTPLATE, iron * chest);
        registerHazmat(Items.IRON_LEGGINGS, iron * legs);
        registerHazmat(Items.IRON_BOOTS, iron * boots);

        registerHazmat(Items.GOLDEN_HELMET, gold * helmet);
        registerHazmat(Items.GOLDEN_CHESTPLATE, gold * chest);
        registerHazmat(Items.GOLDEN_LEGGINGS, gold * legs);
        registerHazmat(Items.GOLDEN_BOOTS, gold * boots);

        // TODO(CE:HazmatRegistry.java:140-143): alloy_* armor — unregistered, skip invent.

        registerHazmat(BasicArmorItems.CMB_HELMET.get(), cmb * helmet);
        registerHazmat(BasicArmorItems.CMB_PLATE.get(), cmb * chest);
        registerHazmat(BasicArmorItems.CMB_LEGS.get(), cmb * legs);
        registerHazmat(BasicArmorItems.CMB_BOOTS.get(), cmb * boots);

        registerHazmat(SpecialArmorItems.SCHRABIDIUM_HELMET.get(), schrab * helmet);
        registerHazmat(SpecialArmorItems.SCHRABIDIUM_PLATE.get(), schrab * chest);
        registerHazmat(SpecialArmorItems.SCHRABIDIUM_LEGS.get(), schrab * legs);
        registerHazmat(SpecialArmorItems.SCHRABIDIUM_BOOTS.get(), schrab * boots);

        registerHazmat(SpecialArmorItems.EUPHEMIUM_HELMET.get(), euph * helmet);
        registerHazmat(SpecialArmorItems.EUPHEMIUM_PLATE.get(), euph * chest);
        registerHazmat(SpecialArmorItems.EUPHEMIUM_LEGS.get(), euph * legs);
        registerHazmat(SpecialArmorItems.EUPHEMIUM_BOOTS.get(), euph * boots);

        // TODO(CE:HazmatRegistry.java:160): Compat.registerCompatHazmat() — unported.
    }

    public static void registerHazmat(Item item, double resistance) {
        entries.put(item, resistance);
    }

    public static double getResistance(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0D;

        double cladding = getCladding(stack);
        Double f = entries.get(stack.getItem());

        return f != null ? f + cladding : cladding;
    }

    /**
     * Exact CE {@code HazmatRegistry.java:185-201} {@code pryMods} cladding-slot path.
     * {@code hfr_cladding} NBT override stays skipped — no DataComponent, do not invent.
     */
    public static double getCladding(ItemStack stack) {
        if (ArmorModHandler.hasMods(stack)) {
            ItemStack cladding = ArmorModHandler.pryMods(stack)[ArmorModHandler.cladding];
            if (!cladding.isEmpty() && cladding.getItem() instanceof ItemModCladding itemModCladding) {
                return itemModCladding.rad;
            }
        }
        return 0;
    }

    /**
     * CE: {@code HazmatRegistry#getResistance(EntityLivingBase)} - sums {@link #getResistance(ItemStack)}
     * across all 4 worn armor slots, plus a hardcoded {@code ShadyUtil.Pu_238} UUID bonus and a flat
     * {@code 0.2F} bonus while {@code com.hbm.potion.HbmPotionEffects#RADX} is active.
     */
    public static float getResistance(LivingEntity entity) {
        float res = 0.0F;

        if (entity.getUUID().equals(ShadyUtil.Pu_238)) {
            res += 0.4F;
        }

        if (entity.hasEffect(HbmPotionEffects.RADX)) {
            res += 0.2F;
        }

        if (entity instanceof Player player) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!slot.isArmor()) continue;
                res += (float) getResistance(player.getItemBySlot(slot));
            }
        }

        return res;
    }
}
