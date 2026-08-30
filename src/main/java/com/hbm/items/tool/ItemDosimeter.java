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
 * Worn radiation-exposure readout. Ported from CE's {@code com.hbm.items.tool.ItemDosimeter}.
 *
 * <p><b>Plain held item, not a Curios/Baubles accessory</b> (documented Phase 1 decision, applies
 * identically to {@link ItemGeigerCounter}/{@link ItemDigammaDiagnostic}/{@link ItemLungDiagnostic}):
 * CE implemented {@code baubles.api.IBauble} behind an {@code @Optional.InterfaceList} soft
 * dependency on the Baubles mod. This port's {@code build.gradle} has no Curios API dependency (the
 * closest NeoForge-era successor to Baubles), and CE's own usage was already optional/soft - so
 * rather than adding a new hard or soft mod dependency for Phase 1, the accessory-slot behavior
 * (auto-tick while worn in a trinket slot) is dropped and this becomes a plain item, usable exactly
 * like CE's held-in-hand right-click path already worked.
 *
 * <p>CE's readout came from {@code ContaminationUtil.getActualPlayerRads}
 * ({@code radBuf + neutrons*20}, scaled by a hazmat-armor resistance multiplier from
 * {@code ContaminationUtil.calculateRadiationMod}). This port's already-ported
 * {@link HbmLivingAttachment} carries the same underlying fields ({@link HbmLivingAttachment#getRadBuf()},
 * {@link HbmLivingAttachment#getNeutrons()}), so the dose readout itself is real; the hazmat
 * resistance multiplier is not applied since that resistance lookup was not confirmed to exist in
 * this port yet - documented simplification, not a fake reading.
 */
public class ItemDosimeter extends Item {

    public ItemDosimeter(Properties properties) {
        super(properties);
    }

    /** CE: {@code ContaminationUtil.getPlayerRads} - radiation buffer plus a neutron contribution. */
    public static double getReceivedRads(Player player) {
        HbmLivingAttachment data = HbmLivingAttachment.getData(player);
        return data.getRadBuf() + data.getNeutrons() * 20.0D;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            double rads = Math.min(getReceivedRads(player), 3.6D);
            rads = ((int) (1000D * rads)) / 1000D;
            player.sendSystemMessage(Component.literal("===== ☢ Dosimeter ☢ =====").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal("Received dose: " + rads + " RAD/s").withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResultHolder.success(stack);
    }
}
