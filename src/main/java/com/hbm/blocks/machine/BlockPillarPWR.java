package com.hbm.blocks.machine;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * PWR core-pillar visual shell (fuel rod holder / control rod / coolant channel), ported from CE's
 * {@code BlockPillarPWR}. Flagged in {@code docs/phase2/reactors_breeding_pwr.md}'s Phase-2-safe
 * scope as "a small gap Phase 1 missed, but trivially Phase-2-safe today ... the exact same shape as
 * the already-ported {@link com.hbm.blocks.generic.BlockGenericPWR} sibling (extends BlockBakeBase,
 * no TE, tooltip-only override)" - ported inline here (that report's own recommendation: "or inline
 * here since it has zero coupling to anything this report defers") rather than opening a third,
 * one-file report. Backs the three registry blocks
 * {@link com.hbm.blocks.machine.PWRBlocks#PWR_FUELROD}/{@code PWR_CONTROL}/{@code PWR_CHANNEL} -
 * {@link com.hbm.blockentity.machine.PWRControllerBlockEntity#setup} recognizes all three by
 * {@code Block} identity only during its flood-fill assembly, exactly like
 * {@link com.hbm.blocks.generic.BlockGenericPWR}'s six siblings are recognized by
 * {@code MachinePWRController.isValidCasing}/{@code isValidCore} in CE.
 */
public class BlockPillarPWR extends BlockBase implements ITooltipProvider {

    public BlockPillarPWR(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        this.addStandardInfo(tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
