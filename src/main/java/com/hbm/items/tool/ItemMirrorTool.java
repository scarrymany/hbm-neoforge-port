package com.hbm.items.tool;

import com.hbm.blockentity.machine.SolarBoilerBlockEntity;
import com.hbm.blockentity.machine.SolarMirrorBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineSolarBoilerBlock;
import com.hbm.blocks.machine.SolarMirrorBlock;
import com.hbm.util.TagsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Solar boiler/mirror alignment tool, ported from CE's {@code com.hbm.items.tool.ItemMirrorTool}
 * (read in full). Right-click a {@link MachineSolarBoilerBlock} core to mark the position one block
 * above it (the boiler's heat-input face), then right-click a {@link SolarMirrorBlock} to aim it via
 * {@link SolarMirrorBlockEntity#setTarget} - exactly the coupling that class's own javadoc names this
 * item as "ready for" (confirmed real target, both classes already exist in this port per
 * {@code docs/phase2/machines_power_generation.md}).
 */
public class ItemMirrorTool extends Item {

    private static final double MAX_REACH = 100;

    public ItemMirrorTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Block block = level.getBlockState(pos).getBlock();

        if (block instanceof MachineSolarBoilerBlock dummy) {
            BlockPos core = dummy.findCore(level, pos);
            if (core != null && !level.isClientSide) {
                CompoundTag tag = TagsUtil.getCustomData(stack);
                tag.putInt("posX", core.getX());
                tag.putInt("posY", core.getY() + 1);
                tag.putInt("posZ", core.getZ());
                TagsUtil.putCustomData(stack, tag);
                if (player != null) {
                    player.displayClientMessage(Component.literal("Mirror target linked").withStyle(ChatFormatting.YELLOW), false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (block instanceof SolarMirrorBlock && TagsUtil.hasCustomData(stack)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SolarMirrorBlockEntity mirror && player != null) {
                CompoundTag tag = TagsUtil.getCustomData(stack);
                int tx = tag.getInt("posX");
                int ty = tag.getInt("posY");
                int tz = tag.getInt("posZ");

                double dx = pos.getX() - tx;
                double dy = pos.getY() - ty;
                double dz = pos.getZ() - tz;

                boolean withinReach = new Vec3(dx, dy, dz).length() <= MAX_REACH;
                boolean withinAngle = dx * dx + dz * dz <= dy * dy;

                if (!withinReach) {
                    player.displayClientMessage(Component.literal("Target out of reach").withStyle(ChatFormatting.RED), false);
                } else if (!withinAngle) {
                    player.displayClientMessage(Component.literal("Target angle too steep").withStyle(ChatFormatting.RED), false);
                } else {
                    mirror.setTarget(tx, ty, tz);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click a solar boiler to mark it as a target,"));
        tooltip.add(Component.literal("then right-click a mirror to aim it there."));
    }
}
