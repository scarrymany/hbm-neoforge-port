package com.hbm.items.food;

import com.hbm.damage.ModDamageTypes;
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
 * {@link ItemLemon}'s approach. {@code apple_lead}'s low/mid tiers use CE's unported
 * {@code HbmPotion.lead} effect and so are registered with no effect at all (TODO'd at the registration
 * site in {@link FoodItems}); its high tier instead deals 500 lethal damage via
 * {@code ModDamageSource.lead}, which this port implements directly through the already-ported
 * {@link ModDamageTypes#LEAD} damage type (see {@link #lethal}).
 */
public class ItemAppleSchrabidium extends Item {

    private final boolean hasEffect;
    private final boolean lethal;

    /**
     * @param hasEffect enchantment-glint flag; CE's {@code hasEffect} override returns
     *                  {@code stack.getItemDamage() == 2} regardless of which base item it is, i.e. only
     *                  the "_high" tier of either base glints.
     * @param lethal    true only for {@code apple_lead_high}: CE's meta-2 {@code apple_lead} branch deals
     *                  500 damage via {@code ModDamageSource.lead} instead of granting an effect.
     */
    public ItemAppleSchrabidium(Properties properties, boolean hasEffect, boolean lethal) {
        super(properties);
        this.hasEffect = hasEffect;
        this.lethal = lethal;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasEffect;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (lethal && !level.isClientSide()) {
            entity.hurt(level.damageSources().source(ModDamageTypes.LEAD), 500.0F);
        }
        return result;
    }
}
