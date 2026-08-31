package com.hbm.blocks.generic;

import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Radiation-shielding decorative pillar, ported from CE's {@code BlockRadResistantPillar}. CE's
 * {@code onBlockAdded}/{@code breakBlock} overrides only ever called
 * {@code RadiationSystemNT.markSectionForRebuild(...)} - now wired below (Phase 4), matching that
 * pattern exactly.
 */
public class BlockRadResistantPillar extends RotatedPillarBlock implements IRadResistantBlock {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    public BlockRadResistantPillar(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("[").append(Component.translatable("trait.radshield")).append("]").withStyle(ChatFormatting.DARK_GREEN));

        float resistance = this.getExplosionResistance();
        if (resistance > BLAST_RESISTANCE_TOOLTIP_THRESHOLD) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }
    }
}
