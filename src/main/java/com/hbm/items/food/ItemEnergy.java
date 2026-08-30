package com.hbm.items.food;

import com.hbm.items.gear.GearItems;
import com.hbm.items.special.ItemSimpleConsumable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Port of CE's {@code ItemEnergy}: cans/bottles (~24 CE instances share this one class, see
 * {@link FoodItems}), each drinkable for a hardcoded potion-effect bundle and, unless the drinker is
 * creative, swapped out for an empty container (+ a cap/ring-pull item) on finish. CE dispatched
 * per-instance behavior via {@code this == ModItems.X} identity checks; this port switches on the
 * item's own registry path instead (same technique as the existing {@link ItemLemon}), since every
 * CE field is already its own distinct registered item post-flattening.
 * <p>
 * <b>Not ported (see docs/phase1/items_food_gear.md finding #2 - flagged per-branch below, not
 * silently dropped):</b> every CE branch called {@code VersatileConfig.applyPotionSickness(player, 5)}
 * unconditionally before its own effects; the radiation-flavored branches additionally called
 * {@code ContaminationUtil.contaminate(...)} or {@code HbmLivingProps.incrementRadiation(...)}. None of
 * {@code HbmPotion}, {@code HbmLivingProps}, or {@code ContaminationUtil} exist in this port yet, so
 * those specific calls are left as TODOs next to the vanilla effects they would have accompanied. CE's
 * {@code chocolate_milk} branch (a 50-power {@code ExplosionLarge.explode(...)}) is TODO'd for the same
 * reason (this area does not own the explosion system).
 */
public class ItemEnergy extends Item {

    private Supplier<? extends Item> container;
    private Supplier<? extends Item> cap;
    private boolean requiresOpener;

    public ItemEnergy(Properties properties) {
        super(properties);
    }

    /** CE's {@code makeCan()}: no opener required. */
    public ItemEnergy makeCan(Supplier<? extends Item> container, Supplier<? extends Item> cap) {
        this.container = container;
        this.cap = cap;
        this.requiresOpener = false;
        return this;
    }

    /** CE's {@code makeBottle(Item, Item)}: requires {@link GearItems#BOTTLE_OPENER} in either hand. */
    public ItemEnergy makeBottle(Supplier<? extends Item> container, Supplier<? extends Item> cap) {
        this.container = container;
        this.cap = cap;
        this.requiresOpener = true;
        return this;
    }

    public static boolean hasOpener(Player player) {
        return player.getMainHandItem().is(GearItems.BOTTLE_OPENER.get())
                || player.getOffhandItem().is(GearItems.BOTTLE_OPENER.get());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (requiresOpener && !hasOpener(player)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return stack;
        }

        // TODO(VersatileConfig follow-up, docs/phase1/items_food_gear.md finding #2): CE calls
        // VersatileConfig.applyPotionSickness(player, 5) here before every branch below - not ported,
        // since neither that method nor the HbmPotion.potionsickness effect it grants exist yet.

        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "can_smart" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 0));
            }
            case "can_creature" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30 * 20, 1));
            }
            case "can_redbomb" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 30 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30 * 20, 1));
            }
            case "can_mrsugar" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30 * 20, 2));
            }
            case "can_overcharge" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 0));
            }
            case "can_luna" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30 * 20, 2));
            }
            case "can_bepis" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 3));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 3));
            }
            case "can_breen" -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 30 * 20, 0));
            case "can_mug" -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3 * 60 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60 * 20, 2));
            }
            case "bottle_nuka" -> {
                player.heal(4F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30 * 20, 1));
                // TODO(ContaminationUtil follow-up): CE contaminates the drinker with 5.0 RAD_BYPASS
                // radiation here (ContaminationUtil.contaminate(player, RADIATION, RAD_BYPASS, 5.0F)).
            }
            case "bottle_cherry" -> {
                player.heal(6F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30 * 20, 2));
                // TODO(ContaminationUtil follow-up): see bottle_nuka above (5.0 RAD_BYPASS radiation).
            }
            case "bottle_quantum" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 1));
                // TODO(ContaminationUtil follow-up): see bottle_nuka above (15.0 RAD_BYPASS radiation).
            }
            case "bottle_sparkle" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120 * 20, 1));
                // TODO(ContaminationUtil follow-up): see bottle_nuka above (5.0 RAD_BYPASS radiation).
            }
            case "bottle_rad" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120 * 20, 4));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120 * 20, 1));
                // TODO(ContaminationUtil follow-up): see bottle_nuka above (15.0 RAD_BYPASS radiation).
            }
            case "coffee" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 2));
            }
            case "coffee_radium" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 2));
                // TODO(HbmLivingProps follow-up): CE calls HbmLivingProps.incrementRadiation(player, 500F)
                // here - HbmLivingProps doesn't exist in this port yet.
            }
            case "bottle2_korl" -> {
                player.heal(6F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 2));
            }
            case "bottle2_fritz" -> {
                player.heal(6F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30 * 20, 2));
            }
            case "bottle2_korl_special" -> {
                player.heal(16F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120 * 20, 2));
            }
            case "bottle2_fritz_special" -> {
                player.heal(16F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120 * 20, 2));
            }
            case "bottle2_sunset" -> {
                player.heal(6F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60 * 20, 2));
            }
            case "chocolate_milk" -> {
                // TODO(explosion-system follow-up): CE detonates a 50-power ExplosionLarge.explode(...)
                // here - out of this area's scope (owned by whichever Phase area ports the explosion system).
            }
            default -> {
            }
        }

        if (!player.hasInfiniteMaterials()) {
            if (cap != null) {
                ItemSimpleConsumable.tryAddItem(player, new ItemStack(cap.get()));
            }
            if (container != null) {
                if (stack.getCount() <= 1) {
                    stack.shrink(1);
                    return new ItemStack(container.get());
                }
                ItemSimpleConsumable.tryAddItem(player, new ItemStack(container.get()));
            }
            stack.shrink(1);
        }

        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (requiresOpener) {
            tooltip.add(Component.literal("[Requires bottle opener]"));
        }
    }
}
