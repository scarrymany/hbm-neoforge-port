package com.hbm.blocks.network;

import com.hbm.blockentity.network.ConveyorBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for Phase 2's item-conveyor / crane-splitter family - see
 * {@code docs/phase2/blocks_network_conveyor_crane.md}. Mirrors {@code EnergyNetworkBlocks}'
 * established shape (table-driven {@link #registerAll()}/{@link #registerBlock} helper, calling
 * this family's sibling block-entity registration class at the end).
 * <p>
 * Wiring this family into the game needs exactly one call from {@code ModBlocks.register()} (see
 * this task's wiring notes) - no other shared file needs a direct edit.
 * <p>
 * <b>Not ported here</b> (see the research report's "Deferred scope" table): the 9
 * {@code BlockCraneBase}-derived crane/inserter blocks (need a menu/screen per-block, out of this
 * task's scope) and {@code EntityMovingPackage}/its crane producers.
 */
public final class ConveyorBlocks {

    private static final BlockBehaviour.Properties CONVEYOR_PROPS =
            BlockBehaviour.Properties.of().strength(1.0F, 5.0F).sound(SoundType.METAL).noOcclusion();
    private static final BlockBehaviour.Properties SPLITTER_PROPS =
            BlockBehaviour.Properties.of().strength(2.0F, 10.0F).sound(SoundType.METAL);

    public static DeferredBlock<BlockConveyor> CONVEYOR;
    public static DeferredBlock<BlockConveyorChute> CONVEYOR_CHUTE;
    public static DeferredBlock<BlockConveyorLift> CONVEYOR_LIFT;
    public static DeferredBlock<BlockConveyorDouble> CONVEYOR_DOUBLE;
    public static DeferredBlock<BlockConveyorExpress> CONVEYOR_EXPRESS;
    public static DeferredBlock<BlockConveyorTriple> CONVEYOR_TRIPLE;
    public static DeferredBlock<CraneSplitterBlock> CRANE_SPLITTER;

    private ConveyorBlocks() {
    }

    public static void registerAll() {
        CONVEYOR = registerBlock("conveyor", () -> new BlockConveyor(CONVEYOR_PROPS));
        CONVEYOR_CHUTE = registerBlock("conveyor_chute", () -> new BlockConveyorChute(CONVEYOR_PROPS));
        CONVEYOR_LIFT = registerBlock("conveyor_lift", () -> new BlockConveyorLift(CONVEYOR_PROPS));
        CONVEYOR_DOUBLE = registerBlock("conveyor_double", () -> new BlockConveyorDouble(CONVEYOR_PROPS));
        CONVEYOR_EXPRESS = registerBlock("conveyor_express", () -> new BlockConveyorExpress(CONVEYOR_PROPS));
        CONVEYOR_TRIPLE = registerBlock("conveyor_triple", () -> new BlockConveyorTriple(CONVEYOR_PROPS));
        CRANE_SPLITTER = registerBlock("crane_splitter", () -> new CraneSplitterBlock(SPLITTER_PROPS));

        ConveyorBlockEntities.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
