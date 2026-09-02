package com.hbm.blocks.network;

import com.hbm.blockentity.network.CraneExtractorBlockEntity;
import com.hbm.blockentity.network.CraneInserterBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + BlockEntity registration for crane family - follows {@link ConveyorBlocks} pattern.
 * Phase 1: crane_inserter + crane_extractor. Other crane types (grabber, boxer, etc.) deferred.
 */
public final class CraneBlocks {

    public static DeferredBlock<? extends Block> CRANE_INSERTER;
    public static Supplier<BlockEntityType<CraneInserterBlockEntity>> CRANE_INSERTER_BE_TYPE;

    public static DeferredBlock<? extends Block> CRANE_EXTRACTOR;
    public static Supplier<BlockEntityType<CraneExtractorBlockEntity>> CRANE_EXTRACTOR_BE_TYPE;

    private CraneBlocks() {
    }

    public static void registerAll() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .strength(2.0F, 10.0F)
                .sound(SoundType.METAL)
                .noOcclusion();

        // crane_inserter
        CRANE_INSERTER = ModBlocks.BLOCKS.register("crane_inserter", () -> new BlockCraneInserter(props));
        ModItems.ITEMS.register("crane_inserter", () -> new BlockItem(CRANE_INSERTER.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, CRANE_INSERTER);

        CRANE_INSERTER_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("crane_inserter", () -> 
                BlockEntityType.Builder.of(
                        (pos, state) -> new CraneInserterBlockEntity(CRANE_INSERTER_BE_TYPE.get(), pos, state),
                        CRANE_INSERTER.get()
                ).build(null));

        // crane_extractor
        CRANE_EXTRACTOR = ModBlocks.BLOCKS.register("crane_extractor", () -> new BlockCraneExtractor(props));
        ModItems.ITEMS.register("crane_extractor", () -> new BlockItem(CRANE_EXTRACTOR.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, CRANE_EXTRACTOR);

        CRANE_EXTRACTOR_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("crane_extractor", () -> 
                BlockEntityType.Builder.of(
                        (pos, state) -> new CraneExtractorBlockEntity(CRANE_EXTRACTOR_BE_TYPE.get(), pos, state),
                        CRANE_EXTRACTOR.get()
                ).build(null));
    }
}
