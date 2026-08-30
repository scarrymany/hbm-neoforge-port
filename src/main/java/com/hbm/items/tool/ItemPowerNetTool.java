package com.hbm.items.tool;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.api.energymk2.PowerNetMK2;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * {@link PowerNetMK2} diagnostic tool, ported from CE's {@code com.hbm.items.tool.ItemPowerNetTool}
 * (read in full). Right-click any {@link IEnergyConductorMK2} to print its network's link/provider/
 * receiver counts - the exact {@code Nodespace.getNode}/{@code PowerNetMK2.links/providerEntries/
 * receiverEntries} surface this port's own {@code PowerNetMK2}/{@code Nodespace} classes already
 * expose (confirmed by direct read, {@code docs/phase2/energy_cable_pylon_network.md}'s owning
 * package for the cable/conductor family this diagnoses).
 * <p>
 * <b>Not ported</b>: CE's particle-marker broadcast over every link position - CE's own code already
 * has that call commented out ("This did not do anything before and im too lazy to add this"), so
 * nothing is lost.
 */
public class ItemPowerNetTool extends Item {

    public ItemPowerNetTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof BlockDummyable dummy) {
            BlockPos core = dummy.findCore(level, pos);
            if (core != null) pos = core;
        }

        BlockEntity te = level.getBlockEntity(pos);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player == null) return InteractionResult.PASS;

        if (te instanceof IEnergyConductorMK2) {
            Nodespace.PowerNode node = Nodespace.getNode(level, pos);

            if (node != null && node.hasValidNet()) {
                PowerNetMK2 net = node.net;
                String id = Integer.toHexString(net.hashCode());
                player.displayClientMessage(Component.literal("Start of diagnostic for network " + id).withStyle(ChatFormatting.GOLD), false);
                player.displayClientMessage(Component.literal("Links: " + net.links.size()).withStyle(ChatFormatting.YELLOW), false);
                player.displayClientMessage(Component.literal("Providers: " + net.providerEntries.size()).withStyle(ChatFormatting.YELLOW), false);
                player.displayClientMessage(Component.literal("Receivers: " + net.receiverEntries.size()).withStyle(ChatFormatting.YELLOW), false);
                player.displayClientMessage(Component.literal("End of diagnostic for network " + id).withStyle(ChatFormatting.GOLD), false);
            } else {
                player.displayClientMessage(Component.literal("Error: No network found!").withStyle(ChatFormatting.RED), false);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click cable to analyze the power net.").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Displays stats such as link and subscriber count").withStyle(ChatFormatting.RED));
    }
}
