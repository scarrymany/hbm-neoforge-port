package com.hbm.items.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Port of CE's {@code ItemTemFlakes}: flattened from CE's single instance with 3 identical-behavior
 * damage-value tiers (differing only in tooltip text) into 3 distinct registry entries -
 * {@code tem_flakes_low}, {@code tem_flakes_mid}, {@code tem_flakes_high} (see {@link FoodItems}), per
 * docs/phase1/items_food_gear.md's explicit instruction to reuse
 * {@link ItemAppleSchrabidium}'s {@code _low}/{@code _mid}/{@code _high} tier-naming convention (CE
 * itself only ever named the base field {@code tem_flakes}, with no per-tier names).
 * <p>
 * {@code player.heal(2.0F)} isn't expressible through {@code FoodProperties.Builder#effect} (that only
 * adds {@link net.minecraft.world.effect.MobEffectInstance}s), so this needs the one small override
 * below rather than being fully declarative like {@link ItemLemon}'s catalog entries.
 */
public class ItemTemFlakes extends Item {

    public ItemTemFlakes(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide()) {
            entity.heal(2.0F);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "tem_flakes_low" -> tooltip.add(Component.literal("Heals 2HP DISCOUNT FOOD OF TEM!!!"));
            case "tem_flakes_mid" -> tooltip.add(Component.literal("Heals 2HP food of tem"));
            case "tem_flakes_high" -> tooltip.add(Component.literal("Heals food of tem (expensiv)"));
            default -> {
            }
        }
    }
}
