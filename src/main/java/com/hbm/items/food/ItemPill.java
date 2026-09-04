package com.hbm.items.food;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.VersatileConfig;
import com.hbm.damage.ModDamageTypes;
import com.hbm.potion.HbmPotionEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
 * {@code HbmLivingProps} asbestos/black-lung/radiation/digamma writes are wired 1:1 now that the
 * facade exists: {@code siox}/{@code pill_herbal} zero asbestos and cap black lung at
 * {@code MAX_BLACKLUNG / 5}; {@code pill_herbal} also {@code incrementRadiation(-100F)};
 * {@code xanax} subtracts 0.5 digamma floored at 0; {@code fmn} caps digamma at 2D (2000mDRX);
 * {@code five_htp} zeroes digamma. {@code VersatileConfig.hasPotionSickness} gates {@link #use}.
 * {@code pill_herbal}'s milk-curative immunity stays skipped (no 1.21 per-application
 * {@code setCurativeItems}). {@code plan_c}/{@code chocolate} self-damage use
 * {@link ModDamageTypes#EUTHANIZED_SELF}/{@link ModDamageTypes#EUTHANIZED_SELF_2}/
 * {@link ModDamageTypes#OVERDOSE}.
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

        VersatileConfig.applyPotionSickness(player, 5);

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
                player.removeEffect(HbmPotionEffects.RADIATION);
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
            case "pill_red" -> player.addEffect(new MobEffectInstance(HbmPotionEffects.DEATH, 60 * 60 * 20, 0));
            case "radx" -> player.addEffect(new MobEffectInstance(HbmPotionEffects.RADX, 3 * 60 * 20, 3));
            case "siox" -> {
                HbmLivingProps.setAsbestos(player, 0);
                HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player),
                        HbmLivingAttachment.MAX_BLACKLUNG / 5));
            }
            case "pill_herbal" -> {
                HbmLivingProps.setAsbestos(player, 0);
                HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player),
                        HbmLivingAttachment.MAX_BLACKLUNG / 5));
                HbmLivingProps.incrementRadiation(player, -100F);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 60 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10 * 60 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 5 * 20, 2));
                // TODO(curative-item override, docs/phase4/hbm_potion_system.md Open questions):
                // CE makes this specific potionsickness grant immune to milk curing via a
                // per-instance PotionEffect#setCurativeItems(emptyList) override. Modern MobEffect
                // curability is EffectCure-based and class-level (MobEffect#fillEffectCures), with
                // no confirmed 1.21.1 equivalent for a per-application override - granted below as
                // an ordinary (milk-curable) potionsickness instance until that's confirmed.
                player.addEffect(new MobEffectInstance(HbmPotionEffects.POTIONSICKNESS, 10 * 60 * 20, 0));
            }
            case "xanax" -> {
                double digamma = HbmLivingProps.getDigamma(player);
                HbmLivingProps.setDigamma(player, Math.max(digamma - 0.5D, 0D));
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
                double digamma = HbmLivingProps.getDigamma(player);
                HbmLivingProps.setDigamma(player, Math.min(digamma, 2D));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
            case "five_htp" -> {
                HbmLivingProps.setDigamma(player, 0D);
                player.addEffect(new MobEffectInstance(HbmPotionEffects.STABILITY, 10 * 60 * 20, 0));
            }
            default -> {
            }
        }

        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (VersatileConfig.hasPotionSickness(player)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
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
