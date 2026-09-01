package com.hbm.items.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.dummyable.DummyableProcessBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * CE {@code ItemReactorSensor}: marks a {@code reactor_research} position (core, via findCore).
 */
public class ItemReactorSensor extends Item {

    public ItemReactorSensor(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("x")) {
                tooltip.add(Component.literal("x: " + tag.getInt("x")));
                tooltip.add(Component.literal("y: " + tag.getInt("y")));
                tooltip.add(Component.literal("z: " + tag.getInt("z")));
                return;
            }
        }
        tooltip.add(Component.literal("No reactor selected!"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Block block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        if (block != DummyableProcessBlocks.REACTOR_RESEARCH.get()) {
            return InteractionResult.PASS;
        }
        BlockPos core = ((BlockDummyable) block).findCore(context.getLevel(), context.getClickedPos());
        if (core == null) core = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("x", core.getX());
        tag.putInt("y", core.getY());
        tag.putInt("z", core.getZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (!context.getLevel().isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                            .append(Component.translatable(stack.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                            .append(Component.literal("Position set!").withStyle(ChatFormatting.GREEN)),
                    false);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
