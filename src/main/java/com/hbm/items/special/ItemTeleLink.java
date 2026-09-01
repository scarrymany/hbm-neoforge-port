package com.hbm.items.special;

import com.hbm.blockentity.machine.dummyable.MachineTeleporterBlockEntity;
import com.hbm.blocks.machine.MachineTeleporterBlock;
import com.hbm.items.tool.ToolDataComponents;
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

import java.util.List;

/**
 * CE {@code ItemTeleLink} — {@code linker}. Saves a BlockPos; sneak-applies it to
 * {@link MachineTeleporterBlockEntity} ({@code target} + {@code linked=true}), then clears.
 *
 * <p>CE raw NBT {@code x/y/z} → port {@link ToolDataComponents#DETONATOR_POS} (same triplet).
 *
 * @see com.hbm.items.special.ItemTeleLink (CE {@code ItemTeleLink.java:32-71})
 */
public class ItemTeleLink extends Item {
    public ItemTeleLink(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();

        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide
                    && stack.has(ToolDataComponents.DETONATOR_POS.get())
                    && level.getBlockState(clicked).getBlock() instanceof MachineTeleporterBlock
                    && level.getBlockEntity(clicked) instanceof MachineTeleporterBlockEntity te) {
                BlockPos saved = stack.get(ToolDataComponents.DETONATOR_POS.get());
                if (saved != null) {
                    te.target = saved.immutable();
                    te.linked = true;
                    te.setChanged();
                    stack.remove(ToolDataComponents.DETONATOR_POS.get());
                    player.displayClientMessage(Component.translatable(
                            "chat.telelink.linked", saved.getX(), saved.getY(), saved.getZ()), false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide) {
            stack.set(ToolDataComponents.DETONATOR_POS.get(), clicked.immutable());
            if (player != null) {
                player.displayClientMessage(Component.translatable(
                        "chat.telelink.set", clicked.getX(), clicked.getY(), clicked.getZ()), false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());
        if (pos != null) {
            tooltip.add(Component.translatable("chat.possetxyz", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.linker.desc1"));
            tooltip.add(Component.translatable("item.linker.desc2"));
            tooltip.add(Component.translatable("chat.posnoset").withStyle(ChatFormatting.YELLOW));
        }
    }
}
