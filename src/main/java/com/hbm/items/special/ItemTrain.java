package com.hbm.items.special;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemTrain}: an {@code ItemEnumMulti}-style multi-metadata item over 2
 * {@link EnumTrainType} rail-car types. Per docs/phase1/items_special.md finding 1, this flattens
 * into one registry entry per type (see {@link SpecialItems}) instead of a single
 * metadata-multiplexed item, the same treatment {@link ItemSoyuz} gets in this same package.
 * <p>
 * Not ported: {@code onItemUse} (spawns the type's {@code EntityRailCarBase} subclass on an
 * {@code IRailNTM} rail block, gauge-checked against the block, positioned and yaw-aligned along the
 * rail) and the {@code EntityRailCarBase}/{@code TrainCargoTram}/{@code TrainCargoTramTrailer}
 * classes it depends on. No rail-block system ({@code IRailNTM}) or rail-car entity system has been
 * ported through Phase 1 (see docs/phase1/items_special.md finding 4's sibling-systems list), so
 * there is nothing yet for this item to place. Registers as a plain shell item; the descriptive stat
 * tooltip (engine/gauge/speed/acceleration/brake/parking-brake - all static per-type strings with no
 * entity dependency) is kept faithful, and the rail-placement interaction is deferred to whichever
 * later phase ports the rail/rocket subsystem.
 */
public class ItemTrain extends Item {

    private final EnumTrainType trainType;

    public ItemTrain(Properties properties, EnumTrainType trainType) {
        super(properties);
        this.trainType = trainType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (trainType.engine != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Engine: " + ChatFormatting.RESET + trainType.engine));
        }
        tooltip.add(Component.literal(ChatFormatting.GREEN + "Gauge: " + ChatFormatting.RESET + trainType.gauge));
        if (trainType.maxSpeed != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Max Speed: " + ChatFormatting.RESET + trainType.maxSpeed));
        }
        if (trainType.acceleration != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Acceleration: " + ChatFormatting.RESET + trainType.acceleration));
        }
        if (trainType.brakeThreshold != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Engine Brake Threshold: " + ChatFormatting.RESET + trainType.brakeThreshold));
        }
        if (trainType.parkingBrake != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Parking Brake: " + ChatFormatting.RESET + trainType.parkingBrake));
        }
    }

    /**
     * Mirrors CE's {@code ItemTrain.EnumTrainType} (2 constants; confirmed against
     * {@code upstream/hbm-ce/.../items/special/ItemTrain.java}). The {@code train} entity-class
     * reference from CE's original enum is dropped - no rail-car entity system exists yet in this
     * port for it to name (see class javadoc) - every display string is otherwise kept verbatim,
     * including CE's own {@code CARGO_TRAM_TRAILER} oddity of putting {@code "Yes"} in the
     * "max speed" slot rather than a speed value; this is CE's original data, not a copy error here.
     */
    public enum EnumTrainType {

        CARGO_TRAM("Electric", "Standard Gauge", "10m/s", "0.2m/s²", "<1m/s", "Yes"),
        CARGO_TRAM_TRAILER(null, "Standard Gauge", "Yes", null, null, "No");

        public static final EnumTrainType[] VALUES = values();

        public final String engine;
        public final String maxSpeed;
        public final String acceleration;
        public final String brakeThreshold;
        public final String parkingBrake;
        public final String gauge;

        EnumTrainType(String engine, String gauge, String maxSpeed, String acceleration, String brakeThreshold, String parkingBrake) {
            this.engine = engine;
            this.maxSpeed = maxSpeed;
            this.acceleration = acceleration;
            this.brakeThreshold = brakeThreshold;
            this.parkingBrake = parkingBrake;
            this.gauge = gauge;
        }
    }
}
