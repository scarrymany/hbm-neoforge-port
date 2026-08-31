package com.hbm.items.food;

import com.hbm.damage.ModDamageTypes;
import com.hbm.potion.HbmPotionEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code ItemAppleSchrabidium}: flattened from CE's 2 base instances
 * ({@code apple_schrabidium}, {@code apple_lead}) x 3 damage-value tiers each into 6 distinct registry
 * entries (see {@link FoodItems}). CE never named the tiers - only meta 0/1/2 - so this port suffixes
 * each with {@code _low}/{@code _mid}/{@code _high}, per docs/phase1/items_food_gear.md's naming
 * recommendation.
 * <p>
 * {@code apple_schrabidium}'s three tiers are plain vanilla {@link net.minecraft.world.effect.MobEffects}
 * bundles and are declared entirely through each item's {@code FoodProperties.Builder#effect} list at
 * registration time (see {@link FoodItems}) - no per-instance code needed here, matching the existing
 * {@link ItemLemon}'s approach. {@code apple_lead}'s low/mid tiers grant
 * {@code com.hbm.potion.HbmPotionEffects#LEAD} (see {@link #leadDuration}/{@link #leadAmplifier},
 * wired at the registration site in {@link FoodItems} per CE's real
 * {@code upstream/hbm-ce ItemAppleSchrabidium#onFoodEaten} numbers: 15x20 ticks amp 2 for
 * {@code apple_lead_low}, 60x20 ticks amp 4 for {@code apple_lead_mid}); its high tier instead deals
 * 500 lethal damage via {@code ModDamageSource.lead}, which this port implements directly through the
 * already-ported {@link ModDamageTypes#LEAD} damage type (see {@link #lethal}).
 */
public class ItemAppleSchrabidium extends Item {

    private final boolean hasEffect;
    private final boolean lethal;
    private final int leadDuration;
    private final int leadAmplifier;

    /**
     * @param hasEffect enchantment-glint flag; CE's {@code hasEffect} override returns
     *                  {@code stack.getItemDamage() == 2} regardless of which base item it is, i.e. only
     *                  the "_high" tier of either base glints.
     * @param lethal    true only for {@code apple_lead_high}: CE's meta-2 {@code apple_lead} branch deals
     *                  500 damage via {@code ModDamageSource.lead} instead of granting an effect.
     */
    public ItemAppleSchrabidium(Properties properties, boolean hasEffect, boolean lethal) {
        this(properties, hasEffect, lethal, 0, 0);
    }

    /**
     * @param leadDuration  CE's {@code HbmPotion.lead} grant duration in ticks for
     *                      {@code apple_lead_low}/{@code apple_lead_mid} (0 = grant nothing; only
     *                      meaningful when {@code lethal} is false).
     * @param leadAmplifier CE's {@code HbmPotion.lead} grant amplifier for the same two tiers.
     */
    public ItemAppleSchrabidium(Properties properties, boolean hasEffect, boolean lethal, int leadDuration, int leadAmplifier) {
        super(properties);
        this.hasEffect = hasEffect;
        this.lethal = lethal;
        this.leadDuration = leadDuration;
        this.leadAmplifier = leadAmplifier;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasEffect;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide()) {
            if (lethal) {
                entity.hurt(level.damageSources().source(ModDamageTypes.LEAD), 500.0F);
            } else if (leadDuration > 0) {
                entity.addEffect(new MobEffectInstance(HbmPotionEffects.LEAD, leadDuration, leadAmplifier));
            }
        }
        return result;
    }
}
