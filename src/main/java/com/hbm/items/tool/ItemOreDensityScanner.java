package com.hbm.items.tool;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.world.feature.BedrockOreFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Reports the local noise-based ore density for every {@link BedrockOreType} bucket. Ported from
 * CE's {@code com.hbm.items.tool.ItemOreDensityScanner}.
 *
 * <p>Per-type density via {@link ItemBedrockOreBase#getOreLevel}. Summary
 * {@code Tier N - X mB <fluid>} Exact CE {@code ItemOreDensityScanner.java:55-63} using
 * {@link BedrockOreFeature#getTier}/{@link BedrockOreFeature#getBoreFluid}
 * ({@code BedrockOre.java:90-101}).
 *
 * <p>CE dispatched readout through {@code PlayerInformPacketLegacy}. No such packet exists in this
 * port; {@code Player#sendSystemMessage} is used instead — same text, chat not action bar.
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

        // CE ItemOreDensityScanner.java:55-63
        int tier = BedrockOreFeature.getTier(totalLevel);
        FluidStack boreFluid = BedrockOreFeature.getBoreFluid(totalLevel);
        MutableComponent summary = Component.literal("Tier " + tier);
        if (boreFluid != null) {
            summary.append(Component.literal(" - " + boreFluid.fill + "mB "))
                    .append(Component.translatable(boreFluid.type.getTranslationKey()));
        }
        player.sendSystemMessage(summary.withStyle(ChatFormatting.YELLOW));
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
