package com.hbm.blocks.generic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

/**
 * Generic stairs base for a mod building material, replacing CE's {@code BlockGenericStairs}.
 * <p>
 * CE's entire file body ({@code BlockBakeFrame}, a custom {@code StateMapperBase} and a hand-baked
 * FACING/HALF/SHAPE model matrix built at {@code ModelBakeEvent} time) exists only to reproduce,
 * from a single texture, exactly the straight/inner/outer stair model and blockstate machinery
 * vanilla's {@link StairBlock} already ships natively - 1.21's stairs already carry FACING, HALF
 * and SHAPE and already resolve inner/outer corners the same way CE's baker computed them by hand.
 * The port's datagen ground rule (runtime model baking is replaced by datagen-authored blockstates)
 * makes porting that baking mechanism doubly redundant here: a datagen {@code stairsBlock(...)} call
 * against the base material's own texture reproduces CE's output exactly, once that datagen pass is
 * wired up for this package's blocks.
 * <p>
 * Like {@link BlockGenericSlab}, this is infrastructure only - no instances are registered from
 * this package, since every concrete stairs block needs a base material block this survey's scope
 * does not include.
 */
public class BlockGenericStairs extends StairBlock {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    public BlockGenericStairs(Supplier<BlockState> baseState, Properties properties) {
        super(baseState, properties);
    }

    public BlockGenericStairs(Block baseBlock, Properties properties) {
        this(baseBlock::defaultBlockState, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        float resistance = this.getExplosionResistance();
        if (resistance > BLAST_RESISTANCE_TOOLTIP_THRESHOLD) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }
    }
}
