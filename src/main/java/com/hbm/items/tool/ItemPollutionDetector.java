package com.hbm.items.tool;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionHandler.PollutionData;
import com.hbm.handler.pollution.PollutionHandler.PollutionType;
import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ambient readout of local soot/poison/heavy-metal/fallout pollution. Ported from CE's
 * {@code com.hbm.items.tool.ItemPollutionDetector}.
 *
 * <p>CE's {@code onUpdate} fires every 10 ticks for a server-side {@code EntityPlayerMP}, reading
 * {@code PollutionHandler.getPollutionData(world, pos)} (nullable - substituted with an all-zero
 * reading, matching CE's own {@code data = new PollutionData()} fallback) and reporting all four
 * {@link PollutionType} values truncated to 2 decimal places via 4 separate toast packets
 * ({@code PlayerInformPacketLegacy}). This port has no toast-packet infrastructure yet, so - like
 * this package's sibling detector/diagnostic items ({@code ItemDosimeter}, {@code ItemGeigerCounter})
 * - the readout goes to {@link Player#sendSystemMessage} instead, one line per value rather than 4
 * separate toasts.
 */
public class ItemPollutionDetector extends ItemBase {

    public ItemPollutionDetector(Properties properties) {
        super(properties);
    }

    /** CE: {@code (int) (value * 100) / 100F} - truncate (not round) to 2 decimal places. */
    private static float truncate2(float value) {
        return ((int) (value * 100)) / 100F;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        if (level.getGameTime() % 10 != 0) return;

        PollutionData data = PollutionHandler.getPollutionData(level, entity.blockPosition());
        if (data == null) data = new PollutionData();

        float soot = truncate2(data.pollution[PollutionType.SOOT.ordinal()]);
        float poison = truncate2(data.pollution[PollutionType.POISON.ordinal()]);
        float heavyMetal = truncate2(data.pollution[PollutionType.HEAVYMETAL.ordinal()]);
        float fallout = truncate2(data.pollution[PollutionType.FALLOUT.ordinal()]);

        player.sendSystemMessage(Component.literal("Soot: " + soot).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Poison: " + poison).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Heavy metal: " + heavyMetal).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Fallout: " + fallout).withStyle(ChatFormatting.YELLOW));
    }
}
