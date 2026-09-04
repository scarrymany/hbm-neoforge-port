package com.hbm.items.food;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.VersatileConfig;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.gear.GearItems;
import com.hbm.items.special.ItemSimpleConsumable;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * CE's {@code VersatileConfig.applyPotionSickness(player, 5)},
 * {@code ContaminationUtil.contaminate(..., RADIATION, RAD_BYPASS, ...)} on the nuka/cherry/sparkle
 * (5.0) and quantum/rad (15.0) bottles, {@code HbmLivingProps.incrementRadiation(player, 500F)} on
 * {@code coffee_radium}, and {@code ExplosionLarge.explode(..., 50, true, false, false)} on
 * {@code chocolate_milk} are wired here 1:1. Polaroid-ID tooltip forks stay skipped (no invent).
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

        // c15 addition (docs/phase5/advancement_and_recipe_datagen_assets.md section 1.8's open
        // question, resolved): achradium.json's minecraft:consume_item criterion needs
        // CriteriaTriggers.CONSUME_ITEM fired for hbm:coffee_radium. ItemEnergy overrides Item#
        // finishUsingItem the same way as the confirmed-real, compiling Neo Edition reference's own
        // com.hbm.items.food.EnergyItem#finishUsingItem (upstream/neo-edition, read directly) -
        // and that class, despite going through the exact same
        // getUseDuration/getUseAnimation(DRINK)/ItemUtils.startUsingInstantly "hold to drink"
        // pipeline this class uses, still calls CriteriaTriggers.CONSUME_ITEM.trigger(...) explicitly
        // right here rather than relying on it firing automatically from some outer wrapper - so it
        // does not fire "for free" in 1.21.1 either. Matched verbatim (same placement, same stack
        // reference, before the effect switch below).
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        VersatileConfig.applyPotionSickness(player, 5);

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
                ContaminationUtil.contaminate(player, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 5.0F);
            }
            case "bottle_cherry" -> {
                player.heal(6F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30 * 20, 2));
                ContaminationUtil.contaminate(player, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 5.0F);
            }
            case "bottle_quantum" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 1));
                ContaminationUtil.contaminate(player, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 15.0F);
            }
            case "bottle_sparkle" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120 * 20, 1));
                ContaminationUtil.contaminate(player, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 5.0F);
            }
            case "bottle_rad" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120 * 20, 4));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120 * 20, 1));
                ContaminationUtil.contaminate(player, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 15.0F);
            }
            case "coffee" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 2));
            }
            case "coffee_radium" -> {
                player.heal(10F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 2));
                HbmLivingProps.incrementRadiation(player, 500F);
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
            case "chocolate_milk" ->
                    ExplosionLarge.explode(level, player, player.getX(), player.getY(), player.getZ(), 50, true, false, false);
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
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "can_smart" -> tooltip.add(Component.literal("Cheap and full of bubbles"));
            case "can_creature" -> tooltip.add(Component.literal("Basically gasoline in a tin can"));
            case "can_redbomb" -> tooltip.add(Component.literal("Liquefied explosives"));
            case "can_mrsugar" -> tooltip.add(Component.literal("An intellectual drink, for the chosen ones!"));
            case "can_overcharge" -> tooltip.add(Component.literal(
                    "Possible side effects include heart attacks, seizures or zombification"));
            case "can_luna" -> tooltip.add(Component.literal("Contains actual selenium and star metal. Tastes like night."));
            case "can_bepis" -> tooltip.add(Component.literal("beppp"));
            case "can_breen" -> {
                tooltip.add(Component.literal("Don't drink the water. They put something in it, to make you forget."));
                tooltip.add(Component.literal("I don't even know how I got here."));
            }
            case "chocolate_milk" -> {
                tooltip.add(Component.literal("Regular chocolate milk. Safe to drink."));
                tooltip.add(Component.literal("Totally not made from nitroglycerine."));
            }
            case "bottle_nuka" -> tooltip.add(Component.literal("Contains about 210 kcal and 1500 mSv."));
            case "bottle_cherry" -> tooltip.add(Component.literal(
                    "Now with severe radiation poisoning in every seventh bottle!"));
            case "bottle_quantum" -> tooltip.add(Component.literal("Comes with a colorful mix of over 70 isotopes!"));
            case "bottle2_korl" -> tooltip.add(Component.literal("Contains actual orange juice!"));
            case "bottle2_fritz" -> tooltip.add(Component.literal("moremore caffeine"));
            // TODO(MainRegistry.polaroidID follow-up): CE picks between two lines here depending on
            // MainRegistry.polaroidID == 11 (an unported easter-egg flag); polaroidID doesn't exist in
            // this port yet, so this always shows CE's non-taint ("else") line.
            case "bottle_sparkle" -> tooltip.add(Component.literal("The most delicious beverage in the wasteland!"));
            case "bottle_rad" -> tooltip.add(Component.literal("Tastes like radish and radiation."));
            default -> {
            }
        }

        if (requiresOpener) {
            tooltip.add(Component.literal("[Requires bottle opener]"));
        }
    }
}
