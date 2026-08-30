package com.hbm.blocks.generic;

import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;

import java.util.List;

/**
 * Rad-resistant/decorative glass pane family, ported from CE's {@code BlockNTMGlassPane}. CE
 * extended {@code BlockPane} and added a {@code canPaneConnectTo} override so panes would also
 * connect against {@code BlockNTMGlass} neighbors - 1.12's default pane-connection check
 * ({@code isBlockNormalCube}) excludes non-opaque blocks like glass, so CE had to special-case it.
 * 1.21's {@link IronBarsBlock} (vanilla's own generic pane implementation, also the base of its
 * {@code StainedGlassPaneBlock}) instead connects based on face sturdiness (collision shape), and
 * {@link BlockNTMGlass} is a normal full-collision block, so it already satisfies that check with
 * no override needed - confirmed by reading {@code IronBarsBlock#attachsTo} (declared {@code
 * final}, so it could not be overridden here even if still necessary). This otherwise collapses to
 * the same thin-subclass shape as {@link BlockMetalFence}, plus the {@code doesDrop}/{@code
 * radResistant} fields ported from {@link BlockNTMGlass}.
 */
public class BlockNTMGlassPane extends IronBarsBlock implements IRadResistantBlock {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    private final boolean doesDrop;
    private final boolean radResistant;

    public BlockNTMGlassPane(Properties properties, boolean doesDrop, boolean radResistant) {
        super(properties);
        this.doesDrop = doesDrop;
        this.radResistant = radResistant;
    }

    /** Whether a loot-table datagen pass should let silk touch return this block itself. */
    public boolean dropsOnSilkTouch() {
        return doesDrop;
    }

    @Override
    public boolean isRadResistant(Level worldIn, BlockPos blockPos) {
        return this.radResistant;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (this.radResistant) {
            tooltip.add(Component.literal("[").append(Component.translatable("trait.radshield")).append("]").withStyle(ChatFormatting.DARK_GREEN));
        }

        float resistance = this.getExplosionResistance();
        if (resistance > BLAST_RESISTANCE_TOOLTIP_THRESHOLD) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }
    }
}
