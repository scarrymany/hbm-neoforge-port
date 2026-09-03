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

    static {
        ITEMS.register("foundry_tank", () -> new BlockItem(FOUNDRY_TANK.get(),
                new Item.Properties().fireResistant()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, FOUNDRY_TANK);
    }

    private FoundryBlocks() {
    }
}
