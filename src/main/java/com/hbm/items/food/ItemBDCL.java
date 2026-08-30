package com.hbm.items.food;

import com.hbm.items.ItemBase;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code ItemBDCL} ("drink slowly", the {@code bdcl} item). CE extends
 * {@code com.hbm.items.ItemBakedBase}, a 1.12 dynamic-model-registration helper (bakes/registers its
 * own {@code IBakedModel}/sprite) with no behavior of its own beyond that - entirely superseded by
 * datagen in this port (see {@code ItemLemon}'s and {@code MachineItems}' javadocs on the same point).
 * {@code ItemBDCL} itself never uses any of {@code ItemBakedBase}'s model-registration methods, so the
 * already-existing, much simpler {@link ItemBase} covers everything this class actually needs -
 * creating a from-scratch {@code ItemBakedBase} port for this one caller was not warranted.
 * <p>
 * Not ported: CE's {@code onUsingTick} progressively shrinks the active stack every 4 ticks (a visual
 * "the bottle empties" trick) and plays a gulp sound every 5 ticks. 1.21's per-tick-while-using hook
 * has no confirmed usage anywhere in this port or the Neo Edition reference to check its exact method
 * name/signature against (docs/phase1/items_food_gear.md's ground rule: don't invent an API without a
 * real example) - rather than guess, this port keeps the core "drink slowly, then consume" behavior
 * (duration, DRINK animation, final shrink) and drops only that cosmetic mid-drink tick effect. The
 * finishing groan sound is kept, since {@code Level#playSound} is a confirmed, widely-used API in this
 * port.
 */
public class ItemBDCL extends ItemBase {

    public ItemBDCL(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        if (!level.isClientSide()) {
            level.playSound(null, entityLiving.getX(), entityLiving.getY(), entityLiving.getZ(),
                    HBMSoundHandler.groan.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (!(entityLiving instanceof Player player) || !player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        return stack;
    }
}
