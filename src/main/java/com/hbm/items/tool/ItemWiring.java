package com.hbm.items.tool;

import com.hbm.blockentity.network.energy.PylonBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.util.TagsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemWiring} (read in full): right-click a source pylon
 * to mark it (sneaking), then right-click a target pylon (not sneaking) to link them - the item's
 * per-stack {@code minecraft:custom_data} tag remembers the marked position, exactly like CE's
 * per-stack {@code NBTTagCompound}. Closes the loop the research report flagged: without this item,
 * {@link com.hbm.blocks.network.energy.PylonLargeBlock}/{@link com.hbm.blocks.network.PylonMediumBlock}/
 * {@link com.hbm.blocks.network.energy.SubstationBlock}/{@link com.hbm.blocks.network.energy.PylonRedWireBlock}
 * would be placeable but permanently unlinkable.
 *
 * <p><b>Not ported</b>: CE's {@code ItemWrench} also touches {@code TileEntityPylonBase.addConnection}
 * (an alternate wrench-based linking flow) - out of this network-graph pass's scope, since
 * {@code ItemWrench} is a large general-purpose multi-block tool unrelated to the cable/pylon package
 * otherwise; {@code ItemWiring} alone is a complete, independently functional way to link and
 * unlink (via {@link BlockDummyable}/pylon break -&gt; {@code disconnectAll}) pylons.
 *
 * <p>Chat feedback uses {@link Player#displayClientMessage} (the universal 1.21 equivalent of CE's
 * client-side-only {@code player.sendMessage} calls) gated behind the same server-only body every
 * other ported {@code useOn} override in this port uses, rather than reproducing CE's
 * {@code if (world.isRemote)} branch verbatim - {@code displayClientMessage} already routes to the
 * right side by itself.
 */
public class ItemWiring extends Item {

    public ItemWiring(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (!level.isClientSide) {
            if (findPylon(level, pos) instanceof PylonBaseBlockEntity thisPylon) {
                handlePylonClick(level, player, stack, pos, thisPylon);
            } else {
                handleNonPylonClick(player, stack, pos);
            }
        }

        player.swing(context.getHand());
        return InteractionResult.SUCCESS;
    }

    private void handlePylonClick(Level level, Player player, ItemStack stack, BlockPos pos, PylonBaseBlockEntity thisPylon) {
        if (player.isShiftKeyDown()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            TagsUtil.putCustomData(stack, tag);
            player.displayClientMessage(Component.translatable("chat.wiring.start", pos.getX(), pos.getY(), pos.getZ()), true);
            return;
        }

        if (!TagsUtil.hasCustomData(stack)) return;
        CompoundTag tag = TagsUtil.getCustomData(stack);
        BlockPos markedPos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));

        if (!isLengthValid(pos, markedPos, thisPylon.getMaxWireLength())) {
            int distance = (int) Math.sqrt(pos.distSqr(markedPos));
            player.displayClientMessage(Component.translatable("chat.wiring.tofar", distance, (int) thisPylon.getMaxWireLength()), true);
            return;
        }
        if (pos.equals(markedPos)) {
            player.displayClientMessage(Component.translatable("chat.wiring.noself"), true);
            return;
        }

        if (!(findPylon(level, markedPos) instanceof PylonBaseBlockEntity targetPylon)) return;

        switch (PylonBaseBlockEntity.canConnect(thisPylon, targetPylon)) {
            case 0 -> {
                thisPylon.addConnection(targetPylon.getBlockPos());
                targetPylon.addConnection(thisPylon.getBlockPos());
                player.displayClientMessage(Component.translatable("chat.wiring.connected"), true);
            }
            case 1 -> player.displayClientMessage(Component.translatable("chat.wiring.notcompatible"), true);
            case 2 -> player.displayClientMessage(Component.translatable("chat.wiring.noself"), true);
            case 3 -> {
                int dist = (int) thisPylon.getConnectionPoint().distanceTo(targetPylon.getConnectionPoint());
                int maxLen = (int) Math.min(thisPylon.getMaxWireLength(), targetPylon.getMaxWireLength());
                player.displayClientMessage(Component.translatable("chat.wiring.tofar", dist, maxLen), true);
            }
            default -> {
            }
        }
    }

    private void handleNonPylonClick(Player player, ItemStack stack, BlockPos pos) {
        if (player.isShiftKeyDown()) {
            if (TagsUtil.hasCustomData(stack)) {
                stack.remove(DataComponents.CUSTOM_DATA);
                player.displayClientMessage(Component.translatable("chat.wiring.cleared"), true);
            }
            return;
        }

        if (!TagsUtil.hasCustomData(stack)) return;
        CompoundTag tag = TagsUtil.getCustomData(stack);
        BlockPos markedPos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        int distance = (int) Math.sqrt(pos.distSqr(markedPos));
        player.displayClientMessage(Component.translatable("chat.wiring.measure", distance), true);
    }

    @org.jetbrains.annotations.Nullable
    private static BlockEntity findPylon(Level level, BlockPos pos) {
        BlockPos core = pos;
        if (level.getBlockState(pos).getBlock() instanceof BlockDummyable dummy) {
            BlockPos found = dummy.findCore(level, pos);
            if (found != null) core = found;
        }
        return level.getBlockEntity(core);
    }

    private static boolean isLengthValid(BlockPos a, BlockPos b, double length) {
        return Math.sqrt(a.distSqr(b)) <= length;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (TagsUtil.hasCustomData(stack)) {
            CompoundTag tag = TagsUtil.getCustomData(stack);
            tooltip.add(Component.translatable("desc.wiring.start", tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
        } else {
            tooltip.add(Component.translatable("desc.wiring.1"));
            tooltip.add(Component.translatable("desc.wiring.2"));
            tooltip.add(Component.translatable("desc.wiring.3"));
            tooltip.add(Component.translatable("desc.wiring.4"));
        }
    }
}
