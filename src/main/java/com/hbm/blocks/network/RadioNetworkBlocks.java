package com.hbm.blocks.network;

import com.hbm.blockentity.network.RadioNetworkBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.network.RadioNetworkMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/** CE {@code ModBlocks.java:1432-1438} — radio_torch_* + telex already in DummyableProcessBlocks. */
public final class RadioNetworkBlocks {

    public static DeferredBlock<RadioTorchSenderBlock> RADIO_TORCH_SENDER;
    public static DeferredBlock<RadioTorchReceiverBlock> RADIO_TORCH_RECEIVER;
    public static DeferredBlock<RadioTorchCounterBlock> RADIO_TORCH_COUNTER;
    public static DeferredBlock<RadioTorchLogicBlock> RADIO_TORCH_LOGIC;
    public static DeferredBlock<RadioTorchReaderBlock> RADIO_TORCH_READER;
    public static DeferredBlock<RadioTorchControllerBlock> RADIO_TORCH_CONTROLLER;

    private RadioNetworkBlocks() {
    }

    public static void registerAll() {
        RADIO_TORCH_SENDER = registerBlock("radio_torch_sender", RadioTorchSenderBlock::new);
        RADIO_TORCH_RECEIVER = registerBlock("radio_torch_receiver", RadioTorchReceiverBlock::new);
        RADIO_TORCH_COUNTER = registerBlock("radio_torch_counter", RadioTorchCounterBlock::new);
        RADIO_TORCH_LOGIC = registerBlock("radio_torch_logic", RadioTorchLogicBlock::new);
        RADIO_TORCH_READER = registerBlock("radio_torch_reader", RadioTorchReaderBlock::new);
        RADIO_TORCH_CONTROLLER = registerBlock("radio_torch_controller", RadioTorchControllerBlock::new);
        RadioNetworkBlockEntities.registerAll();
        RadioNetworkMenus.registerAll();
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
