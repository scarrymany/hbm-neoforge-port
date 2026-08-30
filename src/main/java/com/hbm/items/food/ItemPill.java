package com.hbm.items.food;

import com.hbm.damage.ModDamageTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Port of CE's {@code ItemPill}: ~13 separate pill/medicine instances (see {@link FoodItems}) sharing
 * one class, each with a hardcoded effect branch dispatched (here) off the item's own registry path,
 * same technique as {@link ItemEnergy}/the existing {@link ItemLemon}.
 * <p>
 * <b>Not ported (see docs/phase1/items_food_gear.md finding #2 - flagged per-branch below, not
 * silently dropped):</b> CE calls {@code VersatileConfig.applyPotionSickness(player, 5)} before every
 * branch, and most branches also read/write {@code HbmLivingProps}' asbestos/black-lung/radiation/
 * digamma state or apply an {@code HbmPotion} effect (`radx`, `death`, `stability`, and removing
 * `HbmPotion.radiation`). None of {@code HbmPotion}/{@code HbmLivingProps}/{@code VersatileConfig
 * .applyPotionSickness} exist in this port yet, so those specific calls are TODO'd next to the
 * vanilla-only pieces of each branch, which are ported directly. {@code plan_c} and {@code chocolate}'s
 * self-damage branches use the already-ported {@link ModDamageTypes#EUTHANIZED_SELF}/
 * {@link ModDamageTypes#EUTHANIZED_SELF_2}/{@link ModDamageTypes#OVERDOSE} damage types in place of
 * CE's unported {@code com.hbm.lib.ModDamageSource} singletons of (almost) the same name.
 */
public class ItemPill extends Item {

    public ItemPill(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (level.isClientSide() || !(livingEntity instanceof Player player)) {
            return result;
        }

        // TODO(VersatileConfig follow-up, docs/phase1/items_food_gear.md finding #2): CE calls
        // VersatileConfig.applyPotionSickness(player, 5) here before every branch below - not ported.

        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "pill_iodine" -> {
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.CONFUSION);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
                player.removeEffect(MobEffects.HUNGER);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.WITHER);
                // TODO(HbmPotion follow-up): CE also removes HbmPotion.radiation here - unported effect.
            }
            case "plan_c" -> {
                // CE: 10x attackEntityFrom(random ModDamageSource.euthanizedSelf/euthanizedSelf2, 1000) -
                // guaranteed-lethal "deadly" pill. ModDamageSource itself isn't ported, but the equivalent
                // DamageType keys already exist in ModDamageTypes; use those directly.
                for (int i = 0; i < 10; i++) {
                    var type = level.getRandom().nextBoolean() ? ModDamageTypes.EUTHANIZED_SELF : ModDamageTypes.EUTHANIZED_SELF_2;
                    player.hurt(level.damageSources().source(type), 1000F);
                }
            }
            case "pill_red" -> {
                // TODO(HbmPotion follow-up): CE applies HbmPotion.death (60*60*20 ticks, amp 0) here - unported.
            }
            case "radx" -> {
                // TODO(HbmPotion follow-up): CE applies HbmPotion.radx (3*60*20 ticks, amp 3) here - unported.
            }
            case "siox" -> {
                // TODO(HbmLivingProps follow-up): CE zeroes asbestos and caps black lung to 1/5 max here -
                // HbmLivingProps doesn't exist in this port yet.
            }
            case "pill_herbal" -> {
                // TODO(HbmLivingProps follow-up): CE also zeroes asbestos, caps black lung to 1/5 max, and
                // decrements radiation by 100 here - HbmLivingProps doesn't exist in this port yet.
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 60 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10 * 60 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 5 * 20, 2));
                // TODO(HbmPotion follow-up): CE also applies a 10-minute, curative-item-immune
                // HbmPotion.potionsickness here - unported effect.
            }
            case "xanax" -> {
                // TODO(HbmLivingProps follow-up): CE reduces digamma by 0.5 (floored at 0) here - digamma
                // tracking doesn't exist in this port yet.
            }
            case "chocolate" -> {
                if (level.getRandom().nextInt(25) == 0) {
                    player.hurt(level.damageSources().source(ModDamageTypes.OVERDOSE), 1000F);
                }
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60 * 20, 3));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 3));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60 * 20, 3));
            }
            case "fmn" -> {
                // TODO(HbmLivingProps follow-up): CE caps digamma at 2,000mDRX here - unported.
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
            case "five_htp" -> {
                // TODO(HbmLivingProps/HbmPotion follow-up): CE zeroes digamma and applies a 10-minute
                // HbmPotion.stability here - neither exists in this port yet.
            }
            default -> {
            }
        }

        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "pill_iodine" -> tooltip.add(Component.literal("Removes negative effects"));
            case "plan_c" -> tooltip.add(Component.literal("Deadly"));
            case "radx" -> tooltip.add(Component.literal("Increases radiation resistance by 0.4 for 3 minutes"));
            case "siox" -> tooltip.add(Component.literal("Reverses mesothelioma with the power of Asbestos!"));
            case "pill_herbal" -> {
                tooltip.add(Component.literal("Effective treatment against lung disease and mild radiation poisoning"));
                tooltip.add(Component.literal("Comes with side effects"));
            }
            case "xanax" -> tooltip.add(Component.literal("Removes 500mDRX"));
            case "fmn" -> tooltip.add(Component.literal("Removes all DRX above 2,000mDRX"));
            case "five_htp" -> tooltip.add(Component.literal("Removes all DRX, Stability for 10 minutes"));
            default -> {
            }
        }
    }
}
