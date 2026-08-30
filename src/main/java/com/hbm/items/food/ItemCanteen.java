package com.hbm.items.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Port of CE's {@code ItemCanteen}: a reusable canteen with a reuse cooldown, 3 instances
 * ({@code canteen_13}, {@code canteen_vodka}, {@code canteen_fab} - see {@link FoodItems}).
 * <p>
 * CE reused the stack's item-damage value as a plain countdown timer (not real durability - see
 * docs/phase1/items_food_gear.md's NBT/Data-Component notes), decremented once per second in
 * {@code onUpdate}. This port tracks the same countdown as {@link FoodDataComponents#CANTEEN_COOLDOWN}
 * (a custom int component) instead of {@code DataComponents.DAMAGE}, since 1.21's damage bar is a
 * genuine "this item is worn out" concept and a canteen's cooldown is not that.
 * <p>
 * <b>Not ported (see docs/phase1/items_food_gear.md finding #2):</b> CE gates {@code onItemRightClick}
 * on {@code VersatileConfig.hasPotionSickness(player)} and calls
 * {@code VersatileConfig.applyPotionSickness(entityLiving, 5)} at the end of {@code onItemUseFinish} -
 * neither exists in this port yet (see {@link FoodDataComponents}'s sibling classes' javadoc for the
 * same gap), so both calls are TODO'd below rather than silently dropped.
 */
public class ItemCanteen extends Item {

    /** CE's constructor {@code int cooldown} parameter, in seconds (matches CE's {@code setMaxDamage(cooldown)}). */
    private final int cooldownSeconds;

    public ItemCanteen(int cooldownSeconds, Properties properties) {
        super(properties);
        this.cooldownSeconds = cooldownSeconds;
    }

    private static int getCooldown(ItemStack stack) {
        return stack.getOrDefault(FoodDataComponents.CANTEEN_COOLDOWN.get(), 0);
    }

    private static void setCooldown(ItemStack stack, int seconds) {
        if (seconds <= 0) {
            stack.remove(FoodDataComponents.CANTEEN_COOLDOWN.get());
        } else {
            stack.set(FoodDataComponents.CANTEEN_COOLDOWN.get(), seconds);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide()) {
            return;
        }
        int cooldown = getCooldown(stack);
        if (cooldown > 0 && entity.tickCount % 20 == 0) {
            setCooldown(stack, cooldown - 1);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        setCooldown(stack, cooldownSeconds);

        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "canteen_13" -> entityLiving.heal(5F);
            case "canteen_vodka" -> {
                entityLiving.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 0));
                entityLiving.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 2));
            }
            case "canteen_fab" -> {
                entityLiving.heal(10F);
                entityLiving.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 15 * 20, 0));
                entityLiving.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 2));
                entityLiving.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 2));
                entityLiving.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 1));
            }
            default -> {
            }
        }

        // TODO(VersatileConfig follow-up, docs/phase1/items_food_gear.md finding #2): CE calls
        // VersatileConfig.applyPotionSickness(entityLiving, 5) here - not ported, see class javadoc.

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // TODO(VersatileConfig follow-up, docs/phase1/items_food_gear.md finding #2): CE also gates this
        // on !VersatileConfig.hasPotionSickness(player) - not ported, see class javadoc. Only the
        // cooldown gate below is enforced for now.
        if (getCooldown(stack) > 0) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "canteen_13" -> {
                tooltip.add(Component.literal("Cooldown: 1 minute"));
                tooltip.add(Component.literal("Restores 2.5 hearts"));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("You take a sip from your trusty Vault 13 canteen."));
            }
            case "canteen_vodka" -> {
                tooltip.add(Component.literal("Cooldown: 3 minutes"));
                tooltip.add(Component.literal("Nausea I for 10 seconds"));
                tooltip.add(Component.literal("Strength III for 30 seconds"));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("Smells like disinfectant, tastes like disinfectant."));
            }
            case "canteen_fab" -> {
                tooltip.add(Component.literal("Cooldown: 2 minutes"));
                tooltip.add(Component.literal("Engages the fab drive"));
            }
            default -> {
            }
        }
    }
}
