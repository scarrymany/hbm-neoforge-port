package com.hbm.items.food;

import com.hbm.capability.HbmPlayerAttachment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code ItemFlask} ({@code flask_infusion}). CE built this on the
 * {@code ItemEnumMulti<EnumInfusion>} metadata-multi machinery but only ever declared one enum value
 * ({@code SHIELD}); per docs/phase1/items_food_gear.md's recommendation this is de-generified into a
 * single plain item instead of a 1-variant generic class - there is no metadata axis left to flatten.
 * <p>
 * CE's {@code HbmCapability.IHBMData.getShield/setShield/getMaxShield/setMaxShield/
 * getEffectiveMaxShield} map directly onto the already-ported {@link HbmPlayerAttachment}'s methods of
 * the same names, and {@code HbmCapability.IHBMData.shieldCap} onto
 * {@link HbmPlayerAttachment#SHIELD_CAP} - fully Phase-1-safe, nothing deferred.
 */
public class ItemFlask extends Item {

    public ItemFlask(Properties properties) {
        super(properties);
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
        ItemStack stack = player.getItemInHand(hand);
        if (HbmPlayerAttachment.getData(player).getMaxShield() >= HbmPlayerAttachment.SHIELD_CAP) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        if (!(entityLiving instanceof Player player)) {
            return stack;
        }
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        if (level.isClientSide()) {
            return stack;
        }

        float infusion = 5F;
        HbmPlayerAttachment data = HbmPlayerAttachment.getData(player);
        data.setMaxShield(Math.min(HbmPlayerAttachment.SHIELD_CAP, data.getMaxShield() + infusion));
        data.setShield(Math.min(data.getShield() + infusion, data.getEffectiveMaxShield(player)));
        return stack;
    }
}
