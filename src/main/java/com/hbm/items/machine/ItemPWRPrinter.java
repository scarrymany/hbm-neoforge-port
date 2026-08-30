package com.hbm.items.machine;

import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.blockentity.machine.PWRProxyBlockEntity;
import com.hbm.blocks.machine.MachinePWRControllerBlock;
import com.hbm.blocks.machine.PWRProxyBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PWR construction-diagram scanner, ported (in reduced scope) from CE's
 * {@code com.hbm.items.machine.ItemPWRPrinter} (read in full). Right-click a
 * {@link PWRControllerBlockEntity} to flood-fill every connected {@link PWRProxyBlock} and report
 * which decorative blocks the structure was built from (each proxy's
 * {@link PWRProxyBlockEntity#getOriginalBlockState}) - real, already-shipped
 * {@code docs/phase2/reactors_breeding_pwr.md} content.
 * <p>
 * <b>Scope reduction from CE, documented rather than silent</b>: CE's real item opens a bespoke
 * {@code GUIScreenSlicePrinter} - a slice-by-slice 3D construction-guide client screen with its own
 * block-state-diffing renderer - fed by a static-field packet sync hack that
 * {@link PWRControllerBlockEntity}'s own javadoc explicitly says was dropped ("port the controller's
 * normal serialize/deserialize path first"). That bespoke screen is a standalone client-rendering
 * subsystem outside a coupling-items pass's scope (no {@code docs/phase2} package owns it). This
 * port's version performs the same underlying flood-fill/original-block-state scan CE's GUI would
 * have displayed, and reports it as a chat summary instead of an in-world hologram - the same
 * diagnostic *data*, a simpler presentation.
 */
public class ItemPWRPrinter extends Item {

    public ItemPWRPrinter(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockEntity te = level.getBlockEntity(context.getClickedPos());

        if (!(te instanceof PWRControllerBlockEntity controller)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos core = controller.getBlockPos();
        var facing = level.getBlockState(core).getValue(MachinePWRControllerBlock.FACING).getOpposite();

        Set<BlockPos> visited = new HashSet<>();
        visited.add(core);
        Map<String, Integer> counts = new HashMap<>();
        int[] proxyCount = {0};

        floodFill(level, core.relative(facing), visited, counts, proxyCount);

        if (player != null) {
            player.displayClientMessage(Component.literal("PWR structure scan: " + proxyCount[0] + " decorative blocks").withStyle(ChatFormatting.GOLD), false);
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                player.displayClientMessage(Component.literal(" - " + entry.getKey() + " x" + entry.getValue()).withStyle(ChatFormatting.YELLOW), false);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static void floodFill(Level level, BlockPos pos, Set<BlockPos> visited, Map<String, Integer> counts, int[] proxyCount) {
        if (visited.contains(pos) || visited.size() > 4096) return;

        if (!(level.getBlockState(pos).getBlock() instanceof PWRProxyBlock)) return;
        visited.add(pos);

        if (level.getBlockEntity(pos) instanceof PWRProxyBlockEntity proxy) {
            BlockState original = proxy.getOriginalBlockState();
            if (original != null) {
                String name = BuiltInRegistries.BLOCK.getKey(original.getBlock()).toString();
                counts.merge(name, 1, Integer::sum);
                proxyCount[0]++;
            }
        }

        for (var dir : net.minecraft.core.Direction.values()) {
            floodFill(level, pos.relative(dir), visited, counts, proxyCount);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on a constructed PWR controller to scan its structure"));
    }
}
