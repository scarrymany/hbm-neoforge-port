package com.hbm.blocks.generic;

import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.RotatedPillarBlock;

import java.util.List;

/**
 * Radiation-shielding decorative pillar, ported from CE's {@code BlockRadResistantPillar}. CE's
 * {@code onBlockAdded}/{@code breakBlock} overrides only ever called
 * {@code RadiationSystemNT.markSectionForRebuild(...)} - that system is Phase 2 (see the port
 * report's radiation-system deferral note), so per that note this block ships as a plain rotatable
 * pillar today; {@link IRadResistantBlock#isRadResistant} already reports the correct answer for
 * whenever the Phase 2 shielding pass starts querying it, with nothing further to wire up now.
 */
public class BlockRadResistantPillar extends RotatedPillarBlock implements IRadResistantBlock {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    public BlockRadResistantPillar(Properties properties) {
        super(properties);
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
