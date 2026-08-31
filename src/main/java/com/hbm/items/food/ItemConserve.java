package com.hbm.items.food;

import com.hbm.entity.effect.EntityVortex;
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
 * it" rather than a stack damage value. One deliberate exception: {@link FoodType#PIZZA}'s
 * saturation is 0.75F here rather than CE's literal (and near-certainly bugged) 75.0F - see the
 * comment on that constant.
 * <p>
 * CE gives every can a {@code can_key} on eating (self-contained, ported directly below).
 * {@link FoodType#BHOLE} spawns an {@link EntityVortex} at the eater's position, per
 * docs/phase1/items_food_gear.md's original TODO and docs/phase4/entities_vortex_gravity_wells.md's
 * follow-up (this is CE's own only real call site for {@code EntityVortex#setShrinkRate} - see that
 * method's own javadoc for why the call is made for parity despite having zero gameplay effect in real
 * CE). {@link FoodType#FIST} deals 2 magic damage to the eater - still TODO'd per docs/phase1/
 * items_food_gear.md's explicit guidance, out of this package's scope (unrelated to gravity wells).
 * {@link FoodType#RECURSION} self-duplicates 90% of the time, which is fully self-contained and ported
 * directly.
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
        // CE's real value is `075F` - a valid decimal float literal (leading zeros are
        // insignificant on floats; this is NOT octal) equal to 75.0F. CE's own source flags this
        // as a probable typo ("1.7 has it at 075F, idk why, typo maybe?") but never fixed it, so
        // 75.0F is technically the live CE behavior. This port deliberately corrects it to 0.75F
        // instead of reproducing the apparent bug - the one intentional deviation from this
        // class's otherwise verbatim EnumFoodType fidelity.
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
                    // CE: new EntityVortex(world, 0.5F).setShrinkRate(0.01F).noBreak(), spawned at the
                    // eater's position. setShrinkRate(0.01F) is called for parity even though
                    // EntityVortex#tick() never reads it (a real, documented CE bug - see that
                    // method's own javadoc).
                    EntityVortex vortex = (EntityVortex) new EntityVortex(level, 0.5F).setShrinkRate(0.01F).noBreak();
                    vortex.setPos(entity.getX(), entity.getY(), entity.getZ());
                    level.addFreshEntity(vortex);
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
