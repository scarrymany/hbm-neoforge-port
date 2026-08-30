package com.hbm.blocks.generic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Stairs variant of {@link BlockSpeedy}, ported from CE's {@code BlockSpeedyStairs}.
 */
public class BlockSpeedyStairs extends BlockGenericStairs {

    private final double speed;

    public BlockSpeedyStairs(net.minecraft.world.level.block.Block baseBlock, Properties properties, double speed) {
        super(baseBlock, properties);
        this.speed = speed;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof Player player) {
            Vec3 delta = player.getDeltaMovement();
            if (delta.horizontalDistanceSqr() > 0.0) {
                player.setDeltaMovement(delta.multiply(speed, 1.0, speed));
                player.hasImpulse = true;
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Increases speed by " + Mth.floor((speed - 1) * 100) + "%").withStyle(ChatFormatting.BLUE));
    }
}
