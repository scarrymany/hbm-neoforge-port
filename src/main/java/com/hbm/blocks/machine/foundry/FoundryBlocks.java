package com.hbm.blocks.machine.foundry;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.main.MainRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for foundry multiblock blocks.
 */
public class FoundryBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MainRegistry.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MainRegistry.MODID);

    public static final DeferredBlock<BlockFoundryTank> FOUNDRY_TANK = BLOCKS.register("foundry_tank",
            () -> new BlockFoundryTank(BlockBehaviour.Properties.of()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final DeferredBlock<BlockFoundryChannel> FOUNDRY_CHANNEL = BLOCKS.register("foundry_channel",
            () -> new BlockFoundryChannel(BlockBehaviour.Properties.of()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final DeferredBlock<BlockFoundryOutlet> FOUNDRY_OUTLET = BLOCKS.register("foundry_outlet",
            () -> new BlockFoundryOutlet(BlockBehaviour.Properties.of()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<BlockFoundryBasin> FOUNDRY_BASIN = BLOCKS.register("foundry_basin",
            () -> new BlockFoundryBasin(BlockBehaviour.Properties.of()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final DeferredBlock<BlockFoundryMold> FOUNDRY_MOLD = BLOCKS.register("foundry_mold",
            () -> new BlockFoundryMold(BlockBehaviour.Properties.of()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    static {
        ITEMS.register("foundry_tank", () -> new BlockItem(FOUNDRY_TANK.get(),
                new Item.Properties().fireResistant()));
        ITEMS.register("foundry_channel", () -> new BlockItem(FOUNDRY_CHANNEL.get(),
                new Item.Properties().fireResistant()));
        ITEMS.register("foundry_outlet", () -> new BlockItem(FOUNDRY_OUTLET.get(),
                new Item.Properties().fireResistant()));
        ITEMS.register("foundry_basin", () -> new BlockItem(FOUNDRY_BASIN.get(),
                new Item.Properties().fireResistant()));
        ITEMS.register("foundry_mold", () -> new BlockItem(FOUNDRY_MOLD.get(),
                new Item.Properties().fireResistant()));
        
        CreativeTabContents.add(ModCreativeTabs.MACHINE, FOUNDRY_TANK);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, FOUNDRY_CHANNEL);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, FOUNDRY_OUTLET);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, FOUNDRY_BASIN);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, FOUNDRY_MOLD);
    }

    private FoundryBlocks() {
    }
}
