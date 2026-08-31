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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Rad-resistant/decorative glass family, ported from CE's {@code BlockNTMGlass}. CE's {@code
 * getDrops}/{@code canSilkHarvest}/{@code getSilkTouchDrop} trio (always drop nothing, except allow
 * a self-drop under silk touch when {@code doesDrop} is set) is a loot-table concern in modern
 * Minecraft - {@code Block} no longer carries drop-selection hooks at all, that logic now lives in
 * data-driven loot tables. This class exposes {@link #dropsOnSilkTouch()} for a future loot-table
 * datagen pass to consume (flagged in the port report as a follow-up) and otherwise ports the real
 * Java-side behavior: the {@link IRadResistantBlock} marker (its {@link #onPlace}/{@link #onRemove}
 * hooks now call {@link RadiationSystemNT#markSectionForRebuild}, per that interface's own javadoc,
 * now that Phase 4's radiation system is real) and the shield/blast-resistance tooltip lines.
 */
public class BlockNTMGlass extends Block implements IRadResistantBlock {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    private final boolean doesDrop;
    private final boolean radResistant;

    public BlockNTMGlass(Properties properties) {
        this(properties, false, false);
    }

    public BlockNTMGlass(Properties properties, boolean doesDrop) {
        this(properties, doesDrop, false);
    }

    public BlockNTMGlass(Properties properties, boolean doesDrop, boolean radResistant) {
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

        if (this.radResistant) {
            tooltip.add(Component.literal("[").append(Component.translatable("trait.radshield")).append("]").withStyle(ChatFormatting.DARK_GREEN));
        }

        float resistance = this.getExplosionResistance();
        if (resistance > BLAST_RESISTANCE_TOOLTIP_THRESHOLD) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }
    }
}
