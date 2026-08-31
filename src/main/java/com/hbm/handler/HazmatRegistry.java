package com.hbm.handler;

import com.hbm.potion.HbmPotionEffects;
import com.hbm.util.ShadyUtil;
import com.hbm.util.Tuple;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.handler.HazmatRegistry} - the per-item radiation-resistance
 * coefficient table {@code com.hbm.items.gear.ArmorFSB#setRadResist} and
 * {@code com.hbm.handler.ArmorUtil#isFaradayArmor} already forward-reference (see those classes'
 * own TODO comments, both now resolved to call into this class for real).
 *
 * <p>Per this package's task brief: only the small "engine" itself is ported here
 * ({@link #external}, {@link #helmet}/{@link #chest}/{@link #legs}/{@link #boots},
 * {@link #registerHazmat}, {@link #getResistance(ItemStack)}, {@link #getCladding},
 * {@link #getResistance(LivingEntity)}) - CE's real {@link #initDefault()} body is a ~90-line
 * hardcoded wiring block naming roughly 40 concrete armor items across steel/titanium/alloy/cobalt/
 * cmb/security/starmetal/liquidator/rpa/fau/dns/jackt/vanilla-iron/vanilla-gold sets, most of which
 * belong to other, not-yet-scheduled Phase 3 "armor items" work packages (only the hazmat/gas-mask/
 * schrabidium/euphemium items this package itself registers are in scope here). Left empty per the
 * task's explicit instruction rather than half-wiring a table this package can't fully populate -
 * whoever ports the remaining armor sets should fill this in with the full CE table (reproduced
 * below in a comment for reference) once every named item exists.
 *
 * <p>CE also persists {@link #entries} to a per-world {@code hbmRadResist.json} config file
 * ({@code registerHazmats}/{@code writeDefault}/{@code readConfig}, Gson-backed) - not ported here;
 * out of this package's named scope ("the engine itself... registerHazmat/getResistance/
 * getCladding"), and this port's established config-loading pattern (see
 * {@code com.hbm.inventory.recipes}) is JSON-recipe-shaped, not a 1:1 fit for this ad-hoc format.
 * {@link #initDefault()} should still be called once (CE: {@code FMLPreInitializationEvent} time)
 * once it has real content to wire.
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
     * CE's real body first flushes {@link #external} (restored here, depends on no concrete item -
     * same idiom as {@code ArmorUtil.register()}'s own {@code external} flush), then hardcodes
     * ~40 {@code registerHazmat(ModItems.<item>, <coefficient>)} calls for armor sets outside this
     * package's scope (hazmat/gas-mask/schrabidium/euphemium - the sets this package itself owns -
     * plus steel/titanium/alloy/cobalt/cmb/security/starmetal/liquidator/rpa/fau/dns/jackt/vanilla
     * iron+gold, which do not). Left as a TODO per the task brief rather than populated here.
     */
    public static void initDefault() {
        for (Tuple.Pair<Item, Double> pair : external) {
            registerHazmat(pair.getKey(), pair.getValue());
        }

        // TODO(wider "armor items" scope): CE's real body additionally hardcodes, with
        // helmet=0.2/chest=0.4/legs=0.3/boots=0.1 as the per-slot multiplier and
        // hazYellow=0.6/hazRed=1.0/hazGray=2.0/paa=1.7/liquidator=2.4/security=0.825/star=1.0/
        // cmb=1.3/schrab=3.0/euph=10.0/iron=0.0225/gold=0.0225/steel=0.045/titanium=0.045/
        // alloy=0.07/cobalt=0.125 as the per-material coefficients:
        //   hazmat_helmet/_plate/_legs/_boots            -> hazYellow * <slot>
        //   hazmat_helmet_red/_plate_red/_legs_red/_boots_red   -> hazRed * <slot>
        //   hazmat_helmet_grey/_plate_grey/_legs_grey/_boots_grey -> hazGray * <slot>
        //   hazmat_paa_helmet/_plate/_legs/_boots, paa_plate/_legs/_boots -> paa * <slot>
        //   schrabidium_helmet/_plate/_legs/_boots        -> schrab * <slot>
        //   euphemium_helmet/_plate/_legs/_boots          -> euph * <slot>
        //   gas_mask -> 0.07 (flat), gas_mask_m65 -> 0.095 (flat)
        //   liquidator_helmet/_plate/_legs/_boots, security_*, starmetal_*, steel_*, titanium_*,
        //   cobalt_*, alloy_*, cmb_*, jackt/jackt2 (flat 0.1), vanilla IRON_*/GOLDEN_* armor
        // None of the non-hazmat/gas-mask/schrabidium/euphemium items above exist in this port yet
        // (a wider "armor items" scope than this package) - this package registers the hazmat/gas-
        // mask/schrabidium/euphemium items themselves (com.hbm.items.gear.SpecialArmorItems) but,
        // per the task brief, deliberately leaves this table itself for whoever finishes the rest
        // of the armor-item catalog to populate in one pass rather than half-filling it here.
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
     * TODO(ItemModCladding not yet ported): CE's real body first checks a per-stack persistent
     * "hfr_cladding" float (a rarely-used direct stat override, e.g. for admin/creative-set gear)
     * and then, failing that, {@code ArmorModHandler.pryMods(stack)[ArmorModHandler.cladding]}'s
     * item for an {@code ItemModCladding} instance's {@code .rad} field. Neither the persistent
     * float (no {@code HbmDataComponents} entry exists for it) nor {@code ItemModCladding} (a
     * concrete {@code ItemArmorMod} leaf) exist anywhere in this port yet - both belong to a wider
     * "armor items"/armor-mod-chip scope than this package. Stubbed to 0, matching this port's own
     * established null-safety fallback idiom for this exact gap
     * ({@code ArmorUtil.isFaradayArmor} used to inline this same stub before this class existed).
     */
    public static double getCladding(ItemStack stack) {
        return 0D;
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
