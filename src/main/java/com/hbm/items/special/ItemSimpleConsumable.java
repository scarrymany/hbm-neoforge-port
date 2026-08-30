package com.hbm.items.special;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code ItemSimpleConsumable}: a generic, lambda-configurable {@link ItemCustomLore}
 * subclass used to build many syringe/food items concisely, preserved as the report recommends. The
 * client/server split CE modeled with {@code World.isRemote} checks becomes an explicit
 * {@code level.isClientSide()} check on the server-only delegate variants, same behavior.
 */
public class ItemSimpleConsumable extends ItemCustomLore {

    private BiConsumer<ItemStack, Player> useAction;
    private BiConsumer<ItemStack, Player> useActionServer;
    private BiConsumer<ItemStack, LivingEntity> hitAction;
    private BiConsumer<ItemStack, LivingEntity> hitActionServer;

    public ItemSimpleConsumable(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (useAction != null) {
            useAction.accept(stack, player);
        }
        if (!level.isClientSide() && useActionServer != null) {
            useActionServer.accept(stack, player);
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (hitAction != null) {
            hitAction.accept(stack, target);
        }
        if (!target.level().isClientSide() && hitActionServer != null) {
            hitActionServer.accept(stack, target);
        }
        return false;
    }

    public static void giveSoundAndDecrement(ItemStack stack, LivingEntity entity, SoundEvent sound, ItemStack container) {
        stack.shrink(1);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        tryAddItem(entity, container);
    }

    public static void addPotionEffect(LivingEntity entity, MobEffectInstance effect) {
        MobEffectInstance existing = entity.getEffect(effect.getEffect());
        if (existing == null) {
            entity.addEffect(effect);
        } else {
            int duration = effect.getDuration();
            if (effect.getAmplifier() == existing.getAmplifier()) {
                duration += existing.getDuration();
            }
            entity.addEffect(new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier()));
        }
    }

    public static void tryAddItem(LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player player && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public ItemSimpleConsumable setUseAction(BiConsumer<ItemStack, Player> delegate) {
        this.useAction = delegate;
        return this;
    }

    public ItemSimpleConsumable setUseActionServer(BiConsumer<ItemStack, Player> delegate) {
        this.useActionServer = delegate;
        return this;
    }

    public ItemSimpleConsumable setHitAction(BiConsumer<ItemStack, LivingEntity> delegate) {
        this.hitAction = delegate;
        return this;
    }

    public ItemSimpleConsumable setHitActionServer(BiConsumer<ItemStack, LivingEntity> delegate) {
        this.hitActionServer = delegate;
        return this;
    }
}
