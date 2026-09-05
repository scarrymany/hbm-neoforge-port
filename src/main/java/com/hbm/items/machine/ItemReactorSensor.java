package com.hbm.items.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.dummyable.DummyableProcessBlocks;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * CE {@code ItemReactorSensor}: marks a {@code reactor_research} position (core, via findCore).
 * Link sound Exact CE {@code ItemReactorSensor.java:61} ({@code techBoop} 1.0F/1.0F BLOCKS at player).
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
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!level.isClientSide && player != null) {
            player.displayClientMessage(
                    Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                            .append(Component.translatable(stack.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                            .append(Component.literal("Position set!").withStyle(ChatFormatting.GREEN)),
                    false);
        }
        // Exact CE ItemReactorSensor.java:61 — both sides, player except (client plays locally).
        if (player != null) {
            level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    HBMSoundHandler.techBoop.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
