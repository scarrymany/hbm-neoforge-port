package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemDesignator} (91 lines, read in full):
 * right-click a block (that isn't a {@link LaunchPad} itself) to store its {@code (x, z)} as the
 * launch target, per {@code docs/phase3/missile_launch_infra.md}'s Phase-3-safe scope table. No
 * range check, no line-of-sight requirement beyond vanilla's own reach - preserved exactly, matching
 * this port's own {@link ItemDetonator}'s documented "no validation" precedent.
 *
 * <p>Coordinate storage reuses {@link ToolDataComponents#DETONATOR_POS} (a plain
 * {@code DataComponentType<BlockPos>}) rather than a new component, per the research report's Key
 * design/API decisions: {@code y} is always left at 0, matching CE's own {@code getCoords}
 * (which hardcodes {@code y = 0} in the returned {@link Vec3} regardless of the clicked block's
 * height).
 */
public class ItemDesignator extends Item implements IDesignatorItem {

    public ItemDesignator(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());
        if (pos == null) {
            tooltip.add(Component.literal(I18nUtil.resolveKey("desc.choosetarget1")).withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal(I18nUtil.resolveKey("desc.targetcoord")).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal(ChatFormatting.GREEN + "X: " + pos.getX()));
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Z: " + pos.getZ()));
        }
    }

    /** CE: {@code onItemUse} - plain right-click on any non-{@link LaunchPad} block stores its {@code (x, z)}. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.getBlockState(pos).getBlock() instanceof LaunchPad) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        stack.set(ToolDataComponents.DETONATOR_POS.get(), new BlockPos(pos.getX(), 0, pos.getZ()));

        Player player = context.getPlayer();
        if (level.isClientSide() && player != null) {
            player.displayClientMessage(Component.literal("[" + I18nUtil.resolveKey("chat.posset") + "]").withStyle(ChatFormatting.GREEN), false);
        }

        double px = player != null ? player.getX() : pos.getX();
        double py = player != null ? player.getY() : pos.getY();
        double pz = player != null ? player.getZ() : pos.getZ();
        level.playSound(player, px, py, pz, HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResult.SUCCESS;
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
