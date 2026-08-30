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
 * Digamma-radiation exposure diagnostic readout. Ported from CE's
 * {@code com.hbm.items.tool.ItemDigammaDiagnostic}.
 *
 * <p>Plain held item, not a Curios/Baubles accessory - see {@link ItemDosimeter}'s javadoc for the
 * documented Phase 1 decision shared by all four detector/diagnostic items in this package.
 *
 * <p>CE's readout came from {@code HbmLivingProps.getDigamma(player)}. This port's already-ported
 * {@link HbmLivingAttachment} carries the same field ({@link HbmLivingAttachment#getDigamma()}), so
 * the readout below - including CE's own "half-life" percentage formula
 * ({@code (1 - 0.5^digamma) * 100}) - is real. CE's ambient {@code playVoices} effect (no caller
 * wired it to run every tick regardless of holding the item) is not ported.
 */
public class ItemDigammaDiagnostic extends Item {

    public ItemDigammaDiagnostic(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            double digamma = ((int) (HbmLivingAttachment.getData(player).getDigamma() * 1000)) / 1000D;
            double halflife = ((int) ((1D - Math.pow(0.5, digamma)) * 10000)) / 100D;

            player.sendSystemMessage(Component.literal("===== Ƿ Digamma Diagnostic Ƿ =====").withStyle(ChatFormatting.DARK_PURPLE));
            player.sendSystemMessage(Component.literal("Player digamma: " + digamma + " DRX").withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(Component.literal(String.format("Estimated health: %6.2f%%", halflife)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResultHolder.success(stack);
    }
}
