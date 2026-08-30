package com.hbm.items.tool;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Handheld ambient radiation-click detector. Ported from CE's
 * {@code com.hbm.items.tool.ItemGeigerCounter}.
 *
 * <p>Plain held item, not a Curios/Baubles accessory - see {@link ItemDosimeter}'s javadoc for the
 * documented Phase 1 decision shared by all four detector/diagnostic items in this package.
 *
 * <p>Ambient clicking and the right-click readout both use {@link ItemDosimeter#getReceivedRads}
 * (this port's real equivalent of CE's {@code ContaminationUtil.getActualPlayerRads}, minus the
 * hazmat resistance multiplier - see that class's javadoc). CE's other {@code onItemUse} override
 * (upgrading to {@code survey_scanner} on right-clicking {@code block_red_copper}) is not ported:
 * that block does not exist in this port's still-skeletal {@code ModBlocks} yet.
 */
public class ItemGeigerCounter extends Item {

    public ItemGeigerCounter(Properties properties) {
        super(properties);
    }

    public static void playGeiger(Level level, Player player) {
        if (level.isClientSide() || level.getGameTime() % 5 != 0) {
            return;
        }

        double rads = ItemDosimeter.getReceivedRads(player);
        SoundEvent[] geigerSounds = HBMSoundHandler.geigerSounds();

        if (rads > 1e-5) {
            List<Integer> tiers = new ArrayList<>();
            if (rads < 1) tiers.add(0);
            if (rads < 5) tiers.add(0);
            if (rads < 10) tiers.add(1);
            if (rads > 5 && rads < 15) tiers.add(2);
            if (rads > 10 && rads < 20) tiers.add(3);
            if (rads > 15 && rads < 25) tiers.add(4);
            if (rads > 20 && rads < 30) tiers.add(5);
            if (rads > 25) tiers.add(6);

            int tier = tiers.get(level.getRandom().nextInt(tiers.size()));
            if (tier > 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), geigerSounds[tier - 1], SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        } else if (level.getRandom().nextInt(100) == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), geigerSounds[0], SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player) {
            playGeiger(level, player);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            double rads = HbmLivingAttachment.getData(player).getRadBuf();
            player.sendSystemMessage(Component.literal("===== Geiger Counter =====").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal(String.format("Ambient dose: %.3f RAD/s", rads)).withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResultHolder.success(stack);
    }
}
