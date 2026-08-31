package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Registration for {@link BlockBedrockOreTE} ({@code ore_bedrock_block}) - see that class's own
 * javadoc and docs/phase4/ore_veins_and_bedrock_ores.md's "blocking dependency" note. A small,
 * standalone prerequisite for the (separate) ore-veins content package's world-gen placement code;
 * no placement logic lives here.
 */
public final class BedrockOreBlocks {

    /**
     * CE: {@code new BlockBedrockOreTE("ore_bedrock_block").setBlockUnbreakable().setResistance(3_600_000)}.
     * This port's own already-established "huge finite resistance instead of literal Java infinity"
     * convention (see {@code OilChainBlocks}/{@code GenericDecoBlocks#UNBREAKABLE_RESISTANCE}, both
     * {@code 3_600_000.0F}) is reused verbatim here.
     */
    private static final float UNBREAKABLE_RESISTANCE = 3_600_000.0F;

    public static Supplier<BlockEntityType<BlockBedrockOreTE.BedrockOreBlockEntity>> BEDROCK_ORE_ENTITY_TYPE;

    private BedrockOreBlocks() {
    }

    public static void registerAll() {
        registerBedrockOre();
    }

    private static void registerBedrockOre() {
        DeferredBlock<BlockBedrockOreTE> block = ModBlocks.BLOCKS.register("ore_bedrock_block",
                () -> new BlockBedrockOreTE(BlockBehaviour.Properties.of().strength(-1.0F, UNBREAKABLE_RESISTANCE).sound(SoundType.STONE)));
        ModItems.ITEMS.register("ore_bedrock_block", () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.RESOURCE, block);

        BEDROCK_ORE_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("ore_bedrock_block",
                () -> BlockEntityType.Builder.of(BlockBedrockOreTE.BedrockOreBlockEntity::new, block.get()).build(null));
    }
}
