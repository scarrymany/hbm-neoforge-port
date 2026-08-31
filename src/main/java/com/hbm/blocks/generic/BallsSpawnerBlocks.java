package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Registers {@link BlockBallsSpawner} and its post-use replacement block - see that class's own
 * javadoc and {@code docs/phase4/entities_bosses.md}'s worm-boss table (spawn mechanism #2). Follows
 * the same {@code registerBlock} helper shape {@link WastelandVirusBlocks} already established in this
 * same package.
 */
public final class BallsSpawnerBlocks {

    public static DeferredBlock<BlockBallsSpawner> BALLS_SPAWNER;
    public static DeferredBlock<BlockBase> BALLS_SPAWNER_SPENT;

    private BallsSpawnerBlocks() {
    }

    public static void registerAll() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(15.0F, 60.0F).sound(SoundType.STONE);

        BALLS_SPAWNER = registerBlock("balls_spawner", () -> new BlockBallsSpawner(props), ModCreativeTabs.BLOCKS);
        BALLS_SPAWNER_SPENT = registerBlock("balls_spawner_spent", () -> new BlockBase(props), ModCreativeTabs.BLOCKS);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory, @Nullable ResourceKey<CreativeModeTab> tab) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        if (tab != null) {
            CreativeTabContents.add(tab, block);
        }
        return block;
    }
}
