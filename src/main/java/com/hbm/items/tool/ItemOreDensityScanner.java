package com.hbm.items.tool;

import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Reports the local noise-based ore density for every {@link BedrockOreType} bucket. Ported from
 * CE's {@code com.hbm.items.tool.ItemOreDensityScanner}.
 *
 * <p>CE's version blended two related-but-distinct systems: {@code ItemBedrockOreBase.getOreLevel}
 * (a per-position noise scan, the "how good would a scan tool find this spot" system) and
 * {@code com.hbm.world.feature.BedrockOre.getTier}/{@code getBoreFluid} (translating a combined
 * level into what a *placed* bedrock-ore world-gen feature at that quality would actually yield).
 * This port's {@code com.hbm.items.special} package already carries the first system in full (per
 * this area's task brief: "their world-gen soft-dependency is already satisfied by the existing
 * {@code BlockBedrockOre}/{@code BedrockOre*} cluster"), so the per-type density readout below is
 * real, unabridged logic - not a stub. The second system ({@code com.hbm.world.feature.BedrockOre})
 * does not exist anywhere in this port yet (it is a distinct, larger world-gen feature, not part of
 * the special-items ore cluster), so the closing "Tier N - X mB of fluid" summary line is left an
 * explicit TODO rather than faked.
 *
 * <p>CE dispatched its per-type readout through a custom {@code PlayerInformPacketLegacy} (an
 * action-bar-style toast). No such packet exists in this port; {@code Player#sendSystemMessage} (the
 * pattern already used by sibling items in this package, e.g. {@link ItemDosimeter}) is used
 * instead - same information, delivered to chat rather than the action bar.
 */
public class ItemOreDensityScanner extends Item {

    public ItemOreDensityScanner(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || level.getGameTime() % 5 != 0 || !(entity instanceof Player player)) {
            return;
        }

        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        double totalLevel = 0D;

        for (BedrockOreType type : BedrockOreType.VALUES) {
            double density = ItemBedrockOreBase.getOreLevel(x, z, type);
            totalLevel += density;

            player.sendSystemMessage(Component.translatable("item.bedrock_ore.type." + type.suffix + ".name")
                    .append(Component.literal(": " + formatDensity(density) + " ("))
                    .append(Component.translatable(densityKey(density)).withStyle(densityColor(density)))
                    .append(Component.literal(")").withStyle(ChatFormatting.RESET)));
        }

        totalLevel /= BedrockOreType.VALUES.length;

        // TODO(cross-area follow-up): once com.hbm.world.feature.BedrockOre (tier/fluid-per-level
        // lookup) is ported, close out CE's "Tier N - X mB of <fluid>" summary line here using
        // totalLevel, as CE's BedrockOre.getTier(totalLevel)/getBoreFluid(totalLevel) do.
    }

    public static String formatDensity(double density) {
        return String.valueOf(((int) (density * 100)) / 100D);
    }

    /** Ported verbatim from CE's own {@code ItemOreDensityScanner.translateDensity}. */
    public static String densityKey(double density) {
        if (density <= 0.1) return "item.ore_density_scanner.verypoor";
        if (density <= 0.35) return "item.ore_density_scanner.poor";
        if (density <= 0.75) return "item.ore_density_scanner.low";
        if (density >= 1.9) return "item.ore_density_scanner.excellent";
        if (density >= 1.65) return "item.ore_density_scanner.veryhigh";
        if (density >= 1.25) return "item.ore_density_scanner.high";
        return "item.ore_density_scanner.moderate";
    }

    /** Ported verbatim from CE's own {@code ItemOreDensityScanner.getColor}. */
    public static ChatFormatting densityColor(double density) {
        if (density <= 0.1) return ChatFormatting.DARK_RED;
        if (density <= 0.35) return ChatFormatting.RED;
        if (density <= 0.75) return ChatFormatting.GOLD;
        if (density >= 1.9) return ChatFormatting.AQUA;
        if (density >= 1.65) return ChatFormatting.BLUE;
        if (density >= 1.25) return ChatFormatting.GREEN;
        return ChatFormatting.YELLOW;
    }
}
