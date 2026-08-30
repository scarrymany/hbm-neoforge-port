package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemDesignatorRange} (97 lines, read in full) - a
 * "laser designator": same storage shape as {@link ItemDesignator}, but the target comes from a
 * 300-block ray-trace rather than a direct block click, so it works without touching the target
 * block. CE's {@code Library.rayTrace(player, 300, 1)} maps onto {@link Level#clip(ClipContext)}
 * with {@link ClipContext.Block#OUTLINE}/{@link ClipContext.Fluid#NONE}, the exact idiom this
 * port's already-committed {@link ItemLaserDetonator} uses in place of {@code Library.rayTrace}.
 */
public class ItemDesignatorRange extends Item implements IDesignatorItem {

    private static final double RANGE = 300.0D;

    public ItemDesignatorRange(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());
        if (pos == null) {
            tooltip.add(Component.literal(I18nUtil.resolveKey("desc.choosetarget3")).withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal(I18nUtil.resolveKey("desc.targetcoord")).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal(ChatFormatting.GREEN + "X: " + pos.getX()));
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Z: " + pos.getZ()));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult ray = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockPos pos = ray.getBlockPos();

        if (!(level.getBlockState(pos).getBlock() instanceof LaunchPad)) {
            stack.set(ToolDataComponents.DETONATOR_POS.get(), new BlockPos(pos.getX(), 0, pos.getZ()));

            if (level.isClientSide()) {
                player.displayClientMessage(Component.literal(I18nUtil.resolveKey("chat.possetxz", pos.getX(), pos.getZ()))
                        .withStyle(ChatFormatting.GREEN), false);
            }

            level.playSound(player, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isReady(Level world, ItemStack stack, int x, int y, int z) {
        return stack.get(ToolDataComponents.DETONATOR_POS.get()) != null;
    }

    @Override
    public Vec3 getCoords(Level world, ItemStack stack, int x, int y, int z) {
        BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());
        return pos == null ? Vec3.ZERO : new Vec3(pos.getX(), 0, pos.getZ());
    }
}
