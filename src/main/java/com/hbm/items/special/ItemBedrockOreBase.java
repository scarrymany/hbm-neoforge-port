package com.hbm.items.special;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Ported from CE's {@code ItemBedrockOreBase}: the "raw scanned ore" item the Ore Slopper
 * (Phase 2 machine, not implemented here) turns into {@link ItemBedrockOre} stacks. CE's per-
 * {@code BedrockOreType.suffix} NBT doubles become a single {@link BedrockOreAmounts} data
 * component ({@link BedrockOreComponents#SCAN_AMOUNTS}).
 * <p>
 * {@link #getOreLevel} re-expresses CE's world-position noise scan against 1.21's
 * {@link PerlinNoise} - CE ran on 1.12 Forge's {@code NoiseGeneratorPerlin}, an unrelated
 * implementation that no longer exists in modern Minecraft. Same seeds, octave count, coordinate
 * scale, output magnitude and [0, 2] clamp as CE, but not bit-for-bit identical output since the
 * underlying noise engines differ between versions - this drives a per-world-position flavor value
 * (ore scan density), not something anything depends on matching CE exactly.
 */
public class ItemBedrockOreBase extends Item {

    private static final double COORDINATE_SCALE = 0.01D;
    private static final double MAGNITUDE = 0.05D;
    private static final double MAX_LEVEL = 2.0D;
    private static final long LEVEL_SEED = 2114043L;
    private static final long TYPE_SEED_BASE = 2082127L;

    private static PerlinNoise level;
    private static final Map<BedrockOreType, PerlinNoise> ORE_NOISE = new EnumMap<>(BedrockOreType.class);

    public ItemBedrockOreBase(Properties properties) {
        super(properties);
    }

    public static double getOreAmount(ItemStack stack, BedrockOreType type) {
        return stack.getOrDefault(BedrockOreComponents.SCAN_AMOUNTS.get(), BedrockOreAmounts.EMPTY).get(type);
    }

    public static void setOreAmount(ItemStack stack, int x, int z) {
        setOreAmount(stack, x, z, 1.0D);
    }

    public static void setOreAmount(ItemStack stack, int x, int z, double mult) {
        stack.set(BedrockOreComponents.SCAN_AMOUNTS.get(), new BedrockOreAmounts(
                getOreLevel(x, z, BedrockOreType.LIGHT_METAL) * mult,
                getOreLevel(x, z, BedrockOreType.HEAVY_METAL) * mult,
                getOreLevel(x, z, BedrockOreType.RARE_EARTH) * mult,
                getOreLevel(x, z, BedrockOreType.ACTINIDE) * mult,
                getOreLevel(x, z, BedrockOreType.NON_METAL) * mult,
                getOreLevel(x, z, BedrockOreType.CRYSTALLINE) * mult));
    }

    public static double getOreLevel(int x, int z, BedrockOreType type) {
        if (level == null) {
            level = PerlinNoise.create(RandomSource.create(LEVEL_SEED), IntStream.rangeClosed(-3, 0));
        }
        PerlinNoise typeNoise = ORE_NOISE.computeIfAbsent(type,
                t -> PerlinNoise.create(RandomSource.create(TYPE_SEED_BASE + t.ordinal()), IntStream.rangeClosed(-3, 0)));

        double sampleX = x * COORDINATE_SCALE;
        double sampleZ = z * COORDINATE_SCALE;
        double combined = level.getValue(sampleX, 0, sampleZ) * typeNoise.getValue(sampleX, 0, sampleZ);
        return Mth.clamp(Math.abs(combined) * MAGNITUDE, 0.0, MAX_LEVEL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (BedrockOreType type : BedrockOreType.VALUES) {
            double amount = getOreAmount(stack, type);
            tooltip.add(Component.translatable("item.bedrock_ore.type." + type.suffix + ".name")
                    .append(Component.literal(": " + formatAmount(amount) + " ("))
                    .append(Component.translatable(densityKey(amount)).withStyle(densityColor(amount)))
                    .append(Component.literal(")").withStyle(ChatFormatting.RESET)));
        }
    }

    private static String formatAmount(double amount) {
        return String.valueOf(((int) (amount * 100)) / 100D);
    }

    /**
     * Density thresholds and colors ported from CE's {@code ItemOreDensityScanner}
     * ({@code com.hbm.items.tool}, a different Phase 1 area's package, not yet ported).
     * Duplicated locally rather than depending on a not-yet-existing class in another area's
     * package; consolidate into one shared home once that item lands.
     */
    private static String densityKey(double density) {
        if (density <= 0.1) return "item.ore_density_scanner.verypoor";
        if (density <= 0.35) return "item.ore_density_scanner.poor";
        if (density <= 0.75) return "item.ore_density_scanner.low";
        if (density >= 1.9) return "item.ore_density_scanner.excellent";
        if (density >= 1.65) return "item.ore_density_scanner.veryhigh";
        if (density >= 1.25) return "item.ore_density_scanner.high";
        return "item.ore_density_scanner.moderate";
    }

    private static ChatFormatting densityColor(double density) {
        if (density <= 0.1) return ChatFormatting.DARK_RED;
        if (density <= 0.35) return ChatFormatting.RED;
        if (density <= 0.75) return ChatFormatting.GOLD;
        if (density >= 1.9) return ChatFormatting.AQUA;
        if (density >= 1.65) return ChatFormatting.BLUE;
        if (density >= 1.25) return ChatFormatting.GREEN;
        return ChatFormatting.YELLOW;
    }
}
