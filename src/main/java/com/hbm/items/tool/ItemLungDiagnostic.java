package com.hbm.items.tool;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Lung-condition diagnostic readout (asbestosis / black lung / MKU contagion). Ported from CE's
 * {@code com.hbm.items.tool.ItemLungDiagnostic}.
 *
 * <p>CE's readout came from {@code HbmLivingProps.getAsbestos}/{@code getBlackLung}/
 * {@code getContagion}. This port's already-ported {@link HbmLivingAttachment} carries the same
 * fields ({@link HbmLivingAttachment#getAsbestos()}, {@link HbmLivingAttachment#getBlacklung()},
 * {@link HbmLivingAttachment#getContagion()}) plus their {@link HbmLivingAttachment#MAX_ASBESTOS}/
 * {@link HbmLivingAttachment#MAX_BLACKLUNG} caps, so the "percent lung health remaining" readout
 * below (CE: {@code 100 - accumulated/max*100}) is real, unlike the other three detector items in
 * this package. Not a Curios/Baubles accessory in CE to begin with (CE's {@code ItemLungDiagnostic}
 * never implemented {@code IBauble}), so no accessory-behavior decision applies here.
 */
public class ItemLungDiagnostic extends Item {

    public ItemLungDiagnostic(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            HbmLivingAttachment data = HbmLivingAttachment.getData(player);
            float asbestosHealth = 100F - ((int) (10000F * data.getAsbestos() / HbmLivingAttachment.MAX_ASBESTOS)) / 100F;
            float blacklungHealth = 100F - ((int) (10000F * data.getBlacklung() / HbmLivingAttachment.MAX_BLACKLUNG)) / 100F;
            float totalHealth = asbestosHealth * blacklungHealth / 100F;
            int contagion = data.getContagion();

            player.sendSystemMessage(Component.literal("===== L Lung Diagnostic L =====").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal(String.format("Asbestos health: %6.2f%%", asbestosHealth)).withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal(String.format("Coal health: %6.2f%%", blacklungHealth)).withStyle(ChatFormatting.DARK_GRAY));
            player.sendSystemMessage(Component.literal(String.format("Total lung health: %6.2f%%", totalHealth)).withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("MKU contagion: " + (contagion > 0 ? "positive" : "negative")).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResultHolder.success(stack);
    }
}
