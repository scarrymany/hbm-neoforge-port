package com.hbm.items.food;

import com.hbm.items.special.ItemSimpleConsumable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code ItemConserve} ("canned conserve"): flattened from CE's single
 * metadata-multi item (27 {@code EnumFoodType} damage variants sharing one {@code ItemStack}) into 27
 * distinct registry entries, {@code hbm:canned_<name>} (see {@link FoodItems}), per
 * docs/phase1/items_food_gear.md's flattening table. {@link FoodType} keeps CE's
 * {@code EnumFoodType}'s per-variant food/saturation values; the variant itself is now "which item is
 * it" rather than a stack damage value.
 * <p>
 * CE gives every can a {@code can_key} on eating (self-contained, ported directly below).
 * {@link FoodType#BHOLE} additionally spawns an {@code EntityVortex} and {@link FoodType#FIST} deals 2
 * magic damage to the eater - both are TODO'd per docs/phase1/items_food_gear.md's explicit guidance
 * (the vortex entity doesn't exist in this port yet; the magic-damage branch is deferred alongside it
 * rather than singled out for early implementation). {@link FoodType#RECURSION} self-duplicates 90% of
 * the time, which is fully self-contained and ported directly.
 */
public class ItemConserve extends Item {

    public enum FoodType {
        BEEF(8, 0.75F),
        TUNA(4, 0.75F),
        MYSTERY(6, 0.5F),
        PASHTET(4, 0.5F),
        CHEESE(3, 1F),
        JIZZ(15, 5F),
        MILK(5, 0.25F),
        ASS(6, 0.75F),
        PIZZA(8, 0.75F),
        TUBE(2, 0.25F),
        TOMATO(4, 0.5F),
        ASBESTOS(7, 1F),
        BHOLE(10, 1F),
        HOTDOGS(5, 0.75F),
        LEFTOVERS(1, 0.1F),
        YOGURT(3, 0.5F),
        STEW(5, 0.5F),
        CHINESE(6, 0.1F),
        OIL(3, 1F),
        FIST(6, 0.75F),
        SPAM(8, 1F),
        FRIED(10, 0.75F),
        NAPALM(6, 1F),
        DIESEL(6, 1F),
        KEROSENE(6, 1F),
        RECURSION(1, 1F),
        BARK(2, 1F);

        public final int foodLevel;
        public final float saturation;

        FoodType(int foodLevel, float saturation) {
            this.foodLevel = foodLevel;
            this.saturation = saturation;
        }
    }

    private final FoodType type;

    public ItemConserve(FoodType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public FoodType getFoodType() {
        return type;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof Player player) {
            ItemSimpleConsumable.tryAddItem(player, new ItemStack(FoodItems.CAN_KEY.get()));
        }

        if (!level.isClientSide()) {
            switch (type) {
                case BHOLE -> {
                    // TODO(entities follow-up, docs/phase1/items_food_gear.md): CE spawns an
                    // EntityVortex (0.5 size, 0.01 shrink rate, no-break) at the eater's position here -
                    // EntityVortex doesn't exist in this port yet (a Phase 2 entity).
                }
                case FIST -> {
                    // TODO(follow-up, docs/phase1/items_food_gear.md): CE deals 2 magic damage to the
                    // eater here (DamageSource.MAGIC) - deferred alongside BHOLE per the research
                    // report's explicit guidance rather than implemented ahead of it.
                }
                case RECURSION -> {
                    // CE hands the eater a brand-new stack of 1 (not a bigger held stack) 90% of the
                    // time - matched literally here rather than growing the returned/held stack.
                    if (entity instanceof Player player && level.getRandom().nextInt(10) > 0) {
                        ItemSimpleConsumable.tryAddItem(player, new ItemStack(this, 1));
                    }
                }
                default -> {
                }
            }
        }

        return result;
    }
}
