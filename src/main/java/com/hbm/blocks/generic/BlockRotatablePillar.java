package com.hbm.blocks.generic;

import com.hbm.blocks.ICustomBlockModelRegister;
import com.hbm.blocks.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;

import java.util.List;

/**
 * Rotatable decorative pillar, ported from CE's {@code BlockRotatablePillar}. CE's two constructor
 * overloads (with/without an explicit {@link net.minecraft.world.level.block.SoundType}) collapse
 * to one here, since sound type is now part of the {@code Properties} built at the registration
 * call site rather than a post-construction setter. The two hardcoded schrabidium/euphemium cluster
 * tooltip lines are preserved via direct sibling-field references, matching CE's own
 * {@code ModBlocks.block_schrabidium_cluster} identity check.
 */
public class BlockRotatablePillar extends RotatedPillarBlock implements ICustomBlockModelRegister {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    public BlockRotatablePillar(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (this == ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get()) {
            tooltip.add(Component.translatable("tile.block_schrabidium_cluster.desc"));
        }
        if (this == ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get()) {
            tooltip.add(Component.translatable("tile.block_euphemium_cluster.desc"));
        }

        float resistance = this.getExplosionResistance();
        if (resistance > BLAST_RESISTANCE_TOOLTIP_THRESHOLD) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * {@code ModBlockStateProvider}'s cube-all default would generate a single variant shared by
     * every {@link net.minecraft.core.Direction.Axis} value, which is wrong for a real pillar (the
     * side texture needs to rotate to face outward on the X/Z axes) - {@link BlockStateProvider#axisBlock}
     * is the confirmed real datagen helper for exactly this AXIS-keyed vertical/horizontal model pair,
     * built here from the block's own single registry-name texture (this class has no separate
     * top/side texture pair - see CE's own plain pillar texture sets), matching the exact
     * {@code "block/" + registryName} texture-path convention {@code simpleCubeAllBlock(...)} itself
     * would have used (built directly via {@link BlockStateProvider#modLoc}, confirmed public and
     * already used externally by this same package's {@code WasteEarth}/{@code WasteLog}, rather than
     * via the provider's own {@code blockTexture(Block)} helper, which this port could not confirm is
     * public rather than protected against the real 1.21.1 jar in this sandbox).
     */
    @Override
    public void registerModel(BlockStateProvider provider, ResourceLocation modelLocation) {
        String name = modelLocation.getPath();
        ResourceLocation texture = provider.modLoc("block/" + name);

        provider.axisBlock(this, texture, texture);
        provider.simpleBlockItem(this, provider.models().getExistingFile(texture));
    }
}
