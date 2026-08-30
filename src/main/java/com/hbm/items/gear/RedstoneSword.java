package com.hbm.items.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Port of CE's {@code RedstoneSword} - "the very first NTM item". Right-clicking a full-cube block
 * places redstone wire in the adjacent air block (in the direction of the clicked face) and damages
 * the sword by 14 per use.
 * <p>
 * Not ported: CE's {@code canPlayerEdit} pre-check and {@code IHasCustomModel} (1.12 model-loader
 * plumbing, superseded by datagen). Vanilla's own interaction pipeline already runs its block
 * protection checks before {@link #useOn} is ever invoked, so the explicit re-check is redundant
 * here rather than dropped behavior.
 */
public class RedstoneSword extends SwordItem {

    public RedstoneSword(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Very First NTM Item").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;

        BlockState clicked = level.getBlockState(context.getClickedPos());
        var wirePos = context.getClickedPos().relative(context.getClickedFace());

        if (level.getBlockState(wirePos).isAir() && clicked.isCollisionShapeFullBlock(level, context.getClickedPos())) {
            level.playSound(null, wirePos.getX() + 0.5D, wirePos.getY() + 0.5D, wirePos.getZ() + 0.5D,
                    SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            level.setBlock(wirePos, Blocks.REDSTONE_WIRE.defaultBlockState(), 1 | 2);

            Player player = context.getPlayer();
            if (player != null) {
                context.getItemInHand().hurtAndBreak(14, player, LivingEntity.getSlotForHand(context.getHand()));
            }
        }

        return InteractionResult.SUCCESS;
    }
}
