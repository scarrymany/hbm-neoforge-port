package com.hbm.blocks.generic;

import com.hbm.blocks.ICustomBlockModelRegister;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;

import java.util.List;

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
public class BlockGenericStairs extends StairBlock implements ICustomBlockModelRegister {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    public BlockGenericStairs(BlockState baseState, Properties properties) {
        super(baseState, properties);
    }

    public BlockGenericStairs(Block baseBlock, Properties properties) {
        this(baseBlock.defaultBlockState(), properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        float resistance = this.getExplosionResistance();
        if (resistance > BLAST_RESISTANCE_TOOLTIP_THRESHOLD) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * {@code ModBlockStateProvider}'s cube-all default cannot express this block's real FACING/HALF/
     * SHAPE blockstate at all (a plain cube-all model has none of those properties, let alone the
     * straight/inner/outer corner geometry stairs need) - {@link BlockStateProvider#stairsBlock} is
     * the confirmed real datagen helper that builds the full straight+inner+outer model trio and
     * wires it to every FACING/HALF/SHAPE combination, from this block's own single registry-name
     * texture (the base material's plain texture, per this class's javadoc), built via the exact
     * {@code "block/" + registryName} convention {@code simpleCubeAllBlock(...)} itself would have
     * used (through {@link BlockStateProvider#modLoc}, confirmed public and already used externally
     * by this package's {@code WasteEarth}/{@code WasteLog}, rather than the provider's own
     * {@code blockTexture(Block)} helper, which this port could not confirm is public rather than
     * protected against the real 1.21.1 jar in this sandbox). The stair item's icon reuses the
     * straight-stairs model exactly like vanilla's own stair items do (no separate flat icon),
     * following the confirmed real {@code blockItem(...)} pattern in the Neo Edition reference's
     * {@code NtmBlockStateProvider} (used right after its own {@code stairsBlock(...)} calls).
     */
    @Override
    public void registerModel(BlockStateProvider provider, ResourceLocation modelLocation) {
        String name = modelLocation.getPath();
        String base = name.endsWith("_stairs") ? name.substring(0, name.length() - "_stairs".length()) : name;
        ResourceLocation texture = provider.modLoc("block/" + base);

        provider.stairsBlock(this, texture);
        provider.simpleBlockItem(this, new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile(texture));
    }
}
