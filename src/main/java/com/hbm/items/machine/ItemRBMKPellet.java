package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * RBMK "pellet for recycling" byproduct. CE mixed two independent metadata dimensions on one
 * registry entry per fuel type: which fuel the pellet came from (a handful of hand-authored
 * instances) and a 0-9 depletion/xenon stage layered on top ({@code meta % 5} for stage,
 * {@code meta >= 5} for the xenon flag). Only the fuel-type dimension is flattened into distinct
 * registry entries here, per the porting plan; the depletion/xenon stage stays a data component
 * ({@link MachineDataComponents#RBMK_PELLET_STAGE}) since it is runtime-computed decay, not a fixed
 * craftable variant. Tooltip/creative-tab logic only, no RBMK tile entity reference in CE.
 */
public class ItemRBMKPellet extends ItemBase {

    private final String fullName;

    public ItemRBMKPellet(String fullName, Properties properties) {
        super(properties);
        this.fullName = fullName;
    }

    public static int getStage(ItemStack stack) {
        int stage = stack.getOrDefault(MachineDataComponents.RBMK_PELLET_STAGE.get(), 0);
        return Math.floorMod(stage, 10);
    }

    public static void setStage(ItemStack stack, int stage) {
        stack.set(MachineDataComponents.RBMK_PELLET_STAGE.get(), Math.floorMod(stage, 10));
    }

    public static boolean hasXenon(ItemStack stack) {
        return getStage(stack) >= 5;
    }

    /**
     * CE: {@code ItemRBMKPellet.rectify(int meta)} (clamped {@code Math.abs(meta) % 10} over raw
     * metadata). This port already flattens that 0-9 stage into {@link #getStage}/{@link #setStage}
     * via a data component instead of metadata, so this overload is just an {@link ItemStack} alias
     * for {@link #getStage} - added to fix a real, already-present compile break:
     * {@code com.hbm.hazard.modifier.HazardModifierRBMKRadiation} (committed in an earlier Phase 1
     * pass) calls {@code ItemRBMKPellet.rectify(stack)}, which never existed on this class.
     */
    public static int rectify(ItemStack stack) {
        return getStage(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.ITALIC + this.fullName));
        tooltip.add(Component.literal(ChatFormatting.DARK_GRAY.toString() + ChatFormatting.ITALIC + "Pellet for recycling"));

        int stage = getStage(stack);
        switch (stage % 5) {
            case 0 -> tooltip.add(Component.literal(ChatFormatting.GOLD + "Brand New"));
            case 1 -> tooltip.add(Component.literal(ChatFormatting.YELLOW + "Barely Depleted"));
            case 2 -> tooltip.add(Component.literal(ChatFormatting.GREEN + "Moderately Depleted"));
            case 3 -> tooltip.add(Component.literal(ChatFormatting.DARK_GREEN + "Highly Depleted"));
            case 4 -> tooltip.add(Component.literal(ChatFormatting.DARK_GRAY + "Fully Depleted"));
            default -> {
            }
        }

        if (hasXenon(stack)) tooltip.add(Component.literal(ChatFormatting.DARK_PURPLE + "High Xenon Poison"));
    }
}
