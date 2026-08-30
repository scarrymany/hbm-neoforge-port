package com.hbm.items.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemLemon} - a shared base class for ~22 unrelated single-instance edible
 * items (lemon, loops, twinkie, med_ipecac, ingot_semtex, ...), each with its own
 * {@code this == ModItems.X} tooltip branch in {@code addInformation}. Post-flattening, every
 * registry name is already its own distinct {@link ItemLemon} instance (CE never gave these
 * metadata variants - each was already a separate field), so the identity check becomes a registry
 * path comparison instead.
 * <p>
 * CE's two {@code onFoodEaten} branches (a flat {@code Hunger} debuff on {@code med_ipecac}/
 * {@code med_ptsd}, and four buffs on {@code loop_stew}) and its one {@code onItemUseFinish}
 * container-swap ({@code loop_stew} -&gt; empty bowl) are not overridden here at all: 1.21's
 * {@link net.minecraft.world.food.FoodProperties.Builder#effect} and
 * {@link net.minecraft.world.food.FoodProperties.Builder#usingConvertsTo} bake both directly into
 * each item's {@code FoodProperties} at registration time (see {@link FoodItems}), and vanilla's own
 * default {@code Item#finishUsingItem} already applies them - matching CE's {@code ItemFood}
 * {@code setPotionEffect(effect, probability)} mechanic 1:1 with {@code probability = 1.0F}.
 * <p>
 * {@code ingot_semtex} (registered by the items_billet_ingot area's {@code IngotNuggetItems}) and
 * {@code powder_cement} (registered by {@code BilletPowderItems}) are the two CE {@code ItemLemon}
 * fields that land outside the food-consumables catalog (both PARTS-tab material resources); this
 * class stays their shared base since CE itself never split it, but this file (the food/gear area)
 * only registers the food-catalog fields - see {@link FoodItems}.
 */
public class ItemLemon extends Item {

    public ItemLemon(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "ingot_semtex" -> {
                tooltip.add(Component.literal("Semtex H Plastic Explosive"));
                tooltip.add(Component.literal("Performant explosive for many applications."));
                tooltip.add(Component.literal("Edible"));
            }
            case "lemon" -> tooltip.add(Component.literal("Eh, good enough."));
            case "med_ipecac" -> {
                tooltip.add(Component.literal("Bitter juice that will cause your stomach"));
                tooltip.add(Component.literal("to forcefully eject it's contents."));
            }
            case "med_ptsd" -> {
                tooltip.add(Component.literal("This isn't even PTSD mediaction, it's just"));
                tooltip.add(Component.literal("Ipecac in a different bottle!"));
            }
            case "med_schizophrenia" -> {
                tooltip.add(Component.literal("Makes the voices go away. Just for a while."));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("..."));
                tooltip.add(Component.literal("Better not take it."));
            }
            case "loops" -> tooltip.add(Component.literal("Brøther, may I have some lööps?"));
            case "loop_stew" -> tooltip.add(Component.literal("A very, very healthy breakfast."));
            case "twinkie" -> tooltip.add(Component.literal("Expired 600 years ago!"));
            case "pudding" -> {
                tooltip.add(Component.literal("What if he did?"));
                tooltip.add(Component.literal("What if he didn't?"));
                tooltip.add(Component.literal("What if the world was made of pudding?"));
            }
            case "marshmallow" -> tooltip.add(Component.literal("Gets grilled in the heat of burning nuclear failure"));
            case "marshmallow_roasted" -> tooltip.add(Component.literal("Hmmm... tastes a bit metallic"));
            default -> {
            }
        }
    }
}
