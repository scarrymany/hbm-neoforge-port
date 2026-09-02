package com.hbm.blocks.network;

import com.hbm.blockentity.network.CraneBoxerBlockEntity;
import com.hbm.blockentity.network.CraneExtractorBlockEntity;
import com.hbm.blockentity.network.CraneGrabberBlockEntity;
import com.hbm.blockentity.network.CraneInserterBlockEntity;
import com.hbm.blockentity.network.CraneUnboxerBlockEntity;
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
 * Phase 1: crane_inserter + crane_extractor + crane_grabber.
 * Phase 2: crane_boxer + crane_unboxer (simplified without EntityMovingPackage).
 */
public final class CraneBlocks {

    public static DeferredBlock<? extends Block> CRANE_INSERTER;
    public static Supplier<BlockEntityType<CraneInserterBlockEntity>> CRANE_INSERTER_BE_TYPE;

    public static DeferredBlock<? extends Block> CRANE_EXTRACTOR;
    public static Supplier<BlockEntityType<CraneExtractorBlockEntity>> CRANE_EXTRACTOR_BE_TYPE;

    public static DeferredBlock<? extends Block> CRANE_GRABBER;
    public static Supplier<BlockEntityType<CraneGrabberBlockEntity>> CRANE_GRABBER_BE_TYPE;

    public static DeferredBlock<? extends Block> CRANE_BOXER;
    public static Supplier<BlockEntityType<CraneBoxerBlockEntity>> CRANE_BOXER_BE_TYPE;

    public static DeferredBlock<? extends Block> CRANE_UNBOXER;
    public static Supplier<BlockEntityType<CraneUnboxerBlockEntity>> CRANE_UNBOXER_BE_TYPE;

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

        // crane_grabber
        CRANE_GRABBER = ModBlocks.BLOCKS.register("crane_grabber", () -> new BlockCraneGrabber(props));
        ModItems.ITEMS.register("crane_grabber", () -> new BlockItem(CRANE_GRABBER.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, CRANE_GRABBER);

        CRANE_GRABBER_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("crane_grabber", () -> 
                BlockEntityType.Builder.of(
                        (pos, state) -> new CraneGrabberBlockEntity(CRANE_GRABBER_BE_TYPE.get(), pos, state),
                        CRANE_GRABBER.get()
                ).build(null));

        // crane_boxer (simplified without EntityMovingPackage)
        CRANE_BOXER = ModBlocks.BLOCKS.register("crane_boxer", () -> new BlockCraneBoxer(props));
        ModItems.ITEMS.register("crane_boxer", () -> new BlockItem(CRANE_BOXER.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, CRANE_BOXER);

        CRANE_BOXER_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("crane_boxer", () -> 
                BlockEntityType.Builder.of(
                        (pos, state) -> new CraneBoxerBlockEntity(CRANE_BOXER_BE_TYPE.get(), pos, state),
                        CRANE_BOXER.get()
                ).build(null));

        // crane_unboxer (simplified without EntityMovingPackage)
        CRANE_UNBOXER = ModBlocks.BLOCKS.register("crane_unboxer", () -> new BlockCraneUnboxer(props));
        ModItems.ITEMS.register("crane_unboxer", () -> new BlockItem(CRANE_UNBOXER.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, CRANE_UNBOXER);

        CRANE_UNBOXER_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("crane_unboxer", () -> 
                BlockEntityType.Builder.of(
                        (pos, state) -> new CraneUnboxerBlockEntity(CRANE_UNBOXER_BE_TYPE.get(), pos, state),
                        CRANE_UNBOXER.get()
                ).build(null));
    }
}
