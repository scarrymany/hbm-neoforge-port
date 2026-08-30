package com.hbm.items.tool;

import com.hbm.items.machine.ItemBattery;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Teleanchor-linked battery remote, ported from CE's {@code com.hbm.items.tool.ItemAnchorRemote}
 * (read in full - an {@link ItemBattery} subclass, {@code super(1_000_000, 10_000, 0, name)}, which
 * this class replicates exactly).
 * <p>
 * <b>Battery behavior is real; teleport behavior is stubbed pending {@code ModBlocks.teleanchor}
 * ({@code MachineTeleanchor}).</b> Confirmed via repo-wide grep: no {@code teleanchor} block exists
 * anywhere in this port, and none of this wave's 13 sibling Phase 2 packages claim it (per
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s Deferred scope, which flags
 * this as "a genuinely small gap...recommend...picking it up as a same-pass addendum to any
 * items_tool implementation pass" - exactly this pass, but the block itself still doesn't exist on
 * disk to link against). The item is fully functional as an {@link ItemBattery} (charges/discharges
 * normally via the HE network) right now; only the teleanchor-link/teleport half of {@code useOn} is
 * a documented no-op.
 */
public class ItemAnchorRemote extends ItemBattery {

    public ItemAnchorRemote(Properties properties) {
        super(1_000_000, 10_000, 0, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO(cross-area follow-up): once ModBlocks.teleanchor (MachineTeleanchor) exists, port
        // CE's onItemUse (store the clicked teleanchor's BlockPos in this stack's custom_data) and
        // onItemRightClick (10,000 HE flat-cost teleport back to that position) here.
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Right-click a teleanchor to link, sneak+use elsewhere to teleport back."));
    }
}
